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
 * REST resource for tenant payment operations (e.g. refunds).
 */
@RestController
@RequestMapping("/api/v1/billing/payments")
@Tag(name = "Payments", description = "Tenant payment operations")
@SecurityRequirement(name = "bearerAuth")
@Validated
public class PaymentRestResource {

  private final SubscriptionService subscriptionService;

  public PaymentRestResource(final SubscriptionService subscriptionService) {
    this.subscriptionService = subscriptionService;
  }

  @PostMapping("/{tenantKey}/refund")
  @PreAuthorize("hasAnyAuthority('TENANT_OWNER', 'ADMIN')")
  @Operation(
      summary = "Create refund",
      description = "Creates a refund for a payment. Requires TENANT_OWNER or ADMIN authority. "
                    + "The authenticated tenant must match the tenantKey path variable.")
  @Parameter(name = "tenantKey", in = ParameterIn.PATH, required = true,
             description = "8-char alphanumeric tenantKey (e.g. xk7f2b9a)")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Refund created"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Access denied — not TENANT_OWNER or ADMIN or tenant mismatch")
  })
  public ResponseEntity<SubscriptionDtos.RefundResponse> createRefund(
      @PathVariable final String tenantKey,
      @RequestBody final SubscriptionDtos.CreateRefundRequest request,
      @AuthenticationPrincipal final Jwt jwt) {
    enforceOwnership(tenantKey, jwt);
    return ResponseEntity.ok(subscriptionService.createRefund(tenantKey, request));
  }

  @GetMapping("/{tenantKey}/refunds")
  @PreAuthorize("hasAnyAuthority('TENANT_OWNER', 'ADMIN')")
  @Operation(
      summary = "List refunds",
      description = "Returns all refunds for the given tenant ordered by occurred_at DESC. "
                    + "Requires TENANT_OWNER or ADMIN authority. The authenticated tenant must match the tenantKey path variable.")
  @Parameter(name = "tenantKey", in = ParameterIn.PATH, required = true,
             description = "8-char alphanumeric tenantKey (e.g. xk7f2b9a)")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Refund list returned"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Access denied — not TENANT_OWNER or ADMIN or tenant mismatch")
  })
  public ResponseEntity<List<SubscriptionDtos.AdminRefundResponse>> listRefunds(
      @PathVariable final String tenantKey,
      @AuthenticationPrincipal final Jwt jwt) {
    enforceOwnership(tenantKey, jwt);
    final var refunds = subscriptionService.getAllRefundsByTenantKey(tenantKey);
    return ResponseEntity.ok(refunds.stream()
        .map(RefundDtoMapper::toAdminResponse)
        .toList());
  }

  @PostMapping("/me/refund")
  @PreAuthorize("hasAnyAuthority('TENANT_OWNER', 'ADMIN')")
  @Operation(
      summary = "Create refund for current subject",
      description = "Creates a refund for a payment for the current subject (tenant or user depending on mode). "
          + "Requires TENANT_OWNER or ADMIN authority.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Refund created"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Access denied — not TENANT_OWNER or ADMIN")
  })
  public ResponseEntity<SubscriptionDtos.RefundResponse> createRefundForMe(
      @RequestBody final SubscriptionDtos.CreateRefundRequest request,
      @AuthenticationPrincipal final Jwt jwt) {
    final String tenantKey = jwt.getClaimAsString(JwtClaimNames.TENANT_ID);
    final UUID userId = UUID.fromString(jwt.getSubject());
    return ResponseEntity.ok(subscriptionService.createRefundForSubject(tenantKey, userId, request));
  }

  @GetMapping("/me/refunds")
  @PreAuthorize("hasAnyAuthority('TENANT_OWNER', 'ADMIN')")
  @Operation(
      summary = "List refunds for current subject",
      description = "Returns all refunds for the current subject (tenant or user depending on mode), "
          + "ordered by occurred_at DESC. Requires TENANT_OWNER or ADMIN authority.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Refund list returned"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Access denied — not TENANT_OWNER or ADMIN")
  })
  public ResponseEntity<List<SubscriptionDtos.AdminRefundResponse>> listRefundsForMe(
      @AuthenticationPrincipal final Jwt jwt) {
    final String tenantKey = jwt.getClaimAsString(JwtClaimNames.TENANT_ID);
    final UUID userId = UUID.fromString(jwt.getSubject());
    final var refunds = subscriptionService.getAllRefundsBySubject(tenantKey, userId);
    return ResponseEntity.ok(refunds.stream()
        .map(RefundDtoMapper::toAdminResponse)
        .toList());
  }

  /**
   * Verifies that the authenticated tenant matches the requested tenantKey.
   */
  private void enforceOwnership(final String tenantKey, final Jwt jwt) {
    final String authenticatedTenantKey = jwt.getClaimAsString(JwtClaimNames.TENANT_ID);
    if (!tenantKey.equals(authenticatedTenantKey)) {
      throw new TenantContextMismatchException(
          "Authenticated tenant '" + authenticatedTenantKey + "' does not match requested tenant '" + tenantKey + "'");
    }
  }
}
