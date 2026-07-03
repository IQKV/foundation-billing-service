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

import java.util.Optional;

import com.iqkv.foundation.billingservice.infrastructure.persistence.PlanMapper;
import com.iqkv.foundation.billingservice.infrastructure.persistence.SubscriptionMapper;
import com.iqkv.foundation.billingservice.plan.PlanEntitlement;
import com.iqkv.foundation.billingservice.plan.PlanFeatureRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Default {@link EntitlementEvaluator} implementation.
 *
 * <p>Queries the local subscription cache by {@code (subject_type, subject_key)}.
 * If an active subscription is found, resolves the human-readable {@code planCode}
 * from the plan catalog and looks up the typed {@link PlanEntitlement} from the
 * in-memory {@link PlanFeatureRegistry}.
 *
 * <p>The {@code planId} on a subscription holds the payment gateway's price reference
 * (e.g. a Stripe price ID like {@code price_1Abc...}). Plan lookup therefore tries
 * {@code external_price_id} first, then falls back to an exact {@code plan_code} match.
 */
@Component
public class DefaultEntitlementEvaluator implements EntitlementEvaluator {

  private static final Logger log = LoggerFactory.getLogger(DefaultEntitlementEvaluator.class);

  private final SubscriptionMapper subscriptionMapper;
  private final PlanMapper planMapper;
  private final PlanFeatureRegistry planFeatureRegistry;
  private final MeterRegistry meterRegistry;

  public DefaultEntitlementEvaluator(final SubscriptionMapper subscriptionMapper,
                                     final PlanMapper planMapper,
                                     final PlanFeatureRegistry planFeatureRegistry,
                                     final MeterRegistry meterRegistry) {
    this.subscriptionMapper = subscriptionMapper;
    this.planMapper = planMapper;
    this.planFeatureRegistry = planFeatureRegistry;
    this.meterRegistry = meterRegistry;
  }

  @Override
  public Optional<EntitlementDetails> evaluateEntitlements(final SubscriptionSubject subject) {
    final Optional<Subscription> activeSubscription =
        subscriptionMapper.findActiveBySubject(subject.type().name(), subject.key());

    if (activeSubscription.isEmpty()) {
      log.debug("No active subscription found for subject type={} key={} - returning free plan", subject.type(), subject.key());
      meterRegistry.counter("billing_entitlements_check_total", "result", "free").increment();
      // Return default free plan when no subscription
      return Optional.of(new EntitlementDetails(
          subject,
          "free",
          "active", // free plan is always active
          null, // free plan has no period end
          PlanEntitlement.NONE
      ));
    }

    final Subscription subscription = activeSubscription.get();
    final String planCode = resolvePlanCode(subscription.getPlanId());
    final PlanEntitlement planEntitlement = planFeatureRegistry.resolveEntitlement(planCode);

    meterRegistry.counter("billing_entitlements_check_total",
        "result", "allowed",
        "plan_code", planCode != null ? planCode : "").increment();

    return Optional.of(new EntitlementDetails(
        subject,
        planCode,
        subscription.getStatus(),
        subscription.getCurrentPeriodEnd(),
        planEntitlement
    ));
  }

  /**
   * Resolves the human-readable {@code planCode} from the plan catalog for the given plan ID.
   *
   * <p>Tries {@code external_price_id} first (Stripe price ID), then falls back to an exact
   * {@code plan_code} match for direct assignments and non-Stripe gateways.
   * Returns the raw {@code planId} as a last-resort fallback so callers always have a value.
   */
  private String resolvePlanCode(final String planId) {
    if (planId == null || planId.isBlank()) {
      return null;
    }
    return planMapper.findByExternalPriceId(planId)
        .or(() -> planMapper.findByPlanCode(planId))
        .map(plan -> {
          log.debug("Resolved planCode={} for planId={}", plan.getPlanCode(), planId);
          return plan.getPlanCode();
        })
        .orElseGet(() -> {
          log.debug("Plan not found in catalog for planId={}", planId);
          return planId;
        });
  }
}
