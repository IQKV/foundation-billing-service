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
import com.iqkv.foundation.billingservice.infrastructure.config.ProductSchema;
import org.springframework.stereotype.Component;

/**
* In-memory registry of {@code planCode → PlanEntitlement} and {@code planCode → PricingModel},
 * populated at startup from {@link BillingConfigurationProperties}. Read-only after
 * initialization — O(1) lookups with no DB or network calls.
 *
 * <p>This is the single in-process authority on what features and pricing model each plan
 * includes. It is also the backing store for
 * {@code GET /api/v1/billing/internal/plans} consumed by IAM and Gateway.
 */
@Component
public class PlanFeatureRegistry {

  /**
   * Combines a plan's feature entitlements and pricing mode into a single transferable object.
   * Used by the internal plans endpoint so consumers receive both in one response entry.
   *
   * @param planCode     unique plan identifier (e.g. {@code "pro-monthly"})
   * @param features     typed feature set for entitlement checks and quota enforcement
   * @param pricingModel pricing mode — {@link PricingModel#FLAT} or {@link PricingModel#PER_SEAT}
   */
  public record PlanCatalogEntry(String planCode, PlanEntitlement features, PricingModel pricingModel) {
  }

  private final Map<String, PlanEntitlement> featureRegistry;
  private final Map<String, PricingModel> pricingRegistry;

  public PlanFeatureRegistry(final BillingConfigurationProperties props) {
    this.featureRegistry = props.planCatalog().products().values().stream()
        .filter(s -> s.planCode() != null)
        .collect(Collectors.toUnmodifiableMap(
            ProductSchema::planCode,
            s -> s.features() != null ? s.features() : PlanEntitlement.NONE
        ));
    this.pricingRegistry = props.planCatalog().products().values().stream()
        .filter(s -> s.planCode() != null)
        .collect(Collectors.toUnmodifiableMap(
            ProductSchema::planCode,
            ProductSchema::effectivePricingModel
        ));
  }

  /**
   * Returns the {@link PlanEntitlement} for the given plan code.
   * Falls back to {@link PlanEntitlement#NONE} when the plan code is unknown.
   *
   * @param planCode the plan code to look up (e.g. {@code "pro-monthly"})
   * @return the plan's features, never {@code null}
   */
  public PlanEntitlement resolveEntitlement(final String planCode) {
    if (planCode == null || planCode.isBlank()) {
      return PlanEntitlement.NONE;
    }
    return featureRegistry.getOrDefault(planCode, PlanEntitlement.NONE);
  }

  /**
   * Returns the {@link PricingModel} for the given plan code.
   * Falls back to {@link PricingModel#FLAT} when the plan code is unknown.
   *
   * @param planCode the plan code to look up
   * @return the plan's pricing model, never {@code null}
   */
  public PricingModel pricingModelForPlan(final String planCode) {
    if (planCode == null || planCode.isBlank()) {
      return PricingModel.FLAT;
    }
    return pricingRegistry.getOrDefault(planCode, PricingModel.FLAT);
  }

  /**
   * Returns all registered plan codes.
   *
   * @return unmodifiable set of known plan codes
   */
  public Set<String> knownPlanCodes() {
    return featureRegistry.keySet();
  }

  /**
   * Returns the full catalog as a map of {@code planCode → PlanEntitlement}.
   * Used internally where only feature data is needed.
   *
   * @return unmodifiable map of planCode to PlanEntitlement
   */
  public Map<String, PlanEntitlement> all() {
    return featureRegistry;
  }

  /**
   * Returns the full catalog as a map of {@code planCode → PlanCatalogEntry}, bundling
   * both features and pricing model. Used by the internal plans endpoint.
   *
   * @return unmodifiable map of planCode to PlanCatalogEntry
   */
  public Map<String, PlanCatalogEntry> allEntries() {
    return featureRegistry.keySet().stream()
        .collect(Collectors.toUnmodifiableMap(
            planCode -> planCode,
            planCode -> new PlanCatalogEntry(
                planCode,
                featureRegistry.get(planCode),
                pricingRegistry.getOrDefault(planCode, PricingModel.FLAT)
            )
        ));
  }
}
