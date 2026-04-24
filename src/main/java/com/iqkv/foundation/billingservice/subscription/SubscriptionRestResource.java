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

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST resource for tenant subscription queries.
 *
 * <p>Subscription data is a local cache of Stripe state — no payment gateway
 * round-trips are made. Requires {@code TENANT_OWNER} authority.
 */
@RestController
@RequestMapping("/api/v1/billing/subscriptions")
@Tag(name = "Subscriptions", description = "Tenant subscription queries (local Stripe cache)")
@SecurityRequirement(name = "bearerAuth")
public class SubscriptionRestResource {

  private final SubscriptionService subscriptionService;

  public SubscriptionRestResource(SubscriptionService subscriptionService) {
    this.subscriptionService = subscriptionService;
  }

  /**
   * Returns the active subscription for the given tenant.
   *
   * @param tenantKey the tenant identifier
   * @return 200 with the active subscription, or 404 if none exists
   */
  @GetMapping("/{tenantKey}/active")
  @Operation(summary = "Get active subscription", description = "Returns the active Stripe subscription for the given tenant. No gateway round-trip — reads local cache.")
  @Parameter(name = "X-Tenant-ID", in = ParameterIn.HEADER, required = true, description = "8-char alphanumeric tenantKey")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Active subscription returned"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Access denied — not TENANT_OWNER"),
      @ApiResponse(responseCode = "404", description = "No active subscription found")
  })
  public ResponseEntity<SubscriptionResponse> getActive(@PathVariable String tenantKey) {
    final var subscription = subscriptionService.getActiveByTenantKey(tenantKey);
    return ResponseEntity.ok(SubscriptionResponse.from(subscription));
  }

  /**
   * Returns all subscriptions for the given tenant, ordered by {@code created_at DESC}.
   *
   * @param tenantKey the tenant identifier
   * @return 200 with the list (may be empty)
   */
  @GetMapping("/{tenantKey}")
  @Operation(summary = "Get all subscriptions", description = "Returns all subscriptions for the given tenant ordered by created_at DESC. May be empty.")
  @Parameter(name = "X-Tenant-ID", in = ParameterIn.HEADER, required = true, description = "8-char alphanumeric tenantKey")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Subscription list returned"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Access denied — not TENANT_OWNER")
  })
  public ResponseEntity<List<SubscriptionResponse>> getAll(@PathVariable String tenantKey) {
    final var subscriptions = subscriptionService.getAllByTenantKey(tenantKey)
        .stream()
        .map(SubscriptionResponse::from)
        .toList();
    return ResponseEntity.ok(subscriptions);
  }
}
