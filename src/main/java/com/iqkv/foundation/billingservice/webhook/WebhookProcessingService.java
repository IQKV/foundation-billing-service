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
import java.util.UUID;

import com.iqkv.foundation.billingservice.infrastructure.messaging.MessagingService;
import com.iqkv.foundation.billingservice.infrastructure.persistence.SubscriptionMapper;
import com.iqkv.foundation.billingservice.infrastructure.persistence.WebhookLogMapper;
import com.iqkv.foundation.billingservice.subscription.Subscription;
import com.iqkv.foundation.billingservice.webhook.WebhookLog;
import com.stripe.model.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Processes inbound Stripe webhook events.
 *
 * <p>Handles idempotency via {@code webhook_log}, dispatches subscription lifecycle
 * events, and publishes domain events to RabbitMQ on cancellation.
 *
 * <p>Status lifecycle: {@code RECEIVED} → {@code PROCESSED} on success,
 * or {@code RECEIVED} → {@code FAILED} on error.
 */
@Service
public class WebhookProcessingService {

  private static final Logger log = LoggerFactory.getLogger(WebhookProcessingService.class);

  private static final String EVENT_SUBSCRIPTION_CREATED = "customer.subscription.created";
  private static final String EVENT_SUBSCRIPTION_UPDATED = "customer.subscription.updated";
  private static final String EVENT_SUBSCRIPTION_DELETED = "customer.subscription.deleted";

  static final String STATUS_RECEIVED = "RECEIVED";
  static final String STATUS_PROCESSED = "PROCESSED";
  static final String STATUS_FAILED = "FAILED";

  private final WebhookLogMapper webhookLogMapper;
  private final SubscriptionMapper subscriptionMapper;
  private final MessagingService messagingService;

  public WebhookProcessingService(WebhookLogMapper webhookLogMapper,
                                  SubscriptionMapper subscriptionMapper,
                                  MessagingService messagingService) {
    this.webhookLogMapper = webhookLogMapper;
    this.subscriptionMapper = subscriptionMapper;
    this.messagingService = messagingService;
  }

  /**
   * Processes a verified Stripe event idempotently.
   *
   * @param event the verified Stripe event
   * @return {@code true} if the event was already processed (duplicate), {@code false} otherwise
   */
  public boolean process(Event event) {
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
    } catch (Exception e) {
      log.error("Failed to process Stripe webhook event {}: {}", externalEventId, e.getMessage(), e);
      webhookLogMapper.updateStatus(externalEventId, STATUS_FAILED, e.getMessage(), null);
    }

    return false;
  }

  private void dispatch(Event event) {
    switch (event.getType()) {
      case EVENT_SUBSCRIPTION_CREATED, EVENT_SUBSCRIPTION_UPDATED -> handleSubscriptionUpsert(event);
      case EVENT_SUBSCRIPTION_DELETED -> handleSubscriptionDeleted(event);
      default -> log.debug("Unhandled Stripe event type: {}", event.getType());
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
