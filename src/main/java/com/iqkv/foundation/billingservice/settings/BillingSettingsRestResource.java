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

package com.iqkv.foundation.billingservice.settings;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;

import com.iqkv.foundation.billingservice.infrastructure.security.JwtClaimNames;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * REST resource for tenant billing settings.
 *
 * <p>Requires {@code TENANT_OWNER} authority — enforced via {@code @PreAuthorize}.
 * The {@code tenantKey} path variable is validated against the authenticated tenant's JWT claim
 * inside the service layer to prevent cross-tenant data access.
 */
@RestController
@RequestMapping("/api/v1/billing/settings")
@Tag(name = "Billing Settings", description = "Tenant billing configuration management")
@SecurityRequirement(name = "bearerAuth")
@Validated
public class BillingSettingsRestResource {

  private final BillingSettingsService billingSettingsService;

  public BillingSettingsRestResource(final BillingSettingsService billingSettingsService) {
    this.billingSettingsService = billingSettingsService;
  }

  @GetMapping("/{tenantKey}")
  @PreAuthorize("hasAnyAuthority('TENANT_OWNER', 'ADMIN')")
  @Operation(
      summary = "Get billing settings",
      description = "Returns billing settings for the given tenant. Requires TENANT_OWNER or ADMIN authority. "
                    + "The authenticated tenant must match the tenantKey path variable.")
  @Parameter(name = "tenantKey", in = ParameterIn.PATH, required = true,
             description = "8-char alphanumeric tenantKey (e.g. xk7f2b9a)")
  @Parameter(name = "X-Tenant-ID", in = ParameterIn.HEADER, required = true,
             description = "8-char alphanumeric tenantKey (e.g. xk7f2b9a)")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Billing settings returned"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Access denied — not TENANT_OWNER or ADMIN or tenant mismatch"),
      @ApiResponse(responseCode = "404", description = "Tenant billing settings not found")
  })
  public ResponseEntity<BillingSettingsDtos.BillingSettingsResponse> getSettings(
      @Parameter(description = "8-char alphanumeric tenantKey")
      @PathVariable @Pattern(regexp = "[a-z0-9]{8}", message = "tenantKey must be 8 lowercase alphanumeric characters") final String tenantKey,
      @AuthenticationPrincipal final Jwt jwt) {
    final String authenticatedTenantKey = jwt.getClaimAsString(JwtClaimNames.TENANT_ID);
    if (!tenantKey.equals(authenticatedTenantKey)) {
      throw new com.iqkv.foundation.billingservice.shared.exception.TenantContextMismatchException(
          "Authenticated tenant '" + authenticatedTenantKey + "' does not match requested tenant '" + tenantKey + "'");
    }
    final var settings = billingSettingsService.getByTenantKey(tenantKey);
    return ResponseEntity.ok(BillingSettingsDtoMapper.toResponse(settings));
  }

  @PostMapping("/{tenantKey}")
  @PreAuthorize("hasAnyAuthority('TENANT_OWNER', 'ADMIN')")
  @Operation(
      summary = "Create billing settings",
      description = "Creates billing settings for the tenant. Billing email and currency are required. "
                    + "Returns 409 if settings already exist. Requires TENANT_OWNER or ADMIN authority. "
                    + "The authenticated tenant must match the tenantKey path variable.")
  @Parameter(name = "tenantKey", in = ParameterIn.PATH, required = true,
             description = "8-char alphanumeric tenantKey (e.g. xk7f2b9a)")
  @Parameter(name = "X-Tenant-ID", in = ParameterIn.HEADER, required = true,
             description = "8-char alphanumeric tenantKey (e.g. xk7f2b9a)")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "Billing settings created"),
      @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
      @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
      @ApiResponse(responseCode = "403", description = "Access denied — not TENANT_OWNER or ADMIN or tenant mismatch", content = @Content),
      @ApiResponse(responseCode = "409", description = "Billing settings already exist for this tenant", content = @Content)
  })
  public ResponseEntity<BillingSettingsDtos.BillingSettingsResponse> createSettings(
      @Parameter(description = "8-char alphanumeric tenantKey")
      @PathVariable @Pattern(regexp = "[a-z0-9]{8}", message = "tenantKey must be 8 lowercase alphanumeric characters") final String tenantKey,
      @Valid @RequestBody final BillingSettingsDtos.CreateBillingSettingsRequest request,
      @AuthenticationPrincipal final Jwt jwt) {
    final String authenticatedTenantKey = jwt.getClaimAsString(JwtClaimNames.TENANT_ID);
    final var created = billingSettingsService.createForTenant(tenantKey, authenticatedTenantKey, request);
    return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest().build().toUri())
        .body(BillingSettingsDtoMapper.toResponse(created));
  }

  @PatchMapping("/{tenantKey}")
  @PreAuthorize("hasAnyAuthority('TENANT_OWNER', 'ADMIN')")
  @Operation(
      summary = "Update billing settings",
      description = "Partially updates billing settings. Only non-null fields are applied. "
                    + "Requires TENANT_OWNER or ADMIN authority. The authenticated tenant must match the tenantKey path variable.")
  @Parameter(name = "tenantKey", in = ParameterIn.PATH, required = true,
             description = "8-char alphanumeric tenantKey (e.g. xk7f2b9a)")
  @Parameter(name = "X-Tenant-ID", in = ParameterIn.HEADER, required = true,
             description = "8-char alphanumeric tenantKey (e.g. xk7f2b9a)")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Billing settings updated"),
      @ApiResponse(responseCode = "400", description = "Validation error"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Access denied — not TENANT_OWNER or ADMIN or tenant mismatch"),
      @ApiResponse(responseCode = "404", description = "Tenant billing settings not found")
  })
  public ResponseEntity<BillingSettingsDtos.BillingSettingsResponse> updateSettings(
      @Parameter(description = "8-char alphanumeric tenantKey")
      @PathVariable @Pattern(regexp = "[a-z0-9]{8}", message = "tenantKey must be 8 lowercase alphanumeric characters") final String tenantKey,
      @Valid @RequestBody final BillingSettingsDtos.UpdateBillingSettingsRequest request,
      @AuthenticationPrincipal final Jwt jwt) {
    final String authenticatedTenantKey = jwt.getClaimAsString(JwtClaimNames.TENANT_ID);
    final var settings = billingSettingsService.update(tenantKey, authenticatedTenantKey, request);
    return ResponseEntity.ok(BillingSettingsDtoMapper.toResponse(settings));
  }

  @PostMapping("/{tenantKey}/portal")
  @PreAuthorize("hasAnyAuthority('TENANT_OWNER', 'ADMIN')")
  @Operation(
      summary = "Create customer portal session",
      description = "Creates a Stripe Customer Portal session and returns the URL for redirection. "
                    + "Requires TENANT_OWNER or ADMIN authority. The authenticated tenant must match the tenantKey path variable.")
  @Parameter(name = "tenantKey", in = ParameterIn.PATH, required = true,
             description = "8-char alphanumeric tenantKey (e.g. xk7f2b9a)")
  @Parameter(name = "X-Tenant-ID", in = ParameterIn.HEADER, required = true,
             description = "8-char alphanumeric tenantKey (e.g. xk7f2b9a)")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Portal session created"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Access denied — not TENANT_OWNER or ADMIN or tenant mismatch"),
      @ApiResponse(responseCode = "404", description = "Tenant billing settings or customer ID not found")
  })
  public ResponseEntity<BillingSettingsDtos.PortalSessionResponse> createPortalSession(
      @Parameter(description = "8-char alphanumeric tenantKey")
      @PathVariable @Pattern(regexp = "[a-z0-9]{8}", message = "tenantKey must be 8 lowercase alphanumeric characters") final String tenantKey,
      @AuthenticationPrincipal final Jwt jwt) {
    final String authenticatedTenantKey = jwt.getClaimAsString(JwtClaimNames.TENANT_ID);
    if (!tenantKey.equals(authenticatedTenantKey)) {
      throw new com.iqkv.foundation.billingservice.shared.exception.TenantContextMismatchException(
          "Authenticated tenant '" + authenticatedTenantKey + "' does not match requested tenant '" + tenantKey + "'");
    }
    final String url = billingSettingsService.createPortalSession(tenantKey);
    return ResponseEntity.ok(new BillingSettingsDtos.PortalSessionResponse(url));
  }
}
