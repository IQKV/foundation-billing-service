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

package com.iqkv.foundation.billingservice.webhook;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.iqkv.foundation.billingservice.infrastructure.config.NotificationConfigurationProperties;
import com.iqkv.foundation.billingservice.infrastructure.messaging.MessagingService;
import com.iqkv.foundation.billingservice.infrastructure.messaging.NotificationEvent;
import com.iqkv.foundation.billingservice.infrastructure.messaging.NotificationEventType;
import com.iqkv.foundation.billingservice.infrastructure.persistence.BillingSettingsMapper;
import com.iqkv.foundation.billingservice.infrastructure.persistence.SubscriptionMapper;
import com.iqkv.foundation.billingservice.infrastructure.persistence.WebhookLogMapper;
import com.iqkv.foundation.billingservice.settings.BillingSettings;
import com.iqkv.foundation.billingservice.subscription.Subscription;
import com.iqkv.foundation.billingservice.shared.exception.WebhookProcessingException;
import com.stripe.model.Event;
import com.stripe.model.Invoice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Processes inbound Stripe webhook events.
 *
 * <p>Handles idempotency via {@code webhook_log}, dispatches subscription lifecycle
 * events, and publishes domain events and notification emails to RabbitMQ.
 *
 * <p>Status lifecycle: {@code RECEIVED} → {@code PROCESSED} on success,
 * or {@code RECEIVED} → {@code FAILED} on error.
 */
@Service
public class WebhookProcessingService {

  private static final Logger log = LoggerFactory.getLogger(WebhookProcessingService.class);

  private static final String EVENT_SUBSCRIPTION_CREATED  = "customer.subscription.created";
  private static final String EVENT_SUBSCRIPTION_UPDATED  = "customer.subscription.updated";
  private static final String EVENT_SUBSCRIPTION_DELETED  = "customer.subscription.deleted";
  private static final String EVENT_INVOICE_PAID          = "invoice.payment_succeeded";
  private static final String EVENT_INVOICE_PAYMENT_FAILED = "invoice.payment_failed";

  static final String STATUS_RECEIVED  = "RECEIVED";
  static final String STATUS_PROCESSED = "PROCESSED";
  static final String STATUS_FAILED    = "FAILED";

  private final WebhookLogMapper webhookLogMapper;
  private final SubscriptionMapper subscriptionMapper;
  private final BillingSettingsMapper billingSettingsMapper;
  private final MessagingService messagingService;
  private final NotificationConfigurationProperties notificationProps;

  public WebhookProcessingService(final WebhookLogMapper webhookLogMapper,
                                  final SubscriptionMapper subscriptionMapper,
                                  final BillingSettingsMapper billingSettingsMapper,
                                  final MessagingService messagingService,
                                  final NotificationConfigurationProperties notificationProps) {
    this.webhookLogMapper = webhookLogMapper;
    this.subscriptionMapper = subscriptionMapper;
    this.billingSettingsMapper = billingSettingsMapper;
    this.messagingService = messagingService;
    this.notificationProps = notificationProps;
  }

  /**
   * Processes a verified Stripe event idempotently.
   *
   * @param event the verified Stripe event
   * @return {@code true} if the event was already processed (duplicate), {@code false} otherwise
   */
  public boolean process(final Event event) {
    final String externalEventId = event.getId();
    final String eventType = event.getType();

    if (webhookLogMapper.existsByExternalEventId(externalEventId)) {
      log.debug("Duplicate Stripe webhook event received, skipping: {}", externalEventId);
      return true;
    }

    final var webhookLog = new WebhookLog(
        UUID.randomUUID(),
        externalEventId,
        eventType,
        STATUS_RECEIVED,
        null,
        Instant.now(),
        null
    );
    webhookLogMapper.insert(webhookLog);

    try {
      dispatch(event);
      webhookLogMapper.updateStatus(externalEventId, STATUS_PROCESSED, null, Instant.now());
    } catch (final Exception e) {
      log.error("Failed to process Stripe webhook event {}: {}", externalEventId, e.getMessage(), e);
      webhookLogMapper.updateStatus(externalEventId, STATUS_FAILED, e.getMessage(), null);
    }

    return false;
  }

  private void dispatch(final Event event) {
    switch (event.getType()) {
      case EVENT_SUBSCRIPTION_CREATED  -> handleSubscriptionCreated(event);
      case EVENT_SUBSCRIPTION_UPDATED  -> handleSubscriptionUpsert(event);
      case EVENT_SUBSCRIPTION_DELETED  -> handleSubscriptionDeleted(event);
      case EVENT_INVOICE_PAID          -> handleInvoicePaid(event);
      case EVENT_INVOICE_PAYMENT_FAILED -> handleInvoicePaymentFailed(event);
      default -> log.debug("Unhandled Stripe event type: {}", event.getType());
    }
  }

  private void handleSubscriptionCreated(final Event event) {
    final var stripeSubscription = deserializeSubscription(event);
    final var subscription = mapToSubscription(stripeSubscription);
    subscriptionMapper.upsert(subscription);

    final String tenantKey = subscription.getTenantKey();
    if (tenantKey == null || tenantKey.isBlank()) {
      log.warn("Cannot send subscription activated notification — tenantKey absent. "
               + "externalSubscriptionId={}", subscription.getExternalSubscriptionId());
      return;
    }

    billingSettingsMapper.findByTenantKey(tenantKey).ifPresent(settings -> {
      final String email = resolveEmail(settings);
      if (email != null) {
        publishNotification(new NotificationEvent(
            email,
            notificationProps.defaultLocale(),
            NotificationEventType.SUBSCRIPTION_ACTIVATED,
            Map.of(
                "companyName", settings.getCompanyName() != null ? settings.getCompanyName() : "",
                "planId", subscription.getPlanId() != null ? subscription.getPlanId() : "",
                "externalSubscriptionId", subscription.getExternalSubscriptionId()
            ),
            Instant.now()));
      }
    });

    log.debug("Upserted subscription {} for tenant {}",
        subscription.getExternalSubscriptionId(), tenantKey);
  }

  private void handleSubscriptionUpsert(final Event event) {
    final var stripeSubscription = deserializeSubscription(event);
    final var subscription = mapToSubscription(stripeSubscription);
    subscriptionMapper.upsert(subscription);
    log.debug("Upserted subscription {} for tenant {}",
        subscription.getExternalSubscriptionId(), subscription.getTenantKey());
  }

  private void handleSubscriptionDeleted(final Event event) {
    final var stripeSubscription = deserializeSubscription(event);
    final var subscription = mapToSubscription(stripeSubscription);
    subscription.setStatus("canceled");
    subscription.setCanceledAt(Instant.now());
    subscriptionMapper.upsert(subscription);

    final String tenantKey = stripeSubscription.getMetadata() != null
        ? stripeSubscription.getMetadata().get("tenantKey")
        : null;

    if (tenantKey == null || tenantKey.isBlank()) {
      log.error("Cannot publish subscription.cancelled — tenantKey absent from Stripe metadata. "
                + "externalSubscriptionId={}", subscription.getExternalSubscriptionId());
      return;
    }

    messagingService.publishSubscriptionCancelled(tenantKey, subscription.getExternalSubscriptionId(),
        subscription.getSubjectType() != null ? subscription.getSubjectType() : "TENANT",
        subscription.getSubjectKey() != null ? subscription.getSubjectKey() : tenantKey);
    log.info("Published subscription.cancelled for tenant {}, subscription {}",
        tenantKey, subscription.getExternalSubscriptionId());

    billingSettingsMapper.findByTenantKey(tenantKey).ifPresent(settings -> {
      final String email = resolveEmail(settings);
      if (email != null) {
        publishNotification(new NotificationEvent(
            email,
            notificationProps.defaultLocale(),
            NotificationEventType.SUBSCRIPTION_CANCELLED,
            Map.of(
                "companyName", settings.getCompanyName() != null ? settings.getCompanyName() : "",
                "externalSubscriptionId", subscription.getExternalSubscriptionId()
            ),
            Instant.now()));
      }
    });
  }

  private void handleInvoicePaid(final Event event) {
    final var invoice = deserializeInvoice(event);
    if (invoice == null) {
      return;
    }
    billingSettingsMapper.findByExternalCustomerId(invoice.getCustomer()).ifPresent(settings -> {
      final String email = resolveEmail(settings);
      if (email != null) {
        publishNotification(new NotificationEvent(
            email,
            notificationProps.defaultLocale(),
            NotificationEventType.INVOICE_PAID,
            Map.of(
                "companyName", settings.getCompanyName() != null ? settings.getCompanyName() : "",
                "invoiceId", invoice.getId() != null ? invoice.getId() : "",
                "amountPaid", invoice.getAmountPaid() != null ? invoice.getAmountPaid() / 100.0 : 0.0,
                "currency", settings.getCurrency() != null ? settings.getCurrency() : "USD"
            ),
            Instant.now()));
      }
    });
  }

  private void handleInvoicePaymentFailed(final Event event) {
    final var invoice = deserializeInvoice(event);
    if (invoice == null) {
      return;
    }
    billingSettingsMapper.findByExternalCustomerId(invoice.getCustomer()).ifPresent(settings -> {
      final String email = resolveEmail(settings);
      if (email != null) {
        publishNotification(new NotificationEvent(
            email,
            notificationProps.defaultLocale(),
            NotificationEventType.PAYMENT_FAILED,
            Map.of(
                "companyName", settings.getCompanyName() != null ? settings.getCompanyName() : "",
                "invoiceId", invoice.getId() != null ? invoice.getId() : "",
                "amountDue", invoice.getAmountDue() != null ? invoice.getAmountDue() / 100.0 : 0.0,
                "currency", settings.getCurrency() != null ? settings.getCurrency() : "USD"
            ),
            Instant.now()));
      }
    });
  }

  private void publishNotification(final NotificationEvent event) {
    try {
      messagingService.publishNotification(event);
    } catch (final Exception e) {
      log.warn("Failed to publish billing notification: type={} recipient={}",
          event.getType(), event.getRecipientEmail(), e);
    }
  }

  private String resolveEmail(final BillingSettings settings) {
    if (settings.getBillingEmail() != null && !settings.getBillingEmail().isBlank()) {
      return settings.getBillingEmail();
    }
    log.warn("No billing email configured for tenantKey={}", settings.getTenantKey());
    return null;
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
        .orElse(null);
  }

  private Subscription mapToSubscription(final com.stripe.model.Subscription stripe) {
    final var sub = new Subscription();
    sub.setId(UUID.randomUUID());
    sub.setExternalSubscriptionId(stripe.getId());
    sub.setExternalCustomerId(stripe.getCustomer());
    sub.setStatus(stripe.getStatus());
    sub.setCancelAtPeriodEnd(Boolean.TRUE.equals(stripe.getCancelAtPeriodEnd()));

    if (stripe.getBillingCycleAnchor() != null) {
      sub.setCurrentPeriodStart(Instant.ofEpochSecond(stripe.getBillingCycleAnchor()));
    }
    if (stripe.getCanceledAt() != null) {
      sub.setCanceledAt(Instant.ofEpochSecond(stripe.getCanceledAt()));
    }
    if (stripe.getMetadata() != null) {
      sub.setTenantKey(stripe.getMetadata().get("tenantKey"));
    }
    if (stripe.getItems() != null && stripe.getItems().getData() != null
        && !stripe.getItems().getData().isEmpty()) {
      final com.stripe.model.SubscriptionItem item = stripe.getItems().getData().get(0);
      if (item.getPrice() != null) {
        sub.setPlanId(item.getPrice().getId());
      }
    }

    return sub;
  }
}
