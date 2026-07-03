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

import java.util.Comparator;
import java.util.List;

import com.iqkv.foundation.billingservice.infrastructure.config.BillingConfigurationProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal service-to-service endpoint exposing the plan feature catalog, and public endpoint for pricing pages.
 *
 * <p>Used by the gateway and downstream services to populate their local
 * {@code PlanCatalogCache} at startup and on periodic refresh. Also used by public pricing pages.
 * No authentication required: the response contains no sensitive data
 * (plan codes, names, descriptions, prices, and feature flags that are already publicly visible on the pricing page).
 */
@RestController
@RequestMapping("/api/v1/billing/internal/plans")
@Tag(name = "Plan Catalog Internal", description = "Internal service-to-service plan catalog + public pricing page data — no auth required")
public class PlanInternalRestResource {

  /**
   * Response payload for a single plan entry in the internal catalog.
   * Consumed by downstream services (IAM, Gateway) to populate their local
   * {@code PlanCatalogCache} at startup and on periodic refresh.
   *
   * @param planCode        the plan code (e.g. {@code "pro-monthly"})
   * @param entitlement      the typed feature set for this plan
   * @param pricingModel    pricing mode — {@code FLAT} or {@code PER_SEAT}; never null
   */
  public record PlanCatalogEntry(String planCode, PlanEntitlement entitlement, PricingModel pricingModel) {
  }

  private final PlanFeatureRegistry planFeatureRegistry;
  private final BillingConfigurationProperties billingProps;

  public PlanInternalRestResource(
      final PlanFeatureRegistry planFeatureRegistry,
      final BillingConfigurationProperties billingProps
  ) {
    this.planFeatureRegistry = planFeatureRegistry;
    this.billingProps = billingProps;
  }

  @GetMapping
  @Operation(
      summary = "List plan feature catalog",
      description = "Returns the full plan feature catalog as a list of planCode → entitlement entries. "
                    + "Intended for service-to-service use by the gateway and downstream services. "
                    + "No authentication required — response contains only non-sensitive plan feature data.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Plan catalog returned")
  })
  public ResponseEntity<List<PlanCatalogEntry>> listPlanCatalog() {
    final List<PlanCatalogEntry> entries = planFeatureRegistry.allEntries().values().stream()
        .map(e -> new PlanCatalogEntry(e.planCode(), e.features(), e.pricingModel()))
        .sorted(Comparator.comparing(PlanCatalogEntry::planCode))
        .toList();
    return ResponseEntity.ok(entries);
  }

  @GetMapping("/public")
  @Operation(
      summary = "List plans for pricing page",
      description = "Returns all active plans with full details (names, descriptions, prices, entitlement) for public pricing pages. "
                    + "No authentication required — response contains only non-sensitive, public plan data.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Public plan list returned")
  })
  public ResponseEntity<List<PublicPlanEntry>> listPublicPlans() {
    final List<PublicPlanEntry> entries = billingProps.planCatalog().products().values().stream()
        .filter(schema -> schema.active() == null || schema.active())
        .map(schema -> new PublicPlanEntry(
            schema.planCode(),
            schema.displayName(),
            schema.description(),
            schema.billingPeriod(),
            schema.priceMinor(),
            schema.currency(),
            schema.entitlement() != null ? schema.entitlement() : PlanEntitlement.NONE,
            schema.scope(),
            true,
            schema.effectivePricingModel()
        ))
        .sorted(Comparator.comparing(PublicPlanEntry::planCode))
        .toList();
    return ResponseEntity.ok(entries);
  }
}
