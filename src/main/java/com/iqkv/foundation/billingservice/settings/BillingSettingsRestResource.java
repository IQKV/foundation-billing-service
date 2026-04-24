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

package com.iqkv.foundation.billingservice.settings;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST resource for tenant billing settings.
 *
 * <p>Requires {@code TENANT_OWNER} authority — enforced at the security layer.
 * The {@code tenantKey} path variable must match the authenticated tenant's key.
 */
@RestController
@RequestMapping("/api/v1/billing/settings")
@Tag(name = "Billing Settings", description = "Tenant billing configuration management")
@SecurityRequirement(name = "bearerAuth")
public class BillingSettingsRestResource {

  private final BillingSettingsService billingSettingsService;

  public BillingSettingsRestResource(BillingSettingsService billingSettingsService) {
    this.billingSettingsService = billingSettingsService;
  }

  /**
   * Returns billing settings for the given tenant.
   *
   * @param tenantKey the tenant identifier
   * @return 200 with billing settings, or 404 if not found
   */
  @GetMapping("/{tenantKey}")
  @Operation(summary = "Get billing settings", description = "Returns billing settings for the given tenant. Requires TENANT_OWNER authority.")
  @Parameter(name = "X-Tenant-ID", in = ParameterIn.HEADER, required = true, description = "8-char alphanumeric tenantKey")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Billing settings returned"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Access denied — not TENANT_OWNER"),
      @ApiResponse(responseCode = "404", description = "Tenant not found")
  })
  public ResponseEntity<BillingSettingsResponse> getSettings(@PathVariable String tenantKey) {
    final var settings = billingSettingsService.getByTenantKey(tenantKey);
    return ResponseEntity.ok(BillingSettingsResponse.from(settings));
  }

  /**
   * Partially updates billing settings for the given tenant.
   * Only non-null fields in the request body are applied.
   *
   * @param tenantKey the tenant identifier
   * @param request   the fields to update
   * @return 200 with updated billing settings, or 404 if not found
   */
  @PatchMapping("/{tenantKey}")
  @Operation(summary = "Update billing settings", description = "Partially updates billing settings. Only non-null fields are applied. Requires TENANT_OWNER authority.")
  @Parameter(name = "X-Tenant-ID", in = ParameterIn.HEADER, required = true, description = "8-char alphanumeric tenantKey")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Billing settings updated"),
      @ApiResponse(responseCode = "400", description = "Validation error"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Access denied — not TENANT_OWNER"),
      @ApiResponse(responseCode = "404", description = "Tenant not found")
  })
  public ResponseEntity<BillingSettingsResponse> updateSettings(
      @PathVariable String tenantKey,
      @Valid @RequestBody BillingSettingsRequest request) {
    final var updated = billingSettingsService.update(tenantKey, request);
    return ResponseEntity.ok(BillingSettingsResponse.from(updated));
  }
}
