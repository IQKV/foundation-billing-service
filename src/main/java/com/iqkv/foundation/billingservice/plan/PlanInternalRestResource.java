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

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal service-to-service endpoint exposing the plan feature catalog.
 *
 * <p>Used by the gateway and downstream services to populate their local
 * {@code PlanCatalogCache} at startup and on periodic refresh. Not exposed
 * via a public gateway route — accessible only within the internal network.
 *
 * <p>Backed entirely by the in-memory {@link PlanFeatureRegistry} — no DB reads.
 */
@RestController
@RequestMapping("/api/v1/billing/internal/plans")
@Tag(name = "Plan Catalog Internal", description = "Internal service-to-service plan feature catalog — requires PLATFORM_SERVICE authority")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAuthority('PLATFORM_SERVICE')")
public class PlanInternalRestResource {

  /**
   * Response payload for a single plan entry in the internal catalog.
   *
   * @param planCode the plan code (e.g. {@code "pro-monthly"})
   * @param features the typed feature set for this plan
   */
  public record PlanCatalogEntry(String planCode, PlanFeatures features) {
  }

  private final PlanFeatureRegistry planFeatureRegistry;

  public PlanInternalRestResource(final PlanFeatureRegistry planFeatureRegistry) {
    this.planFeatureRegistry = planFeatureRegistry;
  }

  @GetMapping
  @Operation(
      summary = "List plan feature catalog",
      description = "Returns the full plan feature catalog as a list of planCode → features entries. "
                    + "Intended for service-to-service use by the gateway and downstream services. "
                    + "Requires PLATFORM_SERVICE authority.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Plan catalog returned"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Access denied — PLATFORM_SERVICE required")
  })
  public ResponseEntity<List<PlanCatalogEntry>> listPlanCatalog() {
    final List<PlanCatalogEntry> entries = planFeatureRegistry.all().entrySet().stream()
        .map(e -> new PlanCatalogEntry(e.getKey(), e.getValue()))
        .sorted(java.util.Comparator.comparing(PlanCatalogEntry::planCode))
        .toList();
    return ResponseEntity.ok(entries);
  }
}
