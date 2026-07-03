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

import java.time.Instant;
import java.util.UUID;

import com.iqkv.foundation.billingservice.infrastructure.security.JwtClaimNames;
import com.iqkv.foundation.billingservice.plan.PlanEntitlement;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Self-service endpoint exposing the current subject's entitlement details.
 *
 * <p>Returns the resolved plan code, subscription status, period end, and typed
 * {@link PlanEntitlement} for the active subscription of the authenticated subject.
 * Suitable for UI plan status display and one-off quota lookups.
 */
@RestController
@RequestMapping("/api/v1/billing/entitlements")
@Tag(name = "Entitlements", description = "Current subject's active plan entitlements")
@SecurityRequirement(name = "bearerAuth")
public class EntitlementRestResource {

  /**
   * Response payload for the entitlements endpoint.
   *
   * @param planCode         the human-readable plan code (e.g. {@code "pro-monthly"})
   * @param status           the subscription status (e.g. {@code "active"}, {@code "trialing"})
   * @param currentPeriodEnd when the current billing period ends
   * @param entitlement  the typed feature set for the active plan
   */
  public record EntitlementResponse(
      String planCode,
      String status,
      Instant currentPeriodEnd,
      PlanEntitlement planEntitlement
  ) {
  }

  private final EntitlementEvaluator entitlementEvaluator;
  private final SubscriptionSubjectResolver subjectResolver;

  public EntitlementRestResource(final EntitlementEvaluator entitlementEvaluator,
                                 final SubscriptionSubjectResolver subjectResolver) {
    this.entitlementEvaluator = entitlementEvaluator;
    this.subjectResolver = subjectResolver;
  }

  @GetMapping("/me")
  @PreAuthorize("hasAnyAuthority('TENANT_OWNER', 'ADMIN', 'MEMBER')")
  @Operation(
      summary = "Get current subject entitlements",
      description = "Returns the active plan code, subscription status, period end, and typed feature set "
                    + "for the current authenticated subject. Resolves subject by rollout mode "
                    + "(TENANT in multi-tenant, USER in single-tenant). Returns free plan entitlements "
                    + "when no active subscription exists.")
  @Parameter(name = "X-Tenant-ID", in = ParameterIn.HEADER, required = true,
             description = "Tenant key propagated by the gateway")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Entitlements returned"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Access denied")
  })
  public ResponseEntity<EntitlementResponse> getMyEntitlements(
      @AuthenticationPrincipal final Jwt jwt) {
    final String tenantKey = jwt.getClaimAsString(JwtClaimNames.TENANT_ID);
    final UUID userId = UUID.fromString(jwt.getClaimAsString(JwtClaimNames.USER_ID));

    final SubscriptionSubject subject = subjectResolver.resolveSubject(tenantKey, userId);

    final EntitlementDetails details = entitlementEvaluator.evaluateEntitlements(subject)
        .orElseThrow(() -> new IllegalStateException("Entitlements should always be present"));

    return ResponseEntity.ok(new EntitlementResponse(
        details.planCode(),
        details.status(),
        details.currentPeriodEnd(),
        details.features()
    ));
  }
}
