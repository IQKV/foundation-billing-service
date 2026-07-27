/*
 * Copyright 2026 iQKV Foundation Team.
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
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Platform-operator read-only access for the subscription plan catalog.
 *
 * <p>Plans are global (not tenant-scoped); the admin base path groups them with other
 * {@code PLATFORM_ADMIN} billing operations under {@code /api/v1/billing/admin/…}.
 */
@RestController
@RequestMapping("/api/v1/billing/admin/plans")
@Tag(name = "Plan Catalog Admin", description = "Platform operator read-only views for plan catalog — requires PLATFORM_ADMIN authority")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
@Validated
public class PlanCatalogAdminRestResource {

  private final PlanCatalogService planCatalogService;

  public PlanCatalogAdminRestResource(final PlanCatalogService planCatalogService) {
    this.planCatalogService = planCatalogService;
  }

  @GetMapping
  @Operation(summary = "List all plans",
             description = "Returns every plan in the catalog (including inactive), ordered by plan_code.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Plan list returned"),
      @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
      @ApiResponse(responseCode = "403", description = "Access denied — PLATFORM_ADMIN required", content = @Content)
  })
  public ResponseEntity<List<Plan>> listAll() {
    return ResponseEntity.ok(planCatalogService.listAllPlans());
  }

  @GetMapping("/{planCode}")
  @Operation(summary = "Get plan by code")
  @Parameter(name = "planCode", in = ParameterIn.PATH, required = true,
             description = "Unique plan identifier (e.g. pro-monthly)")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Plan returned"),
      @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
      @ApiResponse(responseCode = "403", description = "Access denied — PLATFORM_ADMIN required", content = @Content),
      @ApiResponse(responseCode = "404", description = "Plan not found", content = @Content)
  })
  public ResponseEntity<Plan> get(@PathVariable final String planCode) {
    return ResponseEntity.ok(planCatalogService.getPlanOrThrow(planCode));
  }
}
