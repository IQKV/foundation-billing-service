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

package com.iqkv.foundation.billingservice.gateway.adapter.lemonsqueezy;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iqkv.foundation.billingservice.gateway.GatewayType;
import com.iqkv.foundation.billingservice.gateway.command.CreateCheckoutSessionCommand;
import com.iqkv.foundation.billingservice.gateway.command.CreateCustomerCommand;
import com.iqkv.foundation.billingservice.gateway.command.CreateRefundCommand;
import com.iqkv.foundation.billingservice.gateway.command.UpdateSubscriptionCommand;
import com.iqkv.foundation.billingservice.gateway.event.GatewayInvoiceEvent;
import com.iqkv.foundation.billingservice.gateway.event.GatewayPaymentFailureEvent;
import com.iqkv.foundation.billingservice.gateway.event.GatewayRefundEvent;
import com.iqkv.foundation.billingservice.gateway.event.GatewaySubscriptionEvent;
import com.iqkv.foundation.billingservice.gateway.event.GatewayWebhookEvent;
import com.iqkv.foundation.billingservice.gateway.port.PaymentGatewayPort;
import com.iqkv.foundation.billingservice.infrastructure.config.ConditionalOnGateway;
import com.iqkv.foundation.billingservice.infrastructure.config.LemonSqueezyConfigurationProperties;
import com.iqkv.foundation.billingservice.plan.Plan;
import com.iqkv.foundation.billingservice.shared.exception.PaymentGatewayException;
import com.iqkv.foundation.billingservice.shared.exception.WebhookProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@ConditionalOnGateway(GatewayType.LEMON_SQUEEZY)
public class LemonSqueezyGatewayAdapter implements PaymentGatewayPort {

  private static final Logger log = LoggerFactory.getLogger(LemonSqueezyGatewayAdapter.class);

  private static final String LS_SUBSCRIPTION_CREATED = "subscription_created";
  private static final String LS_SUBSCRIPTION_UPDATED = "subscription_updated";
  private static final String LS_SUBSCRIPTION_CANCELLED = "subscription_cancelled";
  private static final String LS_SUBSCRIPTION_RESUMED = "subscription_resumed";
  private static final String LS_SUBSCRIPTION_EXPIRED = "subscription_expired";
  private static final String LS_SUBSCRIPTION_PAUSED = "subscription_paused";
  private static final String LS_SUBSCRIPTION_UNPAUSED = "subscription_unpaused";
  private static final String LS_PAYMENT_SUCCESS = "subscription_payment_success";
  private static final String LS_PAYMENT_RECOVERED = "subscription_payment_recovered";
  private static final String LS_PAYMENT_FAILED = "subscription_payment_failed";
  private static final String LS_ORDER_REFUNDED = "order_refunded";

  private final LemonSqueezyConfigurationProperties config;
  private final RestClient restClient;
  private final ObjectMapper objectMapper;

  public LemonSqueezyGatewayAdapter(
      final LemonSqueezyConfigurationProperties config,
      final RestClient lemonSqueezyRestClient,
      final ObjectMapper objectMapper) {
    this.config = config;
    this.restClient = lemonSqueezyRestClient;
    this.objectMapper = objectMapper;
  }

  @Override
  public GatewayType getGatewayType() {
    return GatewayType.LEMON_SQUEEZY;
  }

  @Override
  public String createCustomer(final CreateCustomerCommand command) {
    if (command.email() == null || command.email().isBlank()) {
      throw new PaymentGatewayException(
          "Lemon Squeezy requires a customer email address; none provided for: " + command.name());
    }
    final Map<String, Object> body = Map.of("data", Map.of(
        "type", "customers",
        "attributes", Map.of(
            "name", command.name(),
            "email", command.email(),
            "store_id", Integer.parseInt(config.storeId()))));
    try {
      final JsonNode response = restClient.post().uri("/customers")
          .body(body).retrieve().body(JsonNode.class);
      final String customerId = response.path("data").path("id").asText();
      log.debug("Created LS customer {} for name={}", customerId, command.name());
      return customerId;
    } catch (final RestClientException e) {
      throw new PaymentGatewayException("Failed to create LS customer: " + command.name(), e);
    }
  }

  @Override
  public String createCheckoutSession(final CreateCheckoutSessionCommand command) {
    final Map<String, Object> customData = new HashMap<>(command.metadata());

    final Map<String, Object> checkoutData = new HashMap<>();
    checkoutData.put("custom", customData);

    final Map<String, Object> attributes = new HashMap<>();
    attributes.put("checkout_data", checkoutData);
    attributes.put("product_options", Map.of("redirect_url", command.successUrl()));

    if (command.trialPeriodDays() != null && command.trialPeriodDays() > 0) {
      final String trialEnd = Instant.now()
          .plus(command.trialPeriodDays(), java.time.temporal.ChronoUnit.DAYS).toString();
      attributes.put("expires_at", null);
      checkoutData.put("subscription_data", Map.of("trial_ends_at", trialEnd));
    }

    final Map<String, Object> body = Map.of("data", Map.of(
        "type", "checkouts",
        "attributes", attributes,
        "relationships", Map.of(
            "store", Map.of("data", Map.of("type", "stores", "id", config.storeId())),
            "variant", Map.of("data", Map.of("type", "variants", "id", command.priceId())))));
    try {
      final JsonNode response = restClient.post().uri("/checkouts")
          .body(body).retrieve().body(JsonNode.class);
      final String url = response.path("data").path("attributes").path("url").asText();
      log.debug("Created LS checkout for customer {}: {}", command.customerId(), url);
      return url;
    } catch (final RestClientException e) {
      throw new PaymentGatewayException(
          "Failed to create LS checkout for customer: " + command.customerId(), e);
    }
  }

  @Override
  public void updateSubscription(final UpdateSubscriptionCommand command) {
    final Map<String, Object> attrs = new HashMap<>();
    if (command.priceId() != null) {
      attrs.put("variant_id", Integer.parseInt(command.priceId()));
    }
    if (command.quantity() != null) {
      attrs.put("quantity", command.quantity());
    }
    if ("none".equalsIgnoreCase(command.prorationBehavior())) {
      attrs.put("immediate_payment", false);
    } else if (command.prorationBehavior() != null) {
      attrs.put("immediate_payment", true);
    }
    patch("/subscriptions/" + command.subscriptionId(), attrs);
    log.debug("Updated LS subscription {}", command.subscriptionId());
  }

  @Override
  public void cancelSubscription(final String subscriptionId, final boolean cancelAtPeriodEnd) {
    if (cancelAtPeriodEnd) {
      patch("/subscriptions/" + subscriptionId, Map.of("cancelled", true));
      log.debug("Set LS subscription {} to cancel at period end", subscriptionId);
    } else {
      try {
        restClient.delete().uri("/subscriptions/" + subscriptionId).retrieve().toBodilessEntity();
        log.debug("Deleted LS subscription {} immediately", subscriptionId);
      } catch (final RestClientException e) {
        throw new PaymentGatewayException("Failed to cancel LS subscription: " + subscriptionId, e);
      }
    }
  }

  @Override
  public void pauseSubscription(final String subscriptionId) {
    patch("/subscriptions/" + subscriptionId,
        Map.of("pause", Map.of("mode", "void")));
    log.debug("Paused LS subscription {}", subscriptionId);
  }

  @Override
  public void reactivateSubscription(final String subscriptionId) {
    final Map<String, Object> attrs = new HashMap<>();
    attrs.put("pause", null);
    patch("/subscriptions/" + subscriptionId, attrs);
    log.debug("Reactivated LS subscription {}", subscriptionId);
  }

  @Override
  public String createRefund(final CreateRefundCommand command) {
    final Map<String, Object> attrs = new HashMap<>();
    attrs.put("order_id", Integer.parseInt(command.paymentId()));
    if (command.amount() != null) {
      attrs.put("amount", command.amount());
    }

    final Map<String, Object> body = Map.of("data", Map.of("type", "refunds", "attributes", attrs));
    try {
      final JsonNode response = restClient.post().uri("/refunds")
          .body(body).retrieve().body(JsonNode.class);
      final String refundId = response.path("data").path("id").asText();
      log.debug("Created LS refund {} for order {}", refundId, command.paymentId());
      return refundId;
    } catch (final RestClientException e) {
      throw new PaymentGatewayException(
          "Failed to create LS refund for order: " + command.paymentId(), e);
    }
  }

  @Override
  public String syncProduct(final Plan plan) {
    final String variantId = plan.getExternalPriceId();
    if (variantId == null || variantId.isBlank()) {
      log.warn("Plan {} has no externalVariantId configured. Set iqkv.billing.plan-catalog.products.{}.externalVariantId before going live.",
          plan.getPlanCode(), plan.getPlanCode());
      return "";
    }
    try {
      final JsonNode response = restClient.get()
          .uri("/variants/" + variantId).retrieve().body(JsonNode.class);
      final String status = response.path("data").path("attributes")
          .path("status").asText();
      if (!"published".equalsIgnoreCase(status)) {
        log.warn("LS variant {} for plan {} has status '{}' — expected 'published'",
            variantId, plan.getPlanCode(), status);
      } else {
        log.debug("Verified LS variant {} for plan {}", variantId, plan.getPlanCode());
      }
    } catch (final RestClientException e) {
      log.warn("Could not verify LS variant {} for plan {}: {}",
          variantId, plan.getPlanCode(), e.getMessage());
    }
    return variantId;
  }

  @Override
  public Optional<GatewayWebhookEvent> verifyAndParseWebhookEvent(
      final String payload, final String signature) {
    final String computed;
    try {
      final Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(
          config.webhookSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      final byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
      computed = HexFormat.of().formatHex(hash);
    } catch (final NoSuchAlgorithmException | InvalidKeyException e) {
      throw new WebhookProcessingException("HMAC computation failed", e);
    }
    if (!MessageDigest.isEqual(computed.getBytes(), signature.getBytes())) {
      throw new WebhookProcessingException("Invalid Lemon Squeezy webhook signature");
    }

    final JsonNode root;
    try {
      root = objectMapper.readTree(payload);
    } catch (final JsonProcessingException e) {
      throw new WebhookProcessingException("Failed to parse LS webhook payload", e);
    }

    final String eventName = root.path("meta").path("event_name").asText();
    final String eventId = root.path("meta").path("event_id").asText(
        root.path("data").path("id").asText("unknown"));
    final Instant now = Instant.now();

    return switch (eventName) {
      case LS_SUBSCRIPTION_CREATED ->
          Optional.of(toSubscriptionEvent(root, eventId, "subscription.created", now));
      case LS_SUBSCRIPTION_UPDATED, LS_SUBSCRIPTION_RESUMED ->
          Optional.of(toSubscriptionEvent(root, eventId, "subscription.updated", now));
      case LS_SUBSCRIPTION_CANCELLED, LS_SUBSCRIPTION_EXPIRED ->
          Optional.of(toSubscriptionEvent(root, eventId, "subscription.deleted", now));
      case LS_SUBSCRIPTION_PAUSED ->
          Optional.of(toSubscriptionEventWithStatus(root, eventId, now, "paused"));
      case LS_SUBSCRIPTION_UNPAUSED ->
          Optional.of(toSubscriptionEventWithStatus(root, eventId, now, "active"));
      case LS_PAYMENT_SUCCESS, LS_PAYMENT_RECOVERED ->
          Optional.of(toInvoiceEvent(root, eventId, "invoice.payment_succeeded", now));
      case LS_PAYMENT_FAILED ->
          Optional.of(toPaymentFailureEvent(root, eventId, now));
      case LS_ORDER_REFUNDED ->
          Optional.of(toRefundEvent(root, eventId, now));
      default -> {
        log.debug("Unhandled LS event type: {}", eventName);
        yield Optional.empty();
      }
    };
  }

  @Override
  public String createPortalSession(final String customerId, final String returnUrl) {
    final Map<String, Object> body = Map.of("data", Map.of(
        "type", "customer-portal-sessions",
        "attributes", Map.of("customer_id", Integer.parseInt(customerId))));
    try {
      final JsonNode response = restClient.post()
          .uri("/customer-portal-sessions")
          .body(body).retrieve().body(JsonNode.class);
      return response.path("data").path("attributes").path("url").asText();
    } catch (final RestClientException e) {
      throw new PaymentGatewayException(
          "Failed to create LS customer portal session for: " + customerId, e);
    }
  }

  private void patch(final String path, final Map<String, Object> attributes) {
    try {
      final String id = path.substring(path.lastIndexOf('/') + 1);
      final String type = path.contains("subscription") ? "subscriptions" : path;
      final Map<String, Object> body = Map.of("data", Map.of("type", type, "id", id, "attributes", attributes));
      restClient.patch().uri(path).body(body).retrieve().toBodilessEntity();
    } catch (final RestClientException e) {
      throw new PaymentGatewayException("Failed LS PATCH " + path + ": " + e.getMessage(), e);
    }
  }

  private GatewaySubscriptionEvent toSubscriptionEvent(
      final JsonNode root, final String eventId, final String normalizedType, final Instant now) {
    final JsonNode attrs = root.path("data").path("attributes");
    final JsonNode meta = root.path("meta");

    final Map<String, String> metadata = new HashMap<>();
    meta.path("custom_data").fields()
        .forEachRemaining(e -> metadata.put(e.getKey(), e.getValue().asText()));

    return new GatewaySubscriptionEvent(
        eventId,
        normalizedType,
        now,
        GatewayType.LEMON_SQUEEZY.name(),
        root.path("data").path("id").asText(),
        attrs.path("customer_id").asText(),
        attrs.path("status").asText(),
        String.valueOf(attrs.path("variant_id").asLong()),
        attrs.path("quantity").isNull() ? null : attrs.path("quantity").asLong(),
        null,
        parseInstant(attrs.path("trial_ends_at")),
        null,
        parseInstant(attrs.path("renews_at")),
        attrs.path("cancelled").asBoolean(false),
        parseInstant(attrs.path("ends_at")),
        Collections.unmodifiableMap(metadata));
  }

  private GatewaySubscriptionEvent toSubscriptionEventWithStatus(
      final JsonNode root, final String eventId, final Instant now, final String forcedStatus) {
    final GatewaySubscriptionEvent base =
        toSubscriptionEvent(root, eventId, "subscription.updated", now);
    return new GatewaySubscriptionEvent(
        base.eventId(), base.eventType(), base.occurredAt(), base.gatewayType(),
        base.externalSubscriptionId(), base.externalCustomerId(), forcedStatus,
        base.planId(), base.quantity(), base.trialStart(), base.trialEnd(),
        base.currentPeriodStart(), base.currentPeriodEnd(),
        base.cancelAtPeriodEnd(), base.canceledAt(), base.metadata());
  }

  private GatewayInvoiceEvent toInvoiceEvent(
      final JsonNode root, final String eventId, final String type, final Instant now) {
    final JsonNode attrs = root.path("data").path("attributes");
    return new GatewayInvoiceEvent(
        eventId,
        type,
        now,
        GatewayType.LEMON_SQUEEZY.name(),
        attrs.path("identifier").asText(eventId),
        String.valueOf(attrs.path("customer_id").asLong()),
        String.valueOf(attrs.path("subscription_id").asLong()),
        root.path("data").path("id").asText(),
        toMinorUnits(attrs.path("total")),
        toMinorUnits(attrs.path("total")),
        attrs.path("currency").asText("USD"));
  }

  private GatewayPaymentFailureEvent toPaymentFailureEvent(
      final JsonNode root, final String eventId, final Instant now) {
    final JsonNode attrs = root.path("data").path("attributes");
    return new GatewayPaymentFailureEvent(
        eventId,
        "invoice.payment_failed",
        now,
        GatewayType.LEMON_SQUEEZY.name(),
        attrs.path("identifier").asText(eventId),
        String.valueOf(attrs.path("customer_id").asLong()),
        String.valueOf(attrs.path("subscription_id").asLong()),
        toMinorUnits(attrs.path("total")),
        attrs.path("currency").asText("USD"),
        "payment_failed");
  }

  private GatewayRefundEvent toRefundEvent(
      final JsonNode root, final String eventId, final Instant now) {
    final JsonNode attrs = root.path("data").path("attributes");
    return new GatewayRefundEvent(
        eventId,
        "charge.refunded",
        now,
        GatewayType.LEMON_SQUEEZY.name(),
        root.path("data").path("id").asText(),
        root.path("data").path("id").asText(),
        String.valueOf(attrs.path("customer_id").asLong()),
        toMinorUnits(attrs.path("amount")),
        attrs.path("currency").asText("USD"),
        "succeeded");
  }

  private static Instant parseInstant(final JsonNode node) {
    if (node == null || node.isNull() || node.asText().isBlank()) {
      return null;
    }
    try {
      return Instant.parse(node.asText());
    } catch (final DateTimeParseException e) {
      return null;
    }
  }

  private static Long toMinorUnits(final JsonNode node) {
    if (node == null || node.isNull()) {
      return null;
    }
    try {
      return Math.round(Double.parseDouble(node.asText()) * 100);
    } catch (final NumberFormatException e) {
      return null;
    }
  }

  private static class HexFormat {
    public static HexFormat of() {
      return new HexFormat();
    }

    public String formatHex(byte[] bytes) {
      final StringBuilder sb = new StringBuilder(bytes.length * 2);
      for (byte b : bytes) {
        sb.append(String.format("%02x", b));
      }
      return sb.toString();
    }
  }
}
