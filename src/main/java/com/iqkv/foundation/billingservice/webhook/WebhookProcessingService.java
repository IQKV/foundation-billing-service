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

import com.iqkv.foundation.billingservice.gateway.event.GatewayInvoiceEvent;
import com.iqkv.foundation.billingservice.gateway.event.GatewayPaymentFailureEvent;
import com.iqkv.foundation.billingservice.gateway.event.GatewayRefundEvent;
import com.iqkv.foundation.billingservice.gateway.event.GatewaySubscriptionEvent;
import com.iqkv.foundation.billingservice.gateway.event.GatewayWebhookEvent;
import com.iqkv.foundation.billingservice.infrastructure.config.NotificationConfigurationProperties;
import com.iqkv.foundation.billingservice.infrastructure.messaging.MessagingService;
import com.iqkv.foundation.billingservice.infrastructure.messaging.NotificationEvent;
import com.iqkv.foundation.billingservice.infrastructure.messaging.NotificationEventType;
import com.iqkv.foundation.billingservice.infrastructure.persistence.BillingSettingsMapper;
import com.iqkv.foundation.billingservice.infrastructure.persistence.PlanMapper;
import com.iqkv.foundation.billingservice.infrastructure.persistence.RefundMapper;
import com.iqkv.foundation.billingservice.infrastructure.persistence.SubscriptionMapper;
import com.iqkv.foundation.billingservice.infrastructure.persistence.UserBillingSettingsMapper;
import com.iqkv.foundation.billingservice.infrastructure.persistence.WebhookLogMapper;
import com.iqkv.foundation.billingservice.plan.PlanEligibilityPolicy;
import com.iqkv.foundation.billingservice.settings.BillingSettings;
import com.iqkv.foundation.billingservice.subscription.Refund;
import com.iqkv.foundation.billingservice.subscription.SubjectType;
import com.iqkv.foundation.billingservice.subscription.Subscription;
import com.iqkv.foundation.billingservice.subscription.SubscriptionSubject;
import com.iqkv.foundation.billingservice.subscription.SubscriptionSubjectResolver;
import com.iqkv.foundation.billingservice.userbilling.UserBillingSettingsService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Gateway-agnostic webhook processing orchestrator.
 *
 * <p>Handles idempotency via {@code webhook_log}, dispatches subscription lifecycle
 * events, and publishes domain events and notification emails to RabbitMQ.
 * Operates on normalized {@link GatewayWebhookEvent} instances — no payment gateway
 * SDK types are referenced here.
 *
 * <p>Status lifecycle: {@code RECEIVED} → {@code PROCESSED} on success,
 * or {@code RECEIVED} → {@code FAILED} on error.
 */
@Service
public class WebhookProcessingService {

  private static final Logger log = LoggerFactory.getLogger(WebhookProcessingService.class);

  static final String STATUS_RECEIVED = "RECEIVED";
  static final String STATUS_PROCESSED = "PROCESSED";
  static final String STATUS_FAILED = "FAILED";

  private final WebhookLogMapper webhookLogMapper;
  private final SubscriptionMapper subscriptionMapper;
  private final RefundMapper refundMapper;
  private final BillingSettingsMapper billingSettingsMapper;
  private final UserBillingSettingsMapper userBillingSettingsMapper;
  private final PlanMapper planMapper;
  private final MessagingService messagingService;
  private final NotificationConfigurationProperties notificationProps;
  private final SubscriptionSubjectResolver subjectResolver;
  private final PlanEligibilityPolicy planEligibilityPolicy;
  private final MeterRegistry meterRegistry;

  /**
   * Optional — only present in {@code SINGLE_TENANT} mode.
   * Injected via {@code @Autowired(required = false)} so the service remains constructable
   * in {@code MULTI_TENANT} mode where the bean is not registered.
   */
  @Autowired(required = false)
  private UserBillingSettingsService userBillingSettingsService;

  public WebhookProcessingService(final WebhookLogMapper webhookLogMapper,
                                  final SubscriptionMapper subscriptionMapper,
                                  final RefundMapper refundMapper,
                                  final BillingSettingsMapper billingSettingsMapper,
                                  final UserBillingSettingsMapper userBillingSettingsMapper,
                                  final PlanMapper planMapper,
                                  final MessagingService messagingService,
                                  final NotificationConfigurationProperties notificationProps,
                                  final SubscriptionSubjectResolver subjectResolver,
                                  final PlanEligibilityPolicy planEligibilityPolicy,
                                  final MeterRegistry meterRegistry) {
    this.webhookLogMapper = webhookLogMapper;
    this.subscriptionMapper = subscriptionMapper;
    this.refundMapper = refundMapper;
    this.billingSettingsMapper = billingSettingsMapper;
    this.userBillingSettingsMapper = userBillingSettingsMapper;
    this.planMapper = planMapper;
    this.messagingService = messagingService;
    this.notificationProps = notificationProps;
    this.subjectResolver = subjectResolver;
    this.planEligibilityPolicy = planEligibilityPolicy;
    this.meterRegistry = meterRegistry;
  }

  /**
   * Processes a normalized gateway webhook event idempotently.
   *
   * @param event the normalized gateway event
   * @return {@code true} if the event was already processed (duplicate), {@code false} otherwise
   */
  public boolean process(final GatewayWebhookEvent event) {
    final String externalEventId = event.eventId();
    final String eventType = event.eventType();

    if (webhookLogMapper.existsByExternalEventId(externalEventId)) {
      log.debug("Duplicate webhook event received, skipping: {}", externalEventId);
      meterRegistry.counter("billing_webhooks_total", "event_type", eventType, "status", "duplicate").increment();
      return true;
    }

    webhookLogMapper.insert(new WebhookLog(
        UUID.randomUUID(),
        externalEventId,
        eventType,
        STATUS_RECEIVED,
        null,
        Instant.now(),
        null
    ));

    final Timer.Sample sample = Timer.start(meterRegistry);
    try {
      dispatch(event);
      webhookLogMapper.updateStatus(externalEventId, STATUS_PROCESSED, null, Instant.now());
      sample.stop(meterRegistry.timer("billing_webhooks_processing_duration_seconds", "event_type", eventType, "status", "processed"));
      meterRegistry.counter("billing_webhooks_total", "event_type", eventType, "status", "processed").increment();
    } catch (final Exception e) {
      log.error("Failed to process webhook event {}: {}", externalEventId, e.getMessage(), e);
      webhookLogMapper.updateStatus(externalEventId, STATUS_FAILED, e.getMessage(), null);
      sample.stop(meterRegistry.timer("billing_webhooks_processing_duration_seconds", "event_type", eventType, "status", "failed"));
      meterRegistry.counter("billing_webhooks_total", "event_type", eventType, "status", "failed").increment();
      meterRegistry.counter("billing_webhooks_errors_total", "event_type", eventType, "exception", e.getClass().getSimpleName()).increment();
    }

    return false;
  }

  // -------------------------------------------------------------------------
  // Dispatch
  // -------------------------------------------------------------------------

  private void dispatch(final GatewayWebhookEvent event) {
    switch (event) {
      case GatewaySubscriptionEvent sub when sub.isCreated() -> handleSubscriptionCreated(sub);
      case GatewaySubscriptionEvent sub when sub.isUpdated() -> handleSubscriptionUpdated(sub);
      case GatewaySubscriptionEvent sub when sub.isDeleted() -> handleSubscriptionDeleted(sub);
      case GatewaySubscriptionEvent sub -> log.debug("Unhandled subscription event type: {}", sub.eventType());
      case GatewayInvoiceEvent inv -> handleInvoiceEvent(inv);
      case GatewayPaymentFailureEvent pay -> handlePaymentFailed(pay);
      case GatewayRefundEvent ref -> handleRefundEvent(ref);
    }
  }

  // -------------------------------------------------------------------------
  // Subscription handlers
  // -------------------------------------------------------------------------

  private void handleSubscriptionCreated(final GatewaySubscriptionEvent event) {
    final var subscription = toSubscription(event);

    final String tenantKey = subscription.getTenantKey();
    final UUID userId = resolveUserId(event.metadata());
    final SubscriptionSubject subject = subjectResolver.resolveSubject(tenantKey, userId);

    // In SINGLE_TENANT mode, ensure user billing settings exist before persisting subscription
    if (SubjectType.USER.equals(subject.type()) && userId != null
        && userBillingSettingsService != null) {
      userBillingSettingsService.getOrCreateUserBillingSettings(userId);
    }

    // Validate plan eligibility before persisting
    final String planCode = event.metadata().get("planCode");
    if (planCode != null) {
      planEligibilityPolicy.validatePlanEligibility(planCode, subject.type());
    }

    subscription.setSubjectType(subject.type().name());
    subscription.setSubjectKey(subject.key());
    subscriptionMapper.upsert(subscription);

    meterRegistry.counter("billing_subscriptions_total",
        "action", "created",
        "plan_id", nullToEmpty(subscription.getPlanId()),
        "subject_type", subscription.getSubjectType()
    ).increment();

    messagingService.publishSubscriptionCreated(
        tenantKey,
        subscription.getExternalSubscriptionId(),
        subject.type().name(),
        subject.key(),
        resolvePlanCode(subscription.getPlanId())
    );
    log.info("Published subscription.created for tenant={}, subscription={}",
        tenantKey, subscription.getExternalSubscriptionId());

    publishSubscriptionActivatedNotification(subscription, subject, userId);
  }

  private void handleSubscriptionUpdated(final GatewaySubscriptionEvent event) {
    final var subscription = toSubscription(event);
    subscriptionMapper.upsert(subscription);

    meterRegistry.counter("billing_subscriptions_total",
        "action", "updated",
        "plan_id", nullToEmpty(subscription.getPlanId()),
        "status", nullToEmpty(subscription.getStatus())
    ).increment();

    final String tenantKey = subscription.getTenantKey();
    if (tenantKey != null && !tenantKey.isBlank()) {
      final var subjectContext = resolveSubjectFromSubscription(subscription.getExternalSubscriptionId(), tenantKey);

      messagingService.publishSubscriptionUpdated(
          tenantKey,
          subscription.getExternalSubscriptionId(),
          subjectContext.type().name(),
          subjectContext.key(),
          resolvePlanCode(subscription.getPlanId())
      );
      log.info("Published subscription.updated for tenant={}, subscription={}, planCode={}",
          tenantKey, subscription.getExternalSubscriptionId(), resolvePlanCode(subscription.getPlanId()));

      billingSettingsMapper.findByTenantKey(tenantKey).ifPresent(settings -> {
        final String email = resolveEmail(settings);
        if (email != null) {
          publishNotification(new NotificationEvent(
              email,
              notificationProps.defaultLocale(),
              NotificationEventType.SUBSCRIPTION_UPDATED,
              Map.of(
                  "companyName", nullToEmpty(settings.getCompanyName()),
                  "planId", nullToEmpty(subscription.getPlanId()),
                  "status", nullToEmpty(subscription.getStatus()),
                  "externalSubscriptionId", subscription.getExternalSubscriptionId()
              ),
              Instant.now()));
        }
      });
    }

    log.debug("Upserted subscription {} for tenant {}",
        subscription.getExternalSubscriptionId(), tenantKey);
  }

  private void handleSubscriptionDeleted(final GatewaySubscriptionEvent event) {
    final var subscription = toSubscription(event);
    subscription.setStatus("canceled");
    subscription.setCanceledAt(event.canceledAt() != null ? event.canceledAt() : Instant.now());
    subscriptionMapper.upsert(subscription);

    meterRegistry.counter("billing_subscriptions_total",
        "action", "deleted",
        "plan_id", nullToEmpty(subscription.getPlanId()),
        "subject_type", subscription.getSubjectType() != null ? subscription.getSubjectType() : SubjectType.TENANT.name()
    ).increment();

    final String tenantKey = event.metadata().get("tenantKey");
    if (tenantKey == null || tenantKey.isBlank()) {
      log.error("Cannot publish subscription.cancelled — tenantKey absent from metadata. "
                + "externalSubscriptionId={}", subscription.getExternalSubscriptionId());
      return;
    }

    messagingService.publishSubscriptionCancelled(
        tenantKey,
        subscription.getExternalSubscriptionId(),
        subscription.getSubjectType() != null ? subscription.getSubjectType() : SubjectType.TENANT.name(),
        subscription.getSubjectKey() != null ? subscription.getSubjectKey() : tenantKey
    );
    log.info("Published subscription.cancelled for tenant={}, subscription={}",
        tenantKey, subscription.getExternalSubscriptionId());

    billingSettingsMapper.findByTenantKey(tenantKey).ifPresent(settings -> {
      final String email = resolveEmail(settings);
      if (email != null) {
        publishNotification(new NotificationEvent(
            email,
            notificationProps.defaultLocale(),
            NotificationEventType.SUBSCRIPTION_CANCELLED,
            Map.of(
                "companyName", nullToEmpty(settings.getCompanyName()),
                "externalSubscriptionId", subscription.getExternalSubscriptionId()
            ),
            Instant.now()));
      }
    });
  }

  // -------------------------------------------------------------------------
  // Invoice / payment handlers
  // -------------------------------------------------------------------------

  private void handleInvoiceEvent(final GatewayInvoiceEvent event) {
    final var billingSettingsOpt = billingSettingsMapper.findByExternalCustomerId(event.externalCustomerId());
    if (billingSettingsOpt.isEmpty()) {
      log.warn("Cannot publish {} — no billing settings found for customer={}",
          event.eventType(), event.externalCustomerId());
      return;
    }

    final var billingSettings = billingSettingsOpt.get();
    final String tenantKey = billingSettings.getTenantKey();
    final var subjectContext = resolveSubjectFromSubscription(event.externalSubscriptionId(), tenantKey);
    final String currency = billingSettings.getCurrency() != null ? billingSettings.getCurrency() : "USD";

    switch (event.eventType()) {
      case "invoice.payment_succeeded" -> {
        meterRegistry.counter("billing_payments_total", "status", "success", "currency", currency).increment();
        if (event.amountPaid() != null) {
          meterRegistry.counter("billing_revenue_total", "currency", currency).increment(event.amountPaid() / 100.0);
        }

        messagingService.publishInvoicePaid(
            tenantKey,
            event.externalInvoiceId(),
            event.externalCustomerId(),
            event.externalSubscriptionId(),
            event.amountPaid(),
            currency,
            subjectContext.type().name(),
            subjectContext.key()
        );
        log.info("Published invoice.paid for tenant={}, invoice={}", tenantKey, event.externalInvoiceId());

        final String email = resolveEmail(billingSettings);
        if (email != null) {
          publishNotification(new NotificationEvent(
              email,
              notificationProps.defaultLocale(),
              NotificationEventType.INVOICE_PAID,
              Map.of(
                  "companyName", nullToEmpty(billingSettings.getCompanyName()),
                  "invoiceId", nullToEmpty(event.externalInvoiceId()),
                  "amountPaid", event.amountPaid() != null ? event.amountPaid() / 100.0 : 0.0,
                  "currency", currency
              ),
              Instant.now()));
        }
      }
      case "invoice.created" -> {
        messagingService.publishInvoiceCreated(
            tenantKey,
            event.externalInvoiceId(),
            event.externalCustomerId(),
            event.externalSubscriptionId(),
            event.amountDue(),
            currency,
            subjectContext.type().name(),
            subjectContext.key()
        );
        log.info("Published invoice.created for tenant={}, invoice={}", tenantKey, event.externalInvoiceId());
      }
      case "invoice.finalized" -> {
        messagingService.publishInvoiceFinalized(
            tenantKey,
            event.externalInvoiceId(),
            event.externalCustomerId(),
            event.externalSubscriptionId(),
            event.amountDue(),
            currency,
            subjectContext.type().name(),
            subjectContext.key()
        );
        log.info("Published invoice.finalized for tenant={}, invoice={}", tenantKey, event.externalInvoiceId());
      }
      case "invoice.updated" -> {
        messagingService.publishInvoiceUpdated(
            tenantKey,
            event.externalInvoiceId(),
            event.externalCustomerId(),
            event.externalSubscriptionId(),
            event.amountDue(),
            currency,
            subjectContext.type().name(),
            subjectContext.key()
        );
        log.info("Published invoice.updated for tenant={}, invoice={}", tenantKey, event.externalInvoiceId());
      }
      default -> log.debug("Unhandled invoice event type: {}", event.eventType());
    }
  }

  private void handleRefundEvent(final GatewayRefundEvent event) {
    final var billingSettingsOpt = billingSettingsMapper.findByExternalCustomerId(event.externalCustomerId());
    if (billingSettingsOpt.isEmpty()) {
      log.warn("Cannot publish refund.created — no billing settings found for customer={}",
          event.externalCustomerId());
      return;
    }

    final var billingSettings = billingSettingsOpt.get();
    final String tenantKey = billingSettings.getTenantKey();

    final Refund refund = new Refund(
        null,
        tenantKey,
        event.externalRefundId(),
        event.externalPaymentId(),
        event.externalCustomerId(),
        event.amountRefunded(),
        event.currency(),
        event.status(),
        event.occurredAt(),
        null,
        null
    );
    refundMapper.upsert(refund);

    messagingService.publishRefundCreated(
        tenantKey,
        event.externalRefundId(),
        event.externalPaymentId(),
        event.externalCustomerId(),
        event.amountRefunded(),
        event.currency(),
        event.status(),
        SubjectType.TENANT.name(), // Default to TENANT for now as refund might not have subscription info easily available
        tenantKey
    );
    log.info("Published refund.created for tenant={}, refund={}", tenantKey, event.externalRefundId());

    final String email = resolveEmail(billingSettings);
    if (email != null) {
      publishNotification(new NotificationEvent(
          email,
          notificationProps.defaultLocale(),
          NotificationEventType.REFUND_CREATED,
          Map.of(
              "companyName", nullToEmpty(billingSettings.getCompanyName()),
              "externalRefundId", nullToEmpty(event.externalRefundId()),
              "amountRefunded", event.amountRefunded() != null ? event.amountRefunded() : 0L,
              "currency", event.currency() != null ? event.currency().toUpperCase() : "USD"
          ),
          Instant.now()));
    }
  }

  private void handlePaymentFailed(final GatewayPaymentFailureEvent event) {
    final var billingSettingsOpt = billingSettingsMapper.findByExternalCustomerId(event.externalCustomerId());
    if (billingSettingsOpt.isEmpty()) {
      log.warn("Cannot publish payment.failed — no billing settings found for customer={}",
          event.externalCustomerId());
      return;
    }

    final var billingSettings = billingSettingsOpt.get();
    final String tenantKey = billingSettings.getTenantKey();
    final var subjectContext = resolveSubjectFromSubscription(event.externalSubscriptionId(), tenantKey);
    final String currency = billingSettings.getCurrency() != null ? billingSettings.getCurrency() : "USD";

    meterRegistry.counter("billing_payments_total",
        "status", "failed",
        "currency", currency,
        "reason", nullToEmpty(event.failureReason())
    ).increment();

    messagingService.publishPaymentFailed(
        tenantKey,
        event.externalInvoiceId(),
        event.externalCustomerId(),
        event.externalSubscriptionId(),
        event.amountDue(),
        billingSettings.getCurrency() != null ? billingSettings.getCurrency() : "USD",
        event.failureReason(),
        subjectContext.type().name(),
        subjectContext.key()
    );
    log.info("Published payment.failed for tenant={}, invoice={}", tenantKey, event.externalInvoiceId());

    final String email = resolveEmail(billingSettings);
    if (email != null) {
      publishNotification(new NotificationEvent(
          email,
          notificationProps.defaultLocale(),
          NotificationEventType.PAYMENT_FAILED,
          Map.of(
              "companyName", nullToEmpty(billingSettings.getCompanyName()),
              "invoiceId", nullToEmpty(event.externalInvoiceId()),
              "amountDue", event.amountDue() != null ? event.amountDue() / 100.0 : 0.0,
              "currency", billingSettings.getCurrency() != null ? billingSettings.getCurrency() : "USD"
          ),
          Instant.now()));
    }
  }

  // -------------------------------------------------------------------------
  // Notification helpers
  // -------------------------------------------------------------------------

  private void publishSubscriptionActivatedNotification(final Subscription subscription,
                                                        final SubscriptionSubject subject,
                                                        final UUID userId) {
    if (SubjectType.USER.equals(subject.type()) && userId != null) {
      userBillingSettingsMapper.findByUserId(userId).ifPresent(userSettings -> {
        final String email = userSettings.getBillingEmail();
        if (email != null && !email.isBlank()) {
          publishNotification(new NotificationEvent(
              email,
              notificationProps.defaultLocale(),
              NotificationEventType.SUBSCRIPTION_ACTIVATED,
              Map.of(
                  "companyName", nullToEmpty(userSettings.getCompanyName()),
                  "planId", nullToEmpty(subscription.getPlanId()),
                  "externalSubscriptionId", subscription.getExternalSubscriptionId()
              ),
              Instant.now()));
        }
      });
    } else {
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
                  "companyName", nullToEmpty(settings.getCompanyName()),
                  "planId", nullToEmpty(subscription.getPlanId()),
                  "externalSubscriptionId", subscription.getExternalSubscriptionId()
              ),
              Instant.now()));
        }
      });
    }
  }

  private void publishNotification(final NotificationEvent event) {
    try {
      messagingService.publishNotification(event);
    } catch (final Exception e) {
      log.warn("Failed to publish billing notification: type={} recipient={}",
          event.getType(), event.getRecipientEmail(), e);
    }
  }

  // -------------------------------------------------------------------------
  // Mapping helpers
  // -------------------------------------------------------------------------

  private Subscription toSubscription(final GatewaySubscriptionEvent event) {
    final var sub = new Subscription();
    sub.setId(UUID.randomUUID());
    sub.setExternalSubscriptionId(event.externalSubscriptionId());
    sub.setExternalCustomerId(event.externalCustomerId());
    sub.setStatus(event.status());
    sub.setPlanId(event.planId());
    sub.setQuantity(event.quantity());
    sub.setTrialStart(event.trialStart());
    sub.setTrialEnd(event.trialEnd());
    sub.setCurrentPeriodStart(event.currentPeriodStart());
    sub.setCurrentPeriodEnd(event.currentPeriodEnd());
    sub.setCancelAtPeriodEnd(event.cancelAtPeriodEnd());
    sub.setCanceledAt(event.canceledAt());
    sub.setTenantKey(event.metadata().get("tenantKey"));
    return sub;
  }

  /**
   * Resolves subject context from a cached subscription record.
   * Falls back to {@code TENANT / tenantKey} when no subscription is found.
   */
  private SubscriptionSubject resolveSubjectFromSubscription(final String externalSubscriptionId,
                                                             final String tenantKey) {
    if (externalSubscriptionId != null) {
      final var subscriptionOpt = subscriptionMapper.findByExternalSubscriptionId(externalSubscriptionId);
      if (subscriptionOpt.isPresent()) {
        final var subscription = subscriptionOpt.get();
        final String type = subscription.getSubjectType() != null
            ? subscription.getSubjectType() : SubjectType.TENANT.name();
        final String key = subscription.getSubjectKey() != null
            ? subscription.getSubjectKey() : tenantKey;
        return new SubscriptionSubject(SubjectType.valueOf(type), key);
      }
    }
    return new SubscriptionSubject(SubjectType.TENANT, tenantKey);
  }

  private UUID resolveUserId(final Map<String, String> metadata) {
    final String userIdStr = metadata.get("userId");
    if (userIdStr == null || userIdStr.isBlank()) {
      return null;
    }
    try {
      return UUID.fromString(userIdStr);
    } catch (final IllegalArgumentException e) {
      log.warn("Invalid userId in subscription metadata: {}", userIdStr);
      return null;
    }
  }

  private String resolveEmail(final BillingSettings settings) {
    if (settings.getBillingEmail() != null && !settings.getBillingEmail().isBlank()) {
      return settings.getBillingEmail();
    }
    log.warn("No billing email configured for tenantKey={}", settings.getTenantKey());
    return null;
  }

  private String nullToEmpty(final String value) {
    return value != null ? value : "";
  }

  /**
   * Resolves the human-readable {@code planCode} from the plan catalog for the given plan ID.
   * Tries {@code external_price_id} first (Stripe price ID), then falls back to exact
   * {@code plan_code} match. Returns the raw {@code planId} if no catalog entry is found.
   */
  private String resolvePlanCode(final String planId) {
    if (planId == null || planId.isBlank()) {
      return null;
    }
    return planMapper.findByExternalPriceId(planId)
        .or(() -> planMapper.findByPlanCode(planId))
        .map(plan -> plan.getPlanCode())
        .orElse(planId);
  }
}
