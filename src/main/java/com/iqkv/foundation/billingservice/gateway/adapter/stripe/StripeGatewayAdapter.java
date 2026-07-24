/*
 * Copyright 2026 IQKV Foundation Team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.iqkv.foundation.billingservice.gateway.adapter.stripe;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.iqkv.foundation.billingservice.gateway.GatewayType;
import com.iqkv.foundation.billingservice.gateway.command.CreateCheckoutSessionCommand;
import com.iqkv.foundation.billingservice.gateway.command.CreateCustomerCommand;
import com.iqkv.foundation.billingservice.gateway.command.CreateRefundCommand;
import com.iqkv.foundation.billingservice.gateway.command.ReportUsageCommand;
import com.iqkv.foundation.billingservice.gateway.command.UpdateSubscriptionCommand;
import com.iqkv.foundation.billingservice.gateway.event.GatewayInvoiceEvent;
import com.iqkv.foundation.billingservice.gateway.event.GatewayPaymentFailureEvent;
import com.iqkv.foundation.billingservice.gateway.event.GatewayRefundEvent;
import com.iqkv.foundation.billingservice.gateway.event.GatewaySubscriptionEvent;
import com.iqkv.foundation.billingservice.gateway.event.GatewayWebhookEvent;
import com.iqkv.foundation.billingservice.gateway.port.PaymentGatewayPort;
import com.iqkv.foundation.billingservice.infrastructure.config.ConditionalOnGateway;
import com.iqkv.foundation.billingservice.infrastructure.config.StripeConfigurationProperties;
import com.iqkv.foundation.billingservice.plan.Plan;
import com.iqkv.foundation.billingservice.shared.exception.PaymentGatewayException;
import com.iqkv.foundation.billingservice.shared.exception.WebhookProcessingException;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import com.stripe.model.Customer;
import com.stripe.model.Event;
import com.stripe.model.Invoice;
import com.stripe.model.Price;
import com.stripe.model.Product;
import com.stripe.model.Refund;
import com.stripe.model.SubscriptionItem;
import com.stripe.model.billingportal.Session;
import com.stripe.net.Webhook;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.PriceCreateParams;
import com.stripe.param.ProductCreateParams;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.SubscriptionUpdateParams;
import com.stripe.param.billingportal.SessionCreateParams;
import com.stripe.param.checkout.SessionCreateParams.LineItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Stripe implementation of {@link PaymentGatewayPort}.
 *
 * <p>Adapts Stripe SDK types to gateway-agnostic domain events and commands.
 * Handles customer creation, webhook signature verification, and event parsing.
 *
 * <p>Stripe-specific event types handled:
 * <ul>
 *   <li>{@code customer.subscription.created}</li>
 *   <li>{@code customer.subscription.updated}</li>
 *   <li>{@code customer.subscription.deleted}</li>
 *   <li>{@code invoice.created}</li>
 *   <li>{@code invoice.finalized}</li>
 *   <li>{@code invoice.updated}</li>
 *   <li>{@code invoice.payment_succeeded}</li>
 *   <li>{@code invoice.payment_failed}</li>
 *   <li>{@code charge.refunded}</li>
 * </ul>
 */
@Component
@ConditionalOnGateway(GatewayType.STRIPE)
public class StripeGatewayAdapter implements PaymentGatewayPort {

  private static final Logger log = LoggerFactory.getLogger(StripeGatewayAdapter.class);

  private static final String EVENT_SUBSCRIPTION_CREATED = "customer.subscription.created";
  private static final String EVENT_SUBSCRIPTION_UPDATED = "customer.subscription.updated";
  private static final String EVENT_SUBSCRIPTION_DELETED = "customer.subscription.deleted";
  private static final String EVENT_INVOICE_CREATED = "invoice.created";
  private static final String EVENT_INVOICE_FINALIZED = "invoice.finalized";
  private static final String EVENT_INVOICE_UPDATED = "invoice.updated";
  private static final String EVENT_INVOICE_PAYMENT_SUCCEEDED = "invoice.payment_succeeded";
  private static final String EVENT_INVOICE_PAYMENT_FAILED = "invoice.payment_failed";
  private static final String EVENT_CHARGE_REFUNDED = "charge.refunded";

  private final StripeConfigurationProperties config;

  public StripeGatewayAdapter(final StripeConfigurationProperties config) {
    this.config = config;
    Stripe.apiKey = config.secretKey();
  }

  @Override
  public GatewayType getGatewayType() {
    return GatewayType.STRIPE;
  }

  /**
   * {@inheritDoc}
   *
   * <p>Creates a Stripe {@code Customer} object. The {@code managed_by} metadata key
   * is always set to {@code "foundation-billing-service"} for traceability.
   */
  @Override
  public String createCustomer(final CreateCustomerCommand command) {
    final CustomerCreateParams.Builder builder = CustomerCreateParams.builder()
        .setName(command.name())
        .putMetadata("managed_by", "foundation-billing-service");

    command.metadata().forEach(builder::putMetadata);

    if (command.email() != null && !command.email().isBlank()) {
      builder.setEmail(command.email());
    }

    try {
      final Customer customer = Customer.create(builder.build());
      log.debug("Created Stripe customer {} for name={}", customer.getId(), command.name());
      return customer.getId();
    } catch (final StripeException e) {
      throw new PaymentGatewayException("Failed to create Stripe customer for: " + command.name(), e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String createCheckoutSession(final CreateCheckoutSessionCommand command) {
    final com.stripe.param.checkout.SessionCreateParams.Builder builder =
        com.stripe.param.checkout.SessionCreateParams.builder()
            .setCustomer(command.customerId())
            .setMode(com.stripe.param.checkout.SessionCreateParams.Mode.SUBSCRIPTION)
            .setSuccessUrl(command.successUrl())
            .setCancelUrl(command.cancelUrl())
            .addLineItem(LineItem.builder()
                .setPrice(command.priceId())
                .setQuantity(command.quantity() != null ? command.quantity() : 1L)
                .build());

    if (command.trialPeriodDays() != null && command.trialPeriodDays() > 0) {
      builder.setSubscriptionData(
          com.stripe.param.checkout.SessionCreateParams.SubscriptionData.builder()
              .setTrialPeriodDays(Long.valueOf(command.trialPeriodDays()))
              .build());
    }

    if (Boolean.TRUE.equals(command.allowPromotionCodes())) {
      builder.setAllowPromotionCodes(true);
    }

    command.metadata().forEach(builder::putMetadata);

    try {
      final com.stripe.model.checkout.Session session =
          com.stripe.model.checkout.Session.create(builder.build());
      log.debug("Created Stripe checkout session for customer {}: {}", command.customerId(), session.getUrl());
      return session.getUrl();
    } catch (final StripeException e) {
      throw new PaymentGatewayException("Failed to create Stripe checkout session for customer: " + command.customerId(), e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void updateSubscription(final UpdateSubscriptionCommand command) {
    try {
      final com.stripe.model.Subscription subscription =
          com.stripe.model.Subscription.retrieve(command.subscriptionId());

      final SubscriptionUpdateParams.Builder builder = SubscriptionUpdateParams.builder();

      if (command.priceId() != null) {
        final String subscriptionItemId = subscription.getItems().getData().get(0).getId();
        builder.addItem(SubscriptionUpdateParams.Item.builder()
            .setId(subscriptionItemId)
            .setPrice(command.priceId())
            .setQuantity(command.quantity() != null ? command.quantity() : 1L)
            .build());
      } else if (command.quantity() != null) {
        final String subscriptionItemId = subscription.getItems().getData().get(0).getId();
        builder.addItem(SubscriptionUpdateParams.Item.builder()
            .setId(subscriptionItemId)
            .setQuantity(command.quantity())
            .build());
      }

      final String prorationBehavior = command.prorationBehavior() != null
          ? command.prorationBehavior()
          : "create_prorations";
      builder.setProrationBehavior(SubscriptionUpdateParams.ProrationBehavior.valueOf(
          prorationBehavior.toUpperCase()));

      command.metadata().forEach(builder::putMetadata);

      subscription.update(builder.build());
      log.debug("Updated Stripe subscription {}", command.subscriptionId());
    } catch (final StripeException e) {
      throw new PaymentGatewayException("Failed to update Stripe subscription: " + command.subscriptionId(), e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void cancelSubscription(final String subscriptionId, final boolean cancelAtPeriodEnd) {
    try {
      final com.stripe.model.Subscription subscription =
          com.stripe.model.Subscription.retrieve(subscriptionId);

      if (cancelAtPeriodEnd) {
        final SubscriptionUpdateParams params = SubscriptionUpdateParams.builder()
            .setCancelAtPeriodEnd(true)
            .build();
        subscription.update(params);
        log.debug("Set Stripe subscription {} to cancel at period end", subscriptionId);
      } else {
        subscription.cancel();
        log.debug("Canceled Stripe subscription {} immediately", subscriptionId);
      }
    } catch (final StripeException e) {
      throw new PaymentGatewayException("Failed to cancel Stripe subscription: " + subscriptionId, e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void pauseSubscription(final String subscriptionId) {
    try {
      final com.stripe.model.Subscription subscription =
          com.stripe.model.Subscription.retrieve(subscriptionId);

      final SubscriptionUpdateParams params = SubscriptionUpdateParams.builder()
          .setPauseCollection(SubscriptionUpdateParams.PauseCollection.builder()
              .setBehavior(SubscriptionUpdateParams.PauseCollection.Behavior.MARK_UNCOLLECTIBLE)
              .build())
          .build();

      subscription.update(params);
      log.debug("Paused Stripe subscription {}", subscriptionId);
    } catch (final StripeException e) {
      throw new PaymentGatewayException("Failed to pause Stripe subscription: " + subscriptionId, e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reactivateSubscription(final String subscriptionId) {
    try {
      final com.stripe.model.Subscription subscription =
          com.stripe.model.Subscription.retrieve(subscriptionId);

      final SubscriptionUpdateParams params = SubscriptionUpdateParams.builder()
          .setPauseCollection(SubscriptionUpdateParams.PauseCollection.builder()
              .setBehavior(null) // Setting behavior to null removes pause_collection in Stripe SDK
              .build())
          .build();

      subscription.update(params);
      log.debug("Reactivated Stripe subscription {}", subscriptionId);
    } catch (final StripeException e) {
      throw new PaymentGatewayException("Failed to reactivate Stripe subscription: " + subscriptionId, e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String createRefund(final CreateRefundCommand command) {
    try {
      if (command.externalCustomerId() != null && !command.externalCustomerId().isBlank()) {
        final Charge charge = Charge.retrieve(command.paymentId());
        if (!command.externalCustomerId().equals(charge.getCustomer())) {
          throw new PaymentGatewayException("Payment " + command.paymentId()
                                            + " does not belong to customer " + command.externalCustomerId());
        }
      }

      final RefundCreateParams.Builder builder = RefundCreateParams.builder()
          .setCharge(command.paymentId())
          .putMetadata("managed_by", "foundation-billing-service");

      if (command.amount() != null) {
        builder.setAmount(command.amount());
      }

      if (command.reason() != null) {
        builder.setReason(RefundCreateParams.Reason.valueOf(command.reason().toUpperCase()));
      }

      command.metadata().forEach(builder::putMetadata);

      final Refund refund = Refund.create(builder.build());
      log.debug("Created Stripe refund {} for charge {}", refund.getId(), command.paymentId());
      return refund.getId();
    } catch (final StripeException e) {
      throw new PaymentGatewayException("Failed to create Stripe refund for charge: " + command.paymentId(), e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String syncProduct(final Plan plan) {
    try {
      String productId = plan.getExternalProductId();
      Product product = null;

      if (productId == null || productId.isBlank()) {
        // Create new product
        final ProductCreateParams.Builder productParamsBuilder = ProductCreateParams.builder()
            .setName(plan.getDisplayName())
            .setActive(plan.getActive())
            .putMetadata("plan_code", plan.getPlanCode())
            .putMetadata("managed_by", "foundation-billing-service");

        if (plan.getDescription() != null && !plan.getDescription().isBlank()) {
          productParamsBuilder.setDescription(plan.getDescription());
        }

        product = Product.create(productParamsBuilder.build());
        productId = product.getId();
        plan.setExternalProductId(productId);
        log.debug("Created Stripe product {} for plan {}", productId, plan.getPlanCode());
      } else {
        // Retrieve and update existing product if needed
        product = Product.retrieve(productId);
        boolean needsUpdate = false;
        final com.stripe.param.ProductUpdateParams.Builder productUpdateBuilder =
            com.stripe.param.ProductUpdateParams.builder();

        if (!product.getName().equals(plan.getDisplayName())) {
          productUpdateBuilder.setName(plan.getDisplayName());
          needsUpdate = true;
        }

        if ((plan.getDescription() != null && !plan.getDescription().isBlank())
            && !plan.getDescription().equals(product.getDescription())) {
          productUpdateBuilder.setDescription(plan.getDescription());
          needsUpdate = true;
        } else if (product.getDescription() != null
                   && (plan.getDescription() == null || plan.getDescription().isBlank())) {
          productUpdateBuilder.setDescription("");
          needsUpdate = true;
        }

        if (!Objects.equals(product.getActive(), plan.getActive())) {
          productUpdateBuilder.setActive(plan.getActive());
          needsUpdate = true;
        }

        if (needsUpdate) {
          product = product.update(productUpdateBuilder.build());
          log.debug("Updated Stripe product {} for plan {}", productId, plan.getPlanCode());
        }
      }

      String priceId = plan.getExternalPriceId();
      boolean createNewPrice = false;

      if (priceId == null || priceId.isBlank()) {
        createNewPrice = true;
      } else {
        final Price price = Price.retrieve(priceId);
        if (!price.getUnitAmount().equals(Long.valueOf(plan.getPriceMinor()))
            || !price.getCurrency().equalsIgnoreCase(plan.getCurrency())) {
          createNewPrice = true;
          log.info("Price changed for plan {}, creating new Stripe price", plan.getPlanCode());
        }
      }

      if (createNewPrice) {
        final PriceCreateParams.Recurring.Interval interval =
            "ANNUAL".equalsIgnoreCase(plan.getBillingPeriod())
                ? PriceCreateParams.Recurring.Interval.YEAR
                : PriceCreateParams.Recurring.Interval.MONTH;

        final PriceCreateParams.Builder priceParamsBuilder = PriceCreateParams.builder()
            .setProduct(productId)
            .setUnitAmount(Long.valueOf(plan.getPriceMinor()))
            .setCurrency(plan.getCurrency())
            .setActive(plan.getActive())
            .putMetadata("plan_code", plan.getPlanCode())
            .putMetadata("managed_by", "foundation-billing-service");

        final PriceCreateParams.Recurring.Builder recurringBuilder = PriceCreateParams.Recurring.builder()
            .setInterval(interval);

        if ("METERED".equalsIgnoreCase(plan.getPricingModel())) {
          recurringBuilder.setUsageType(PriceCreateParams.Recurring.UsageType.METERED);
        }

        priceParamsBuilder.setRecurring(recurringBuilder.build());

        final Price price = Price.create(priceParamsBuilder.build());
        priceId = price.getId();
        plan.setExternalPriceId(priceId);
        log.debug("Created Stripe price {} for product {}", priceId, productId);
      }

      return priceId;
    } catch (final StripeException e) {
      throw new PaymentGatewayException("Failed to sync product/price with Stripe for plan: " + plan.getPlanCode(), e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * <p>Verifies the {@code Stripe-Signature} header using the configured webhook secret,
   * then parses the verified event into a normalized domain event.
   *
   * @throws WebhookProcessingException if the signature is invalid
   */
  @Override
  public Optional<GatewayWebhookEvent> verifyAndParseWebhookEvent(final String payload,
                                                                  final String signature) {
    final Event event;
    try {
      event = Webhook.constructEvent(payload, signature, config.webhookSecret());
    } catch (final SignatureVerificationException e) {
      throw new WebhookProcessingException("Invalid Stripe webhook signature", e);
    }

    return switch (event.getType()) {
      case EVENT_SUBSCRIPTION_CREATED,
           EVENT_SUBSCRIPTION_UPDATED,
           EVENT_SUBSCRIPTION_DELETED -> Optional.of(toSubscriptionEvent(event));
      case EVENT_INVOICE_CREATED,
           EVENT_INVOICE_FINALIZED,
           EVENT_INVOICE_UPDATED,
           EVENT_INVOICE_PAYMENT_SUCCEEDED -> Optional.of(toInvoiceEvent(event));
      case EVENT_INVOICE_PAYMENT_FAILED -> Optional.of(toPaymentFailureEvent(event));
      case EVENT_CHARGE_REFUNDED -> Optional.of(toRefundEvent(event));
      default -> {
        log.debug("Unhandled Stripe event type: {}", event.getType());
        yield Optional.empty();
      }
    };
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String createPortalSession(final String customerId, final String returnUrl) {
    final SessionCreateParams params = SessionCreateParams.builder()
        .setCustomer(customerId)
        .setReturnUrl(returnUrl)
        .build();

    try {
      final Session session = Session.create(params);
      log.debug("Created Stripe portal session for customer {}: {}", customerId, session.getUrl());
      return session.getUrl();
    } catch (final StripeException e) {
      throw new PaymentGatewayException("Failed to create Stripe portal session for customer: " + customerId, e);
    }
  }

  // -------------------------------------------------------------------------
  // Private mapping methods
  // -------------------------------------------------------------------------

  private GatewaySubscriptionEvent toSubscriptionEvent(final Event event) {
    final var stripe = deserializeSubscription(event);
    final Instant occurredAt = Instant.ofEpochSecond(event.getCreated());

    final Long quantity = stripe.getItems() != null && !stripe.getItems().getData().isEmpty()
        ? stripe.getItems().getData().get(0).getQuantity()
        : null;

    return new GatewaySubscriptionEvent(
        event.getId(),
        event.getType(),
        occurredAt,
        "STRIPE",
        stripe.getId(),
        stripe.getCustomer(),
        stripe.getStatus(),
        extractPlanId(stripe),
        quantity,
        stripe.getTrialStart() != null ? Instant.ofEpochSecond(stripe.getTrialStart()) : null,
        stripe.getTrialEnd() != null ? Instant.ofEpochSecond(stripe.getTrialEnd()) : null,
        stripe.getBillingCycleAnchor() != null ? Instant.ofEpochSecond(stripe.getBillingCycleAnchor()) : null,
        stripe.getTrialEnd() != null ? Instant.ofEpochSecond(stripe.getTrialEnd()) : null,
        Boolean.TRUE.equals(stripe.getCancelAtPeriodEnd()),
        stripe.getEndedAt() != null ? Instant.ofEpochSecond(stripe.getEndedAt()) : null,
        stripe.getMetadata() != null ? Map.copyOf(stripe.getMetadata()) : Map.of()
    );
  }

  private GatewayInvoiceEvent toInvoiceEvent(final Event event) {
    final Invoice invoice = deserializeInvoice(event);
    return new GatewayInvoiceEvent(
        event.getId(),
        event.getType(),
        Instant.ofEpochSecond(event.getCreated()),
        "STRIPE",
        invoice.getId(),
        invoice.getCustomer(),
        extractSubscriptionId(invoice),
        null,
        invoice.getAmountPaid(),
        invoice.getAmountDue(),
        invoice.getCurrency()
    );
  }

  private GatewayPaymentFailureEvent toPaymentFailureEvent(final Event event) {
    final Invoice invoice = deserializeInvoice(event);
    final String failureReason = invoice.getLastFinalizationError() != null
                                 && invoice.getLastFinalizationError().getMessage() != null
        ? invoice.getLastFinalizationError().getMessage()
        : "Payment failed";

    return new GatewayPaymentFailureEvent(
        event.getId(),
        event.getType(),
        Instant.ofEpochSecond(event.getCreated()),
        "STRIPE",
        invoice.getId(),
        invoice.getCustomer(),
        extractSubscriptionId(invoice),
        invoice.getAmountDue(),
        invoice.getCurrency(),
        failureReason
    );
  }

  private GatewayRefundEvent toRefundEvent(final Event event) {
    final Object obj = event.getDataObjectDeserializer().getObject().orElseThrow();
    if (obj instanceof Charge charge) {
      final Refund refund = charge.getRefunds() != null && !charge.getRefunds().getData().isEmpty()
          ? charge.getRefunds().getData().get(0)
          : null;

      return new GatewayRefundEvent(
          event.getId(),
          event.getType(),
          Instant.ofEpochSecond(event.getCreated()),
          "STRIPE",
          refund != null ? refund.getId() : null,
          charge.getId(),
          charge.getCustomer(),
          refund != null ? refund.getAmount() : charge.getAmountRefunded(),
          charge.getCurrency(),
          refund != null ? refund.getStatus() : "succeeded"
      );
    } else if (obj instanceof Refund refund) {
      return new GatewayRefundEvent(
          event.getId(),
          event.getType(),
          Instant.ofEpochSecond(event.getCreated()),
          "STRIPE",
          refund.getId(),
          refund.getCharge(),
          null, // Refund object might not have customer ID directly in some versions
          refund.getAmount(),
          refund.getCurrency(),
          refund.getStatus()
      );
    }
    throw new WebhookProcessingException("Unknown object type for refund event: " + obj.getClass().getName());
  }

  private com.stripe.model.Subscription deserializeSubscription(final Event event) {
    return event.getDataObjectDeserializer()
        .getObject()
        .filter(obj -> obj instanceof com.stripe.model.Subscription)
        .map(obj -> (com.stripe.model.Subscription) obj)
        .orElseThrow(() -> new WebhookProcessingException(
            "Could not deserialize Stripe Subscription from event " + event.getId()));
  }

  private Invoice deserializeInvoice(final Event event) {
    return event.getDataObjectDeserializer()
        .getObject()
        .filter(obj -> obj instanceof Invoice)
        .map(obj -> (Invoice) obj)
        .orElseThrow(() -> new WebhookProcessingException(
            "Could not deserialize Stripe Invoice from event " + event.getId()));
  }

  private String extractPlanId(final com.stripe.model.Subscription stripe) {
    if (stripe.getItems() == null || stripe.getItems().getData() == null
        || stripe.getItems().getData().isEmpty()) {
      return null;
    }
    final SubscriptionItem item = stripe.getItems().getData().get(0);
    return item.getPrice() != null ? item.getPrice().getId() : null;
  }

  @Override
  public void reportUsage(ReportUsageCommand command) {
    log.warn("Usage reporting is not yet implemented: subscription={}, metric={}, quantity={}",
        command.externalSubscriptionId(), command.metricName(), command.quantity());
  }

  private String extractSubscriptionId(final Invoice invoice) {
    if (invoice.getLines() == null || invoice.getLines().getData() == null
        || invoice.getLines().getData().isEmpty()) {
      return null;
    }
    return invoice.getLines().getData().get(0).getSubscription();
  }
}
