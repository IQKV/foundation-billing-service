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

package com.iqkv.foundation.billingservice.infrastructure.config;

import java.time.Instant;
import java.util.UUID;

import com.iqkv.foundation.billingservice.infrastructure.messaging.MessagingService;
import com.iqkv.foundation.billingservice.infrastructure.persistence.SubscriptionMapper;
import com.iqkv.foundation.billingservice.infrastructure.persistence.WebhookLogMapper;
import com.iqkv.foundation.billingservice.shared.domain.Subscription;
import com.iqkv.foundation.billingservice.shared.domain.WebhookLog;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoint for inbound payment gateway webhook events.
 *
 * <p>Handles Stripe webhook delivery with signature verification, idempotency guard via
 * {@code webhook_log}, and dispatches subscription lifecycle events to the appropriate handlers.
 *
 * <p>Always returns HTTP 200 after the idempotency check to prevent Stripe from retrying
 * on business logic failures — failed events are recorded in {@code webhook_log} for
 * manual replay or investigation.
 */
@RestController
@RequestMapping("/api/v1/billing/webhooks")
public class PaymentWebhookRestResource {

  private static final Logger log = LoggerFactory.getLogger(PaymentWebhookRestResource.class);

  private static final String EVENT_SUBSCRIPTION_CREATED = "customer.subscription.created";
  private static final String EVENT_SUBSCRIPTION_UPDATED = "customer.subscription.updated";
  private static final String EVENT_SUBSCRIPTION_DELETED = "customer.subscription.deleted";

  private static final String STATUS_RECEIVED = "RECEIVED";
  private static final String STATUS_PROCESSED = "PROCESSED";
  private static final String STATUS_FAILED = "FAILED";

  private final WebhookLogMapper webhookLogMapper;
  private final SubscriptionMapper subscriptionMapper;
  private final MessagingService messagingService;

  @Value("${iqkv.stripe.webhook-secret}")
  private String webhookSecret;

  public PaymentWebhookRestResource(WebhookLogMapper webhookLogMapper,
                                    SubscriptionMapper subscriptionMapper,
                                    MessagingService messagingService) {
    this.webhookLogMapper = webhookLogMapper;
    this.subscriptionMapper = subscriptionMapper;
    this.messagingService = messagingService;
  }

  @PostMapping("/stripe")
  public ResponseEntity<Void> handleStripeWebhook(
      @RequestBody String payload,
      @RequestHeader("Stripe-Signature") String sigHeader) {

    // 1. Verify Stripe signature
    final Event event;
    try {
      event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
    } catch (SignatureVerificationException e) {
      log.warn("Invalid Stripe webhook signature: {}", e.getMessage());
      return ResponseEntity.badRequest().build();
    }

    final String externalEventId = event.getId();
    final String eventType = event.getType();

    // 2. Idempotency check — return 200 immediately for duplicate deliveries
    if (webhookLogMapper.existsByExternalEventId(externalEventId)) {
      log.debug("Duplicate Stripe webhook event received, skipping: {}", externalEventId);
      return ResponseEntity.ok().build();
    }

    // 3. Insert webhook_log row with status = RECEIVED
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

    // 4. Dispatch by event type
    try {
      dispatch(event, externalEventId, eventType);
      // 5. On success: mark PROCESSED
      webhookLogMapper.updateStatus(externalEventId, STATUS_PROCESSED, null, Instant.now());
    } catch (Exception e) {
      // 6. On failure: mark FAILED, still return 200 to prevent Stripe retry
      log.error("Failed to process Stripe webhook event {}: {}", externalEventId, e.getMessage(), e);
      webhookLogMapper.updateStatus(externalEventId, STATUS_FAILED, e.getMessage(), null);
    }

    return ResponseEntity.ok().build();
  }

  private void dispatch(Event event, String externalEventId, String eventType) {
    switch (eventType) {
      case EVENT_SUBSCRIPTION_CREATED, EVENT_SUBSCRIPTION_UPDATED -> handleSubscriptionUpsert(event);
      case EVENT_SUBSCRIPTION_DELETED -> handleSubscriptionDeleted(event);
      default -> log.debug("Unhandled Stripe event type: {}", eventType);
    }
  }

  private void handleSubscriptionUpsert(Event event) {
    final var stripeSubscription = deserializeSubscription(event);
    if (stripeSubscription == null) {
      return;
    }
    final var subscription = mapToSubscription(stripeSubscription);
    subscriptionMapper.upsert(subscription);
    log.debug("Upserted subscription {} for tenant {}",
        subscription.getExternalSubscriptionId(), subscription.getTenantKey());
  }

  private void handleSubscriptionDeleted(Event event) {
    final var stripeSubscription = deserializeSubscription(event);
    if (stripeSubscription == null) {
      return;
    }

    final var subscription = mapToSubscription(stripeSubscription);
    subscription.setStatus("canceled");
    subscription.setCanceledAt(Instant.now());
    subscriptionMapper.upsert(subscription);

    // Resolve tenantKey from subscription metadata to publish cancellation event
    final String tenantKey = stripeSubscription.getMetadata() != null
        ? stripeSubscription.getMetadata().get("tenantKey")
        : null;

    if (tenantKey == null || tenantKey.isBlank()) {
      log.error("Cannot publish subscription.cancelled — tenantKey absent from Stripe subscription metadata. "
                + "externalSubscriptionId={}", subscription.getExternalSubscriptionId());
      return;
    }

    messagingService.publishSubscriptionCancelled(tenantKey, subscription.getExternalSubscriptionId());
    log.info("Published subscription.cancelled for tenant {}, subscription {}",
        tenantKey, subscription.getExternalSubscriptionId());
  }

  private com.stripe.model.Subscription deserializeSubscription(Event event) {
    return event.getDataObjectDeserializer()
        .getObject()
        .filter(obj -> obj instanceof com.stripe.model.Subscription)
        .map(obj -> (com.stripe.model.Subscription) obj)
        .orElseGet(() -> {
          log.error("Could not deserialize Stripe Subscription from event {}", event.getId());
          return null;
        });
  }

  private Subscription mapToSubscription(com.stripe.model.Subscription stripe) {
    final var sub = new Subscription();
    sub.setId(UUID.randomUUID());
    sub.setExternalSubscriptionId(stripe.getId());
    sub.setExternalCustomerId(stripe.getCustomer());
    sub.setStatus(stripe.getStatus());
    sub.setCancelAtPeriodEnd(Boolean.TRUE.equals(stripe.getCancelAtPeriodEnd()));

    // currentPeriodStart / currentPeriodEnd were removed in Stripe SDK 31.x.
    // Use billingCycleAnchor as a proxy for the period start; period end is not
    // directly available on the Subscription object in this SDK version.
    if (stripe.getBillingCycleAnchor() != null) {
      sub.setCurrentPeriodStart(Instant.ofEpochSecond(stripe.getBillingCycleAnchor()));
    }
    if (stripe.getCanceledAt() != null) {
      sub.setCanceledAt(Instant.ofEpochSecond(stripe.getCanceledAt()));
    }

    // Resolve tenantKey and planId from metadata / items
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
