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

  public DefaultEntitlementEvaluator(final SubscriptionMapper subscriptionMapper,
                                     final PlanMapper planMapper) {
    this.subscriptionMapper = subscriptionMapper;
    this.planMapper = planMapper;
  }

  @Override
  public Optional<EntitlementDetails> evaluateEntitlements(final SubscriptionSubject subject) {
    final Optional<Subscription> activeSubscription =
        subscriptionMapper.findActiveBySubject(subject.type().name(), subject.key());

    if (activeSubscription.isEmpty()) {
      log.debug("No active subscription found for subject type={} key={}", subject.type(), subject.key());
      return Optional.empty();
    }

    final Subscription subscription = activeSubscription.get();
    final String featureSet = resolveFeatureSet(subscription.getPlanId());

    return Optional.of(new EntitlementDetails(
        subject,
        subscription.getPlanId(),
        subscription.getStatus(),
        subscription.getCurrentPeriodEnd(),
        featureSet
    ));
  }

  /**
   * Resolves the feature set JSON from the plan catalog for the given plan ID.
   * Returns null if the plan is not found in the catalog (e.g. legacy or external plan).
   */
  private String resolveFeatureSet(final String planId) {
    if (planId == null || planId.isBlank()) {
      return null;
    }
    return planMapper.findByPlanCode(planId)
        .map(plan -> {
          log.debug("Resolved feature set for planCode={}", planId);
          return plan.getFeatureSet();
        })
        .orElseGet(() -> {
          log.debug("Plan not found in catalog for planId={}, feature set will be null", planId);
          return null;
        });
  }
}
