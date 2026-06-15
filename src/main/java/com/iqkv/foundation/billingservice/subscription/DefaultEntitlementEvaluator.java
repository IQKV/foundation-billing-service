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
import com.iqkv.foundation.billingservice.plan.Plan;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Default {@link EntitlementEvaluator} implementation.
 *
 * <p>Queries the local subscription cache by {@code (subject_type, subject_key)}.
 * If an active subscription is found, enriches it with plan feature data from the plan catalog.
 * If no plan catalog entry is found for the subscription's {@code planId}, the feature set is null.
 */
@Component
public class DefaultEntitlementEvaluator implements EntitlementEvaluator {

  private static final Logger log = LoggerFactory.getLogger(DefaultEntitlementEvaluator.class);

  private final SubscriptionMapper subscriptionMapper;
  private final PlanMapper planMapper;
  private final MeterRegistry meterRegistry;

  public DefaultEntitlementEvaluator(final SubscriptionMapper subscriptionMapper,
                                     final PlanMapper planMapper,
                                     final MeterRegistry meterRegistry) {
    this.subscriptionMapper = subscriptionMapper;
    this.planMapper = planMapper;
    this.meterRegistry = meterRegistry;
  }

  @Override
  public Optional<EntitlementDetails> evaluateEntitlements(final SubscriptionSubject subject) {
    final Optional<Subscription> activeSubscription =
        subscriptionMapper.findActiveBySubject(subject.type().name(), subject.key());

    if (activeSubscription.isEmpty()) {
      log.debug("No active subscription found for subject type={} key={}", subject.type(), subject.key());
      meterRegistry.counter("billing_entitlements_check_total", "result", "denied").increment();
      return Optional.empty();
    }

    final Subscription subscription = activeSubscription.get();
    final String featureSet = resolveFeatureSet(subscription.getPlanId());
    final String planCode = resolvePlanCode(subscription.getPlanId());

    meterRegistry.counter("billing_entitlements_check_total", "result", "allowed", "plan_code", nullToEmpty(planCode)).increment();

    return Optional.of(new EntitlementDetails(
        subject,
        subscription.getPlanId(),
        subscription.getStatus(),
        subscription.getCurrentPeriodEnd(),
        featureSet
    ));
  }

  private String nullToEmpty(String str) {
    return str == null ? "" : str;
  }

  /**
   * Resolves the human-readable {@code planCode} from the plan catalog for the given plan ID.
   * Uses the same lookup chain as {@link #resolveFeatureSet}: external price ID first, then plan code.
   * Returns the raw {@code planId} as fallback so callers always have a non-null value.
   */
  private String resolvePlanCode(final String planId) {
    if (planId == null || planId.isBlank()) {
      return "";
    }
    return planMapper.findByExternalPriceId(planId)
        .or(() -> planMapper.findByPlanCode(planId))
        .map(Plan::getPlanCode)
        .orElse(planId);
  }

  /**
   * Resolves the feature set JSON from the plan catalog for the given plan ID.
   *
   * <p>The {@code planId} stored on a subscription is the payment gateway's price/plan reference
   * (e.g. a Stripe price ID like {@code price_1Abc...}). The lookup therefore first tries to match
   * by {@code external_price_id}, then falls back to an exact {@code plan_code} match (which covers
   * direct assignments and non-Stripe gateways). Returns {@code null} if no match is found.
   */
  private String resolveFeatureSet(final String planId) {
    if (planId == null || planId.isBlank()) {
      return null;
    }
    return planMapper.findByExternalPriceId(planId)
        .or(() -> planMapper.findByPlanCode(planId))
        .map(plan -> {
          log.debug("Resolved feature set for planId={} planCode={}", planId, plan.getPlanCode());
          return plan.getFeatureSet();
        })
        .orElseGet(() -> {
          log.debug("Plan not found in catalog for planId={}, feature set will be null", planId);
          return null;
        });
  }
}
