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

import jakarta.validation.constraints.Pattern;
import java.util.List;
import java.util.UUID;

import com.iqkv.foundation.billingservice.infrastructure.security.JwtClaimNames;
import com.iqkv.foundation.billingservice.shared.exception.TenantContextMismatchException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST resource for tenant subscription queries.
 *
 * <p>Subscription data is a local cache of Stripe state — no payment gateway
 * round-trips are made. Requires {@code TENANT_OWNER} authority — enforced via {@code @PreAuthorize}.
 * The {@code tenantKey} path variable is validated against the authenticated tenant's JWT claim
 * to prevent cross-tenant data access.
 */
@RestController
@RequestMapping("/api/v1/billing/subscriptions")
@Tag(name = "Subscriptions", description = "Tenant subscription queries (local Stripe cache)")
@SecurityRequirement(name = "bearerAuth")
@Validated
public class SubscriptionRestResource {

  private final SubscriptionService subscriptionService;

  public SubscriptionRestResource(final SubscriptionService subscriptionService) {
    this.subscriptionService = subscriptionService;
  }

  @GetMapping("/{tenantKey}/active")
  @PreAuthorize("hasAuthority('TENANT_OWNER')")
  @Operation(
      summary = "Get active subscription",
      description = "Returns the active Stripe subscription for the given tenant. "
          + "No gateway round-trip — reads local cache. "
          + "Requires TENANT_OWNER authority. The authenticated tenant must match the tenantKey path variable.")
  @Parameter(name = "tenantKey", in = ParameterIn.PATH, required = true,
      description = "8-char alphanumeric tenantKey (e.g. xk7f2b9a)")
  @Parameter(name = "X-Tenant-ID", in = ParameterIn.HEADER, required = true,
      description = "8-char alphanumeric tenantKey (e.g. xk7f2b9a)")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Active subscription returned"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Access denied — not TENANT_OWNER or tenant mismatch"),
      @ApiResponse(responseCode = "404", description = "No active subscription found")
  })
  public ResponseEntity<SubscriptionDtos.SubscriptionResponse> getActive(
      @Parameter(description = "8-char alphanumeric tenantKey")
      @PathVariable @Pattern(regexp = "[a-z0-9]{8}", message = "tenantKey must be 8 lowercase alphanumeric characters") final String tenantKey,
      @AuthenticationPrincipal final Jwt jwt) {
    enforceOwnership(tenantKey, jwt);
    final var subscription = subscriptionService.getActiveByTenantKey(tenantKey);
    return ResponseEntity.ok(SubscriptionDtoMapper.toResponse(subscription));
  }

  @GetMapping("/{tenantKey}")
  @PreAuthorize("hasAuthority('TENANT_OWNER')")
  @Operation(
      summary = "Get all subscriptions",
      description = "Returns all subscriptions for the given tenant ordered by created_at DESC. May be empty. "
          + "Requires TENANT_OWNER authority. The authenticated tenant must match the tenantKey path variable.")
  @Parameter(name = "tenantKey", in = ParameterIn.PATH, required = true,
      description = "8-char alphanumeric tenantKey (e.g. xk7f2b9a)")
  @Parameter(name = "X-Tenant-ID", in = ParameterIn.HEADER, required = true,
      description = "8-char alphanumeric tenantKey (e.g. xk7f2b9a)")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Subscription list returned"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Access denied — not TENANT_OWNER or tenant mismatch")
  })
  public ResponseEntity<List<SubscriptionDtos.SubscriptionResponse>> getAll(
      @Parameter(description = "8-char alphanumeric tenantKey")
      @PathVariable @Pattern(regexp = "[a-z0-9]{8}", message = "tenantKey must be 8 lowercase alphanumeric characters") final String tenantKey,
      @AuthenticationPrincipal final Jwt jwt) {
    enforceOwnership(tenantKey, jwt);
    final var subscriptions = subscriptionService.getAllByTenantKey(tenantKey);
    return ResponseEntity.ok(subscriptions.stream().map(SubscriptionDtoMapper::toResponse).toList());
  }

  @PostMapping("/{tenantKey}/checkout")
  @PreAuthorize("hasAuthority('TENANT_OWNER')")
  @Operation(
      summary = "Create checkout session",
      description = "Creates a Stripe Checkout Session for subscription creation. "
          + "Requires TENANT_OWNER authority.")
  public ResponseEntity<SubscriptionDtos.CheckoutSessionResponse> createCheckout(
      @PathVariable @Pattern(regexp = "[a-z0-9]{8}") final String tenantKey,
      @RequestBody final SubscriptionDtos.CreateCheckoutSessionRequest request,
      @AuthenticationPrincipal final Jwt jwt) {
    enforceOwnership(tenantKey, jwt);
    return ResponseEntity.ok(subscriptionService.createCheckoutSession(tenantKey, request));
  }

  @PostMapping("/{tenantKey}/{externalSubscriptionId}")
  @PreAuthorize("hasAuthority('TENANT_OWNER')")
  @Operation(
      summary = "Update subscription",
      description = "Updates an existing subscription (upgrade/downgrade, quantity change). "
          + "Requires TENANT_OWNER authority.")
  public ResponseEntity<Void> updateSubscription(
      @PathVariable @Pattern(regexp = "[a-z0-9]{8}") final String tenantKey,
      @PathVariable final String externalSubscriptionId,
      @RequestBody final SubscriptionDtos.UpdateSubscriptionRequest request,
      @AuthenticationPrincipal final Jwt jwt) {
    enforceOwnership(tenantKey, jwt);
    subscriptionService.updateSubscription(tenantKey, externalSubscriptionId, request);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/me/active")
  @PreAuthorize("hasAnyAuthority('TENANT_OWNER', 'MEMBER')")
  @Operation(
      summary = "Get active subscription for current subject",
      description = "Returns the active subscription for the resolved subject (tenant in multi-tenant mode, "
          + "user in single-tenant mode). Requires TENANT_OWNER or MEMBER authority.")
  @Parameter(name = "X-Tenant-ID", in = ParameterIn.HEADER, required = true,
      description = "8-char alphanumeric tenantKey (e.g. xk7f2b9a)")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Active subscription returned"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Access denied"),
      @ApiResponse(responseCode = "404", description = "No active subscription found")
  })
  public ResponseEntity<SubscriptionDtos.SubscriptionResponse> getActiveForSubject(
      @AuthenticationPrincipal final Jwt jwt) {
    final String tenantKey = jwt.getClaimAsString(JwtClaimNames.TENANT_ID);
    final UUID userId = UUID.fromString(jwt.getSubject());
    final var subscription = subscriptionService.getActiveBySubject(tenantKey, userId);
    return ResponseEntity.ok(SubscriptionDtoMapper.toResponse(subscription));
  }

  @GetMapping("/me")
  @PreAuthorize("hasAnyAuthority('TENANT_OWNER', 'MEMBER')")
  @Operation(
      summary = "Get all subscriptions for current subject",
      description = "Returns all subscriptions for the resolved subject (tenant in multi-tenant mode, "
          + "user in single-tenant mode), ordered by created_at DESC. Requires TENANT_OWNER or MEMBER authority.")
  @Parameter(name = "X-Tenant-ID", in = ParameterIn.HEADER, required = true,
      description = "8-char alphanumeric tenantKey (e.g. xk7f2b9a)")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Subscription list returned"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Access denied")
  })
  public ResponseEntity<List<SubscriptionDtos.SubscriptionResponse>> getAllForSubject(
      @AuthenticationPrincipal final Jwt jwt) {
    final String tenantKey = jwt.getClaimAsString(JwtClaimNames.TENANT_ID);
    final UUID userId = UUID.fromString(jwt.getSubject());
    final var subscriptions = subscriptionService.getAllBySubject(tenantKey, userId)
        .stream()
        .map(SubscriptionDtoMapper::toResponse)
        .toList();
    return ResponseEntity.ok(subscriptions);
  }

  /**
   * Verifies that the authenticated tenant matches the requested tenantKey.
   * Throws {@link TenantContextMismatchException} (→ HTTP 403) on mismatch.
   */
  private void enforceOwnership(final String tenantKey, final Jwt jwt) {
    final String authenticatedTenantKey = jwt.getClaimAsString(JwtClaimNames.TENANT_ID);
    if (!tenantKey.equals(authenticatedTenantKey)) {
      throw new TenantContextMismatchException(
          "Authenticated tenant '" + authenticatedTenantKey + "' does not match requested tenant '" + tenantKey + "'");
    }
  }
}
