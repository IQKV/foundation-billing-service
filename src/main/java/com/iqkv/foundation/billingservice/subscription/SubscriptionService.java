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

package com.iqkv.foundation.billingservice.subscription;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.iqkv.foundation.billingservice.gateway.command.CreateCheckoutSessionCommand;
import com.iqkv.foundation.billingservice.gateway.command.CreateRefundCommand;
import com.iqkv.foundation.billingservice.gateway.command.UpdateSubscriptionCommand;
import com.iqkv.foundation.billingservice.gateway.port.PaymentGatewayPort;
import com.iqkv.foundation.billingservice.infrastructure.messaging.MessagingService;
import com.iqkv.foundation.billingservice.infrastructure.persistence.BillingSettingsMapper;
import com.iqkv.foundation.billingservice.infrastructure.persistence.PlanMapper;
import com.iqkv.foundation.billingservice.infrastructure.persistence.RefundMapper;
import com.iqkv.foundation.billingservice.infrastructure.persistence.SubscriptionMapper;
import com.iqkv.foundation.billingservice.plan.Plan;
import com.iqkv.foundation.billingservice.plan.PlanFeatureRegistry;
import com.iqkv.foundation.billingservice.plan.PricingModel;
import com.iqkv.foundation.billingservice.shared.exception.ResourceNotFoundException;
import com.iqkv.foundation.billingservice.shared.exception.SeatLimitExceededException;
import com.iqkv.foundation.billingservice.shared.exception.TenantContextMismatchException;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Application service for subscription queries.
 *
 * <p>Subscription state is a local cache populated by {@code PaymentWebhookRestResource}
 * on Stripe webhook events — no payment gateway round-trips are made here.
 *
 * <p>Subject-scoped queries use {@link SubscriptionSubjectResolver} to determine whether
 * subscriptions are keyed by tenant (multi-tenant mode) or by user (single-tenant mode).
 */
@Service
public class SubscriptionService {

  private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);

  private final SubscriptionMapper subscriptionMapper;
  private final RefundMapper refundMapper;
  private final SubscriptionSubjectResolver subjectResolver;
  private final PaymentGatewayPort paymentGatewayPort;
  private final BillingSettingsMapper billingSettingsMapper;
  private final com.iqkv.foundation.billingservice.infrastructure.persistence.UserBillingSettingsMapper userBillingSettingsMapper;
  private final PlanMapper planMapper;
  private final PlanFeatureRegistry planFeatureRegistry;
  private final MeterRegistry meterRegistry;
  private final MessagingService messagingService;

  public SubscriptionService(final SubscriptionMapper subscriptionMapper,
                             final RefundMapper refundMapper,
                             final SubscriptionSubjectResolver subjectResolver,
                             final PaymentGatewayPort paymentGatewayPort,
                             final BillingSettingsMapper billingSettingsMapper,
                             final com.iqkv.foundation.billingservice.infrastructure.persistence.UserBillingSettingsMapper userBillingSettingsMapper,
                             final PlanMapper planMapper,
                             final PlanFeatureRegistry planFeatureRegistry,
                             final MeterRegistry meterRegistry,
                             final MessagingService messagingService) {
    this.subscriptionMapper = subscriptionMapper;
    this.refundMapper = refundMapper;
    this.subjectResolver = subjectResolver;
    this.paymentGatewayPort = paymentGatewayPort;
    this.billingSettingsMapper = billingSettingsMapper;
    this.userBillingSettingsMapper = userBillingSettingsMapper;
    this.planMapper = planMapper;
    this.planFeatureRegistry = planFeatureRegistry;
    this.meterRegistry = meterRegistry;
    this.messagingService = messagingService;
  }

  // ─── Self-service ──────────────────────────────────────────────────────────

  // ─── Seat routing helpers ──────────────────────────────────────────────────

  /**
   * Returns the effective Stripe line-item quantity for the given plan and caller-supplied value.
   * <ul>
   *   <li>For {@link PricingModel#FLAT} plans: always {@code 1}, caller input ignored.</li>
   *   <li>For {@link PricingModel#PER_SEAT} plans: {@code requested} if ≥ 1, otherwise {@code 1}.</li>
   * </ul>
   */
  private long resolveEffectiveQuantity(final Plan plan, final Long requested) {
    if (PricingModel.PER_SEAT.name().equals(plan.getPricingModel())) {
      return requested != null && requested > 0 ? requested : 1L;
    }
    return 1L;
  }

  /**
   * Validates that the requested seat count does not exceed the plan ceiling.
   * Only called for {@link PricingModel#PER_SEAT} plans.
   *
   * @throws SeatLimitExceededException if {@code requestedSeats} exceeds {@code maxUsers} (when > 0)
   */
  private void validateSeatCount(final Plan plan, final long requestedSeats) {
    if (requestedSeats < 1) {
      throw new IllegalArgumentException("Seat count must be at least 1");
    }
    final int maxUsers = planFeatureRegistry.resolveEntitlement(plan.getPlanCode()).maxUsers();
    if (maxUsers > 0 && requestedSeats > maxUsers) {
      throw new SeatLimitExceededException(plan.getPlanCode(), requestedSeats, maxUsers);
    }
  }

  // ─── Self-service ──────────────────────────────────────────────────────────

  /**
   * Returns the most recent active subscription for the given tenant.
   *
   * @throws ResourceNotFoundException if no active subscription exists
   */
  public Subscription getActiveByTenantKey(final String tenantKey) {
    return subscriptionMapper.findActiveByTenantKey(tenantKey)
        .orElseThrow(() -> new ResourceNotFoundException(
            "No active subscription found for tenantKey=" + tenantKey));
  }

  /**
   * Returns all subscription records for the given tenant, ordered by {@code created_at DESC}.
   */
  public List<Subscription> getAllByTenantKey(final String tenantKey) {
    return subscriptionMapper.findAllByTenantKey(tenantKey);
  }

  /**
   * Returns the active subscription for the resolved subject (tenant or user depending on mode).
   *
   * @param tenantKey the tenant key from request context
   * @param userId    the user ID from JWT claims
   * @throws ResourceNotFoundException if no active subscription exists for the subject
   */
  public Subscription getActiveBySubject(final String tenantKey, final UUID userId) {
    final SubscriptionSubject subject = subjectResolver.resolveSubject(tenantKey, userId);
    return subscriptionMapper.findActiveBySubject(subject.type().name(), subject.key())
        .orElseThrow(() -> new ResourceNotFoundException(
            "No active subscription found for subject type=" + subject.type() + " key=" + subject.key()));
  }

  /**
   * Returns all subscriptions for the resolved subject (tenant or user depending on mode),
   * ordered by {@code created_at DESC}.
   *
   * @param tenantKey the tenant key from request context
   * @param userId    the user ID from JWT claims
   */
  public List<Subscription> getAllBySubject(final String tenantKey, final UUID userId) {
    final SubscriptionSubject subject = subjectResolver.resolveSubject(tenantKey, userId);
    return subscriptionMapper.findBySubject(subject.type().name(), subject.key());
  }

  /**
   * Creates a checkout session for the given tenant.
   */
  public SubscriptionDtos.CheckoutSessionResponse createCheckoutSession(
      final String tenantKey,
      final SubscriptionDtos.CreateCheckoutSessionRequest request) {
    final var settings = billingSettingsMapper.findByTenantKey(tenantKey)
        .orElseThrow(() -> {
          log.warn("Create checkout session failed: billing settings not found for tenantKey={}", tenantKey);
          return new ResourceNotFoundException("Billing settings not found for tenant: " + tenantKey);
        });

    if (settings.getExternalCustomerId() == null) {
      log.warn("Create checkout session failed: no external customer ID for tenantKey={}", tenantKey);
      throw new IllegalStateException("External customer ID not found for tenant: " + tenantKey);
    }

    final Plan plan = planMapper.findByPlanCode(request.planCode())
        .orElseThrow(() -> {
          log.warn("Create checkout session failed: plan not found for planCode={}, tenantKey={}", request.planCode(), tenantKey);
          return new ResourceNotFoundException("Plan not found for planCode: " + request.planCode());
        });

    if (plan.getExternalPriceId() == null) {
      log.warn("Create checkout session failed: plan {} has no externalPriceId, tenantKey={}", request.planCode(), tenantKey);
      throw new IllegalStateException("Plan " + request.planCode() + " has no externalPriceId (not synced with payment gateway yet)");
    }

    final Integer trialDays = request.trialPeriodDays() != null ? request.trialPeriodDays() : plan.getTrialPeriodDays();
    final long effectiveQuantity = resolveEffectiveQuantity(plan, request.quantity());
    if (PricingModel.PER_SEAT.name().equals(plan.getPricingModel())) {
      validateSeatCount(plan, effectiveQuantity);
    }
    final var command = new CreateCheckoutSessionCommand(
        settings.getExternalCustomerId(),
        plan.getExternalPriceId(),
        request.successUrl(),
        request.cancelUrl(),
        trialDays,
        effectiveQuantity,
        request.allowPromotionCodes(),
        java.util.Map.of("tenantKey", tenantKey)
    );

    final String url = paymentGatewayPort.createCheckoutSession(command);
    log.info("Created checkout session: tenantKey={}, planCode={}", tenantKey, request.planCode());
    meterRegistry.counter("billing_checkout_sessions_total", "plan_id", request.planCode()).increment();
    return new SubscriptionDtos.CheckoutSessionResponse(url);
  }

  /**
   * Creates a checkout session for the current subject (tenant or user depending on mode).
   */
  public SubscriptionDtos.CheckoutSessionResponse createCheckoutSessionForSubject(
      final String tenantKey,
      final UUID userId,
      final SubscriptionDtos.CreateCheckoutSessionRequest request) {
    final SubscriptionSubject subject = subjectResolver.resolveSubject(tenantKey, userId);

    String externalCustomerId;
    final Map<String, String> metadata = new java.util.HashMap<>();

    if (subject.type() == SubjectType.TENANT) {
      final var settings = billingSettingsMapper.findByTenantKey(subject.key())
          .orElseThrow(() -> {
            log.warn("Create checkout session for subject failed: billing settings not found, subjectKey={}", subject.key());
            return new ResourceNotFoundException("Billing settings not found for tenant: " + subject.key());
          });
      if (settings.getExternalCustomerId() == null) {
        log.warn("Create checkout session for subject failed: no external customer ID, subjectKey={}", subject.key());
        throw new IllegalStateException("External customer ID not found for tenant: " + subject.key());
      }
      externalCustomerId = settings.getExternalCustomerId();
      metadata.put("tenantKey", subject.key());
    } else {
      final var settings = userBillingSettingsMapper.findByUserId(userId)
          .orElseThrow(() -> {
            log.warn("Create checkout session for subject failed: user billing settings not found, userId={}", userId);
            return new ResourceNotFoundException("User billing settings not found for user: " + userId);
          });
      if (settings.getExternalCustomerId() == null) {
        log.warn("Create checkout session for subject failed: no external customer ID, userId={}", userId);
        throw new IllegalStateException("External customer ID not found for user: " + userId);
      }
      externalCustomerId = settings.getExternalCustomerId();
      metadata.put("userId", userId.toString());
    }

    final Plan plan = planMapper.findByPlanCode(request.planCode())
        .orElseThrow(() -> {
          log.warn("Create checkout session for subject failed: plan not found, planCode={}", request.planCode());
          return new ResourceNotFoundException("Plan not found for planCode: " + request.planCode());
        });

    if (plan.getExternalPriceId() == null) {
      log.warn("Create checkout session for subject failed: plan {} has no externalPriceId", request.planCode());
      throw new IllegalStateException("Plan " + request.planCode() + " has no externalPriceId (not synced with payment gateway yet)");
    }

    final Integer trialDays = request.trialPeriodDays() != null ? request.trialPeriodDays() : plan.getTrialPeriodDays();
    final long effectiveQuantity = resolveEffectiveQuantity(plan, request.quantity());
    if (PricingModel.PER_SEAT.name().equals(plan.getPricingModel())) {
      validateSeatCount(plan, effectiveQuantity);
    }
    final var command = new CreateCheckoutSessionCommand(
        externalCustomerId,
        plan.getExternalPriceId(),
        request.successUrl(),
        request.cancelUrl(),
        trialDays,
        effectiveQuantity,
        request.allowPromotionCodes(),
        metadata
    );

    final String url = paymentGatewayPort.createCheckoutSession(command);
    log.info("Created checkout session for subject: subjectType={}, subjectKey={}, planCode={}",
        subject.type(), subject.key(), request.planCode());
    meterRegistry.counter("billing_checkout_sessions_total", "plan_id", request.planCode()).increment();
    return new SubscriptionDtos.CheckoutSessionResponse(url);
  }

  /**
   * Updates an existing subscription.
   */
  public void updateSubscription(
      final String tenantKey,
      final String externalSubscriptionId,
      final SubscriptionDtos.UpdateSubscriptionRequest request) {
    final Subscription subscription = subscriptionMapper.findByExternalSubscriptionId(externalSubscriptionId)
        .orElseThrow(() -> {
          log.warn("Update subscription failed: subscription not found, externalSubscriptionId={}", externalSubscriptionId);
          return new ResourceNotFoundException("Subscription not found: " + externalSubscriptionId);
        });

    if (!tenantKey.equals(subscription.getTenantKey())) {
      log.warn("Update subscription failed: tenant mismatch, externalSubscriptionId={}, tenantKey={}",
          externalSubscriptionId, tenantKey);
      throw new TenantContextMismatchException("Subscription " + externalSubscriptionId + " does not belong to tenant " + tenantKey);
    }

    String priceId = null;
    if (request.planCode() != null) {
      final Plan plan = planMapper.findByPlanCode(request.planCode())
          .orElseThrow(() -> {
            log.warn("Update subscription failed: plan not found, planCode={}", request.planCode());
            return new ResourceNotFoundException("Plan not found for planCode: " + request.planCode());
          });
      if (plan.getExternalPriceId() == null) {
        log.warn("Update subscription failed: plan {} has no externalPriceId", request.planCode());
        throw new IllegalStateException("Plan " + request.planCode() + " has no externalPriceId (not synced with payment gateway yet)");
      }
      priceId = plan.getExternalPriceId();
    }

    final var command = new UpdateSubscriptionCommand(
        externalSubscriptionId,
        priceId,
        request.quantity(),
        request.prorationBehavior(),
        java.util.Map.of()
    );

    paymentGatewayPort.updateSubscription(command);
    log.info("Updated subscription: externalSubscriptionId={}, tenantKey={}", externalSubscriptionId, tenantKey);
    meterRegistry.counter("billing_subscription_updates_total", "plan_id", request.planCode()).increment();
  }

  /**
   * Adjusts the seat count on a {@link PricingModel#PER_SEAT} subscription.
   *
   * <p>Validates that {@code request.seatCount()} is within the plan ceiling before
   * delegating to the payment gateway. The gateway webhook will write the updated
   * {@code quantity} back to the local {@code subscriptions} cache automatically.
   *
   * @throws IllegalStateException          if the subscription's plan is not {@code PER_SEAT}
   * @throws SeatLimitExceededException     if the requested seat count exceeds {@code maxUsers}
   * @throws TenantContextMismatchException if {@code tenantKey} does not own the subscription
   */
  public void adjustSeats(
      final String tenantKey,
      final String externalSubscriptionId,
      final SubscriptionDtos.AdjustSeatsRequest request) {

    final Subscription subscription = subscriptionMapper.findByExternalSubscriptionId(externalSubscriptionId)
        .orElseThrow(() -> {
          log.warn("Adjust seats failed: subscription not found, externalSubscriptionId={}", externalSubscriptionId);
          return new ResourceNotFoundException("Subscription not found: " + externalSubscriptionId);
        });

    if (!tenantKey.equals(subscription.getTenantKey())) {
      log.warn("Adjust seats failed: tenant mismatch, externalSubscriptionId={}, tenantKey={}",
          externalSubscriptionId, tenantKey);
      throw new TenantContextMismatchException(
          "Subscription " + externalSubscriptionId + " does not belong to tenant " + tenantKey);
    }

    final Plan plan = planMapper.findByExternalPriceId(subscription.getPlanId())
        .orElseThrow(() -> {
          log.warn("Adjust seats failed: plan not found for priceId={}", subscription.getPlanId());
          return new ResourceNotFoundException("Plan not found for priceId: " + subscription.getPlanId());
        });

    if (!PricingModel.PER_SEAT.name().equals(plan.getPricingModel())) {
      throw new IllegalStateException(
          "Plan " + plan.getPlanCode() + " uses " + plan.getPricingModel()
          + " pricing — seat adjustment is only supported for PER_SEAT plans");
    }

    validateSeatCount(plan, request.seatCount());

    final String prorationBehavior = request.prorationBehavior() != null
        ? request.prorationBehavior()
        : "create_prorations";

    paymentGatewayPort.updateSubscription(new UpdateSubscriptionCommand(
        externalSubscriptionId,
        null,           // no plan change
        request.seatCount(),
        prorationBehavior,
        java.util.Map.of("tenantKey", tenantKey)
    ));

    log.info("Seat adjustment initiated: tenantKey={}, subscription={}, seats={}",
        tenantKey, externalSubscriptionId, request.seatCount());
    meterRegistry.counter("billing_seat_adjustments_total",
        "plan_code", plan.getPlanCode()).increment();
  }

  /**
   * Creates a refund for a payment.
   */
  public SubscriptionDtos.RefundResponse createRefund(
      final String tenantKey,
      final SubscriptionDtos.CreateRefundRequest request) {
    final var settings = billingSettingsMapper.findByTenantKey(tenantKey)
        .orElseThrow(() -> {
          log.warn("Create refund failed: billing settings not found for tenantKey={}", tenantKey);
          return new ResourceNotFoundException("Billing settings not found for tenant: " + tenantKey);
        });

    if (settings.getExternalCustomerId() == null) {
      log.warn("Create refund failed: no external customer ID for tenantKey={}", tenantKey);
      throw new IllegalStateException("External customer ID not found for tenant: " + tenantKey);
    }

    final var command = new CreateRefundCommand(
        request.paymentId(),
        settings.getExternalCustomerId(),
        request.amount(),
        request.reason(),
        java.util.Map.of("tenantKey", tenantKey)
    );

    final String refundId = paymentGatewayPort.createRefund(command);
    log.info("Created refund: tenantKey={}, paymentId={}", tenantKey, request.paymentId());
    meterRegistry.counter("billing_refunds_issued_total", "reason", request.reason() != null ? request.reason() : "unknown").increment();
    return new SubscriptionDtos.RefundResponse(refundId);
  }

  /**
   * Creates a refund for a payment for the current subject (tenant or user depending on mode).
   */
  public SubscriptionDtos.RefundResponse createRefundForSubject(
      final String tenantKey,
      final UUID userId,
      final SubscriptionDtos.CreateRefundRequest request) {
    final var subject = subjectResolver.resolveSubject(tenantKey, userId);

    String externalCustomerId;
    final java.util.Map<String, String> metadata = new java.util.HashMap<>();

    if (subject.type() == SubjectType.TENANT) {
      final var settings = billingSettingsMapper.findByTenantKey(subject.key())
          .orElseThrow(() -> {
            log.warn("Create refund for subject failed: billing settings not found, subjectKey={}", subject.key());
            return new ResourceNotFoundException("Billing settings not found for tenant: " + subject.key());
          });
      if (settings.getExternalCustomerId() == null) {
        log.warn("Create refund for subject failed: no external customer ID, subjectKey={}", subject.key());
        throw new IllegalStateException("External customer ID not found for tenant: " + subject.key());
      }
      externalCustomerId = settings.getExternalCustomerId();
      metadata.put("tenantKey", subject.key());
    } else {
      final var settings = userBillingSettingsMapper.findByUserId(userId)
          .orElseThrow(() -> {
            log.warn("Create refund for subject failed: user billing settings not found, userId={}", userId);
            return new ResourceNotFoundException("User billing settings not found for user: " + userId);
          });
      if (settings.getExternalCustomerId() == null) {
        log.warn("Create refund for subject failed: no external customer ID, userId={}", userId);
        throw new IllegalStateException("External customer ID not found for user: " + userId);
      }
      externalCustomerId = settings.getExternalCustomerId();
      metadata.put("userId", userId.toString());
    }

    final var command = new CreateRefundCommand(
        request.paymentId(),
        externalCustomerId,
        request.amount(),
        request.reason(),
        metadata
    );

    final String refundId = paymentGatewayPort.createRefund(command);
    log.info("Created refund for subject: subjectType={}, subjectKey={}, paymentId={}",
        subject.type(), subject.key(), request.paymentId());
    meterRegistry.counter("billing_refunds_issued_total", "reason", request.reason() != null ? request.reason() : "unknown").increment();
    return new SubscriptionDtos.RefundResponse(refundId);
  }

  /**
   * Returns all refund records for the given subject (type + key),
   * ordered by {@code occurred_at DESC}.
   */
  public java.util.List<Refund> getAllRefundsBySubject(final String tenantKey, final UUID userId) {
    final var subject = subjectResolver.resolveSubject(tenantKey, userId);
    return refundMapper.findAllBySubject(subject.type().name(), subject.key());
  }

  // ─── Platform admin ────────────────────────────────────────────────────────

  /**
   * Returns the total number of subscription records, optionally filtered by status.
   *
   * @param status optional status filter (e.g. "active", "past_due")
   * @return subscription count
   */
  @org.springframework.transaction.annotation.Transactional(readOnly = true)
  public SubscriptionDtos.SubscriptionCountResponse countSubscriptions(final String status) {
    final String safeStatus = (status != null && !status.isBlank()) ? status.strip() : null;
    return new SubscriptionDtos.SubscriptionCountResponse(subscriptionMapper.countAll(null, safeStatus, null));
  }

  /**
   * Returns a paginated, sorted, and optionally filtered list of subscriptions.
   *
   * @param query encapsulates pagination, sort, and optional filters
   * @return a {@link SubscriptionDtos.PagedSubscriptionResponse}
   */
  @org.springframework.transaction.annotation.Transactional(readOnly = true)
  public SubscriptionDtos.PagedSubscriptionResponse listSubscriptions(
      final SubscriptionDtos.SubscriptionListQuery query) {
    final String safeSortBy = java.util.Set.of("tenantKey", "planId", "status", "updatedAt", "createdAt")
        .contains(query.sortBy()) ? query.sortBy() : "createdAt";
    final String safeSortDir = "asc".equalsIgnoreCase(query.sortDir()) ? "asc" : "desc";

    final String safeSearch = (query.search() != null && !query.search().isBlank())
        ? query.search().strip() : null;
    final String safeStatus = (query.status() != null && !query.status().isBlank())
        ? query.status().strip() : null;
    final String safeTenantKey = (query.tenantKey() != null && !query.tenantKey().isBlank())
        ? query.tenantKey().strip() : null;

    final int offset = query.page() * query.size();
    final var subscriptions = subscriptionMapper.findAll(
            query.size(), offset, safeSortBy, safeSortDir, safeSearch, safeStatus, safeTenantKey)
        .stream()
        .map(SubscriptionDtoMapper::toAdminResponse)
        .toList();
    final long total = subscriptionMapper.countAll(safeSearch, safeStatus, safeTenantKey);
    final int totalPages = (int) Math.ceil((double) total / query.size());
    return new SubscriptionDtos.PagedSubscriptionResponse(subscriptions, query.page(), query.size(), total, totalPages);
  }

  /**
   * Retrieves a subscription by its UUID.
   *
   * @param id the subscription's unique identifier
   * @return the subscription as a {@link SubscriptionDtos.AdminSubscriptionResponse}
   * @throws ResourceNotFoundException if no subscription exists with the given ID
   */
  @org.springframework.transaction.annotation.Transactional(readOnly = true)
  public SubscriptionDtos.AdminSubscriptionResponse getSubscriptionById(final UUID id) {
    final Subscription subscription = subscriptionMapper.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Subscription not found: " + id));
    return SubscriptionDtoMapper.toAdminResponse(subscription);
  }

  /**
   * Partially updates a subscription (PATCH semantics).
   * Only non-null fields in the request are applied.
   *
   * @param id      the subscription's unique identifier
   * @param request partial update payload; any field may be {@code null}
   * @return the updated subscription as a {@link SubscriptionDtos.AdminSubscriptionResponse}
   * @throws ResourceNotFoundException if no subscription exists with the given ID
   */
  @org.springframework.transaction.annotation.Transactional
  public SubscriptionDtos.AdminSubscriptionResponse patchSubscription(
      final UUID id,
      final SubscriptionDtos.AdminUpdateSubscriptionRequest request) {
    final Subscription subscription = subscriptionMapper.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Subscription not found: " + id));

    boolean gatewayUpdateNeeded = false;
    String newPriceId = null;
    Long newQuantity = null;

    if (request.status() != null) {
      subscription.setStatus(request.status());
      if ("canceled".equalsIgnoreCase(request.status()) && subscription.getCanceledAt() == null) {
        subscription.setCanceledAt(java.time.Instant.now());
      }
    }

    if (request.currentPeriodEnd() != null) {
      subscription.setCurrentPeriodEnd(request.currentPeriodEnd());
    }

    if (request.quantity() != null) {
      subscription.setQuantity(request.quantity());
      newQuantity = request.quantity();
      gatewayUpdateNeeded = true;
    }

    if (request.planId() != null) {
      subscription.setPlanId(request.planId());
      newPriceId = request.planId();
      gatewayUpdateNeeded = true;
    }

    if (request.trialStart() != null) {
      subscription.setTrialStart(request.trialStart());
    }

    if (request.trialEnd() != null) {
      subscription.setTrialEnd(request.trialEnd());
    }

    if (request.cancelAtPeriodEnd() != null) {
      subscription.setCancelAtPeriodEnd(request.cancelAtPeriodEnd());
      // We handle cancelAtPeriodEnd via a dedicated gateway call or as part of update
      paymentGatewayPort.cancelSubscription(subscription.getExternalSubscriptionId(), request.cancelAtPeriodEnd());
    }

    if (gatewayUpdateNeeded) {
      final var command = new UpdateSubscriptionCommand(
          subscription.getExternalSubscriptionId(),
          newPriceId,
          newQuantity,
          null,
          java.util.Map.of()
      );
      paymentGatewayPort.updateSubscription(command);
    }

    subscription.setUpdatedAt(java.time.LocalDateTime.now());
    subscriptionMapper.update(subscription);

    // Publish subscription updated event
    final String tenantKey = subscription.getTenantKey();
    if (tenantKey != null && !tenantKey.isBlank()) {
      messagingService.publishSubscriptionUpdated(
          tenantKey,
          subscription.getExternalSubscriptionId(),
          subscription.getSubjectType(),
          subscription.getSubjectKey(),
          resolvePlanCode(subscription.getPlanId()),
          subscription.getQuantity()
      );
      log.info("Published subscription.updated for tenant={}, subscription={}", tenantKey, subscription.getExternalSubscriptionId());
    }

    return SubscriptionDtoMapper.toAdminResponse(subscription);
  }

  /**
   * Cancels an existing subscription.
   *
   * @param id                the subscription's unique identifier
   * @param cancelAtPeriodEnd whether to cancel at the end of the period or immediately
   * @return the updated subscription
   */
  @org.springframework.transaction.annotation.Transactional
  public SubscriptionDtos.AdminSubscriptionResponse cancelSubscription(final UUID id, final boolean cancelAtPeriodEnd) {
    final Subscription subscription = subscriptionMapper.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Subscription not found: " + id));

    paymentGatewayPort.cancelSubscription(subscription.getExternalSubscriptionId(), cancelAtPeriodEnd);

    if (!cancelAtPeriodEnd) {
      subscription.setStatus("canceled");
      subscription.setCanceledAt(java.time.Instant.now());
    } else {
      subscription.setCancelAtPeriodEnd(true);
    }

    subscription.setUpdatedAt(java.time.LocalDateTime.now());
    subscriptionMapper.update(subscription);

    // Publish subscription cancelled event
    final String tenantKey = subscription.getTenantKey();
    if (tenantKey != null && !tenantKey.isBlank()) {
      messagingService.publishSubscriptionCancelled(
          tenantKey,
          subscription.getExternalSubscriptionId(),
          subscription.getSubjectType(),
          subscription.getSubjectKey()
      );
      log.info("Published subscription.cancelled for tenant={}, subscription={}", tenantKey, subscription.getExternalSubscriptionId());
    }

    return SubscriptionDtoMapper.toAdminResponse(subscription);
  }

  /**
   * Pauses an existing subscription.
   *
   * @param id the subscription's unique identifier
   * @return the updated subscription
   */
  @org.springframework.transaction.annotation.Transactional
  public SubscriptionDtos.AdminSubscriptionResponse pauseSubscription(final UUID id) {
    final Subscription subscription = subscriptionMapper.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Subscription not found: " + id));

    paymentGatewayPort.pauseSubscription(subscription.getExternalSubscriptionId());

    subscription.setStatus("paused");
    subscription.setUpdatedAt(java.time.LocalDateTime.now());
    subscriptionMapper.update(subscription);

    // Publish subscription updated event
    final String tenantKey = subscription.getTenantKey();
    if (tenantKey != null && !tenantKey.isBlank()) {
      messagingService.publishSubscriptionUpdated(
          tenantKey,
          subscription.getExternalSubscriptionId(),
          subscription.getSubjectType(),
          subscription.getSubjectKey(),
          resolvePlanCode(subscription.getPlanId()),
          subscription.getQuantity()
      );
      log.info("Published subscription.updated for tenant={}, subscription={}", tenantKey, subscription.getExternalSubscriptionId());
    }

    return SubscriptionDtoMapper.toAdminResponse(subscription);
  }

  /**
   * Reactivates a paused subscription.
   *
   * @param id the subscription's unique identifier
   * @return the updated subscription
   */
  @org.springframework.transaction.annotation.Transactional
  public SubscriptionDtos.AdminSubscriptionResponse reactivateSubscription(final UUID id) {
    final Subscription subscription = subscriptionMapper.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Subscription not found: " + id));

    paymentGatewayPort.reactivateSubscription(subscription.getExternalSubscriptionId());

    subscription.setStatus("active");
    subscription.setUpdatedAt(java.time.LocalDateTime.now());
    subscriptionMapper.update(subscription);

    // Publish subscription updated event
    final String tenantKey = subscription.getTenantKey();
    if (tenantKey != null && !tenantKey.isBlank()) {
      messagingService.publishSubscriptionUpdated(
          tenantKey,
          subscription.getExternalSubscriptionId(),
          subscription.getSubjectType(),
          subscription.getSubjectKey(),
          resolvePlanCode(subscription.getPlanId()),
          subscription.getQuantity()
      );
      log.info("Published subscription.updated for tenant={}, subscription={}", tenantKey, subscription.getExternalSubscriptionId());
    }

    return SubscriptionDtoMapper.toAdminResponse(subscription);
  }

  /**
   * Permanently deletes a subscription.
   *
   * @param id the subscription's unique identifier
   * @throws ResourceNotFoundException if no subscription exists with the given ID
   */
  @org.springframework.transaction.annotation.Transactional
  public void deleteSubscription(final UUID id) {
    subscriptionMapper.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Subscription not found: " + id));
    subscriptionMapper.deleteById(id);
  }

  /**
   * Returns a paginated, sorted, and optionally filtered list of refunds.
   */
  @org.springframework.transaction.annotation.Transactional(readOnly = true)
  public SubscriptionDtos.PagedRefundResponse listRefunds(final SubscriptionDtos.RefundListQuery query) {
    final int limit = query.size();
    final int offset = query.page() * limit;

    final var refunds = refundMapper.findAll(
        limit, offset, query.sortBy(), query.sortDir(), query.tenantKey());
    final long total = refundMapper.countAll(query.tenantKey());

    final var content = refunds.stream()
        .map(RefundDtoMapper::toAdminResponse)
        .toList();

    return new SubscriptionDtos.PagedRefundResponse(
        content,
        query.page(),
        query.size(),
        total,
        (int) Math.ceil((double) total / limit)
    );
  }

  /**
   * Resolves the human-readable planCode from the plan catalog for the given plan ID.
   */
  private String resolvePlanCode(final String planId) {
    if (planId == null || planId.isBlank()) {
      return null;
    }
    return planMapper.findByExternalPriceId(planId)
        .or(() -> planMapper.findByPlanCode(planId))
        .map(Plan::getPlanCode)
        .orElse(planId);
  }

  /**
   * Returns a refund by ID.
   */
  @org.springframework.transaction.annotation.Transactional(readOnly = true)
  public SubscriptionDtos.AdminRefundResponse getRefundById(final UUID id) {
    return refundMapper.findById(id)
        .map(RefundDtoMapper::toAdminResponse)
        .orElseThrow(() -> new ResourceNotFoundException("Refund not found: " + id));
  }

  /**
   * Returns all refunds for the given tenant.
   */
  @org.springframework.transaction.annotation.Transactional(readOnly = true)
  public List<Refund> getAllRefundsByTenantKey(final String tenantKey) {
    return refundMapper.findAllByTenantKey(tenantKey);
  }
}
