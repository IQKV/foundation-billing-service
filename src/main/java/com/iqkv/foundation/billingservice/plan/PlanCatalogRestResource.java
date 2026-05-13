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
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public read-only plan catalog for authenticated users.
 *
 * <p>Mutations are available only under {@code /api/v1/billing/admin/plans} for {@code PLATFORM_ADMIN}.
 */
@RestController
@RequestMapping("/api/v1/billing/plans")
@Tag(name = "Plan Catalog", description = "Subscription plan catalog (read-only for authenticated users)")
@SecurityRequirement(name = "bearerAuth")
@Validated
public class PlanCatalogRestResource {

  private final PlanCatalogService planCatalogService;

  public PlanCatalogRestResource(final PlanCatalogService planCatalogService) {
    this.planCatalogService = planCatalogService;
  }

  @GetMapping
  @Operation(
      summary = "List all active plans",
      description = "Returns all active plans in the catalog, ordered by plan_code ASC. "
          + "Accessible to any authenticated user.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Plan list returned"),
      @ApiResponse(responseCode = "401", description = "Unauthorized")
  })
  public ResponseEntity<List<Plan>> listActivePlans() {
    return ResponseEntity.ok(planCatalogService.listActivePlans());
  }

  @GetMapping("/{planCode}")
  @Operation(
      summary = "Get plan by planCode",
      description = "Returns the plan with the given planCode. Accessible to any authenticated user.")
  @Parameter(name = "planCode", in = ParameterIn.PATH, required = true,
      description = "Unique plan identifier (e.g. pro-monthly)")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Plan returned"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "404", description = "Plan not found")
  })
  public ResponseEntity<Plan> getPlan(@PathVariable final String planCode) {
    return ResponseEntity.ok(planCatalogService.getPlanOrThrow(planCode));
  }
}
