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

package com.iqkv.foundation.billingservice.plan;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.iqkv.foundation.billingservice.infrastructure.config.BillingConfigurationProperties;
import com.iqkv.foundation.billingservice.infrastructure.config.StripeProductSchema;
import org.springframework.stereotype.Component;

/**
 * In-memory registry of {@code planCode → PlanFeatures}, populated at startup from
 * {@link BillingConfigurationProperties}. Read-only after initialization — O(1) lookups
 * with no DB or network calls.
 *
 * <p>This is the single in-process authority on what features each plan includes.
 * It is also the backing store for {@code GET /api/v1/billing/internal/plans}.
 */
@Component
public class PlanFeatureRegistry {

  private final Map<String, PlanFeatures> registry;

  public PlanFeatureRegistry(final BillingConfigurationProperties props) {
    this.registry = props.stripe().schema().products().values().stream()
        .filter(s -> s.planCode() != null)
        .collect(Collectors.toUnmodifiableMap(
            StripeProductSchema::planCode,
            s -> s.features() != null ? s.features() : PlanFeatures.NONE
        ));
  }

  /**
   * Returns the {@link PlanFeatures} for the given plan code.
   * Falls back to {@link PlanFeatures#NONE} when the plan code is unknown.
   *
   * @param planCode the plan code to look up (e.g. {@code "pro-monthly"})
   * @return the plan's features, never {@code null}
   */
  public PlanFeatures forPlan(final String planCode) {
    if (planCode == null || planCode.isBlank()) {
      return PlanFeatures.NONE;
    }
    return registry.getOrDefault(planCode, PlanFeatures.NONE);
  }

  /**
   * Returns all registered plan codes.
   *
   * @return unmodifiable set of known plan codes
   */
  public Set<String> knownPlanCodes() {
    return registry.keySet();
  }

  /**
   * Returns the full registry as an unmodifiable map.
   * Used by the internal plans endpoint to serialize the catalog.
   *
   * @return unmodifiable map of planCode to PlanFeatures
   */
  public Map<String, PlanFeatures> all() {
    return registry;
  }
}
