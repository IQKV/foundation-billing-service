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

import jakarta.validation.Valid;
import java.util.List;

import com.iqkv.foundation.billingservice.infrastructure.persistence.PlanMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST resource for plan catalog management.
 *
 * <p>Read endpoints are accessible to any authenticated user.
 * Mutation endpoints (create, update, delete) require the {@code PLATFORM_ADMIN} authority.
 *
 * <p>DELETE performs a soft-delete by setting {@code active = false} rather than removing the row.
 */
@RestController
@RequestMapping("/api/v1/billing/plans")
@Tag(name = "Plan Catalog", description = "Subscription plan catalog management")
@SecurityRequirement(name = "bearerAuth")
@Validated
public class PlanCatalogRestResource {

  private final PlanMapper planMapper;

  public PlanCatalogRestResource(final PlanMapper planMapper) {
    this.planMapper = planMapper;
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
    return ResponseEntity.ok(planMapper.findAllActive());
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
    final Plan plan = planMapper.findByPlanCode(planCode)
        .orElseThrow(() -> new PlanNotFoundException(planCode));
    return ResponseEntity.ok(plan);
  }

  @PostMapping
  @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
  @Operation(
      summary = "Create a new plan",
      description = "Creates a new plan in the catalog. Requires PLATFORM_ADMIN authority.")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "Plan created"),
      @ApiResponse(responseCode = "400", description = "Validation error"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Access denied — PLATFORM_ADMIN required")
  })
  public ResponseEntity<Plan> createPlan(@Valid @RequestBody final PlanRequest request) {
    final Plan plan = toPlan(request);
    planMapper.insert(plan);
    final Plan created = planMapper.findByPlanCode(plan.getPlanCode())
        .orElseThrow(() -> new PlanNotFoundException(plan.getPlanCode()));
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  @PutMapping("/{planCode}")
  @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
  @Operation(
      summary = "Update a plan",
      description = "Replaces mutable fields of the plan identified by planCode. "
          + "Requires PLATFORM_ADMIN authority.")
  @Parameter(name = "planCode", in = ParameterIn.PATH, required = true,
      description = "Unique plan identifier (e.g. pro-monthly)")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Plan updated"),
      @ApiResponse(responseCode = "400", description = "Validation error"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Access denied — PLATFORM_ADMIN required"),
      @ApiResponse(responseCode = "404", description = "Plan not found")
  })
  public ResponseEntity<Plan> updatePlan(
      @PathVariable final String planCode,
      @Valid @RequestBody final PlanRequest request) {
    planMapper.findByPlanCode(planCode)
        .orElseThrow(() -> new PlanNotFoundException(planCode));

    final Plan plan = toPlan(request);
    plan.setPlanCode(planCode);
    planMapper.update(plan);

    final Plan updated = planMapper.findByPlanCode(planCode)
        .orElseThrow(() -> new PlanNotFoundException(planCode));
    return ResponseEntity.ok(updated);
  }

  @DeleteMapping("/{planCode}")
  @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
  @Operation(
      summary = "Deactivate a plan",
      description = "Soft-deletes the plan by setting active = false. "
          + "The row is retained for historical reference. "
          + "Requires PLATFORM_ADMIN authority.")
  @Parameter(name = "planCode", in = ParameterIn.PATH, required = true,
      description = "Unique plan identifier (e.g. pro-monthly)")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Plan deactivated"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Access denied — PLATFORM_ADMIN required"),
      @ApiResponse(responseCode = "404", description = "Plan not found")
  })
  public ResponseEntity<Void> deactivatePlan(@PathVariable final String planCode) {
    final Plan plan = planMapper.findByPlanCode(planCode)
        .orElseThrow(() -> new PlanNotFoundException(planCode));

    plan.setActive(false);
    planMapper.update(plan);

    return ResponseEntity.noContent().build();
  }

  private Plan toPlan(final PlanRequest request) {
    final Plan plan = new Plan();
    plan.setPlanCode(request.planCode());
    plan.setDisplayName(request.displayName());
    plan.setBillingPeriod(request.billingPeriod());
    plan.setPriceMinor(request.priceMinor());
    plan.setCurrency(request.currency() != null ? request.currency() : "USD");
    plan.setFeatureSet(request.featureSet());
    plan.setScope(request.scope());
    plan.setActive(request.active() != null ? request.active() : Boolean.TRUE);
    return plan;
  }
}
