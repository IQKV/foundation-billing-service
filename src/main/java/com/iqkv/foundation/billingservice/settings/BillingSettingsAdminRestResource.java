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
import jakarta.validation.constraints.Pattern;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Platform-operator CRUD for per-tenant billing settings.
 *
 * <p>Resource path nests billing settings under the tenant identifier ({@code /admin/tenants/{tenantKey}/…})
 * following common REST collection/member patterns.
 */
@RestController
@RequestMapping("/api/v1/billing/admin/tenants/{tenantKey}/billing-settings")
@Tag(name = "Billing Settings Admin", description = "Platform operator CRUD for tenant billing settings — requires PLATFORM_ADMIN authority")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
@Validated
public class BillingSettingsAdminRestResource {

  private final BillingSettingsService billingSettingsService;

  public BillingSettingsAdminRestResource(final BillingSettingsService billingSettingsService) {
    this.billingSettingsService = billingSettingsService;
  }

  @GetMapping
  @Operation(summary = "Get billing settings for tenant",
      description = "Returns the billing settings row for the given tenant key.")
  @Parameter(name = "tenantKey", in = ParameterIn.PATH, required = true,
      description = "8-char alphanumeric tenantKey (e.g. xk7f2b9a)")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Billing settings returned"),
      @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
      @ApiResponse(responseCode = "403", description = "Access denied — PLATFORM_ADMIN required", content = @Content),
      @ApiResponse(responseCode = "404", description = "Billing settings not found", content = @Content)
  })
  public ResponseEntity<BillingSettingsDtos.AdminBillingSettingsResponse> get(
      @Parameter(description = "8-char alphanumeric tenantKey")
      @PathVariable @Pattern(regexp = "[a-z0-9]{8}", message = "tenantKey must be 8 lowercase alphanumeric characters") final String tenantKey) {
    final var settings = billingSettingsService.getByTenantKey(tenantKey);
    return ResponseEntity.ok(BillingSettingsDtoMapper.toAdminResponse(settings));
  }

  @PostMapping
  @Operation(summary = "Create billing settings for tenant",
      description = "Creates the billing settings record for a tenant. Returns 409 if a row already exists.")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "Billing settings created"),
      @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
      @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
      @ApiResponse(responseCode = "403", description = "Access denied — PLATFORM_ADMIN required", content = @Content),
      @ApiResponse(responseCode = "409", description = "Billing settings already exist for this tenant", content = @Content)
  })
  public ResponseEntity<BillingSettingsDtos.AdminBillingSettingsResponse> create(
      @Parameter(description = "8-char alphanumeric tenantKey")
      @PathVariable @Pattern(regexp = "[a-z0-9]{8}", message = "tenantKey must be 8 lowercase alphanumeric characters") final String tenantKey,
      @Valid @RequestBody final BillingSettingsDtos.AdminCreateBillingSettingsRequest request) {
    final var created = billingSettingsService.createForPlatformAdmin(tenantKey, request);
    return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest().build().toUri())
        .body(BillingSettingsDtoMapper.toAdminResponse(created));
  }

  @PutMapping
  @Operation(summary = "Replace billing settings for tenant",
      description = "Replaces all mutable fields on the tenant's billing settings. Optional fields in the body may be null to clear them.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Billing settings replaced"),
      @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
      @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
      @ApiResponse(responseCode = "403", description = "Access denied — PLATFORM_ADMIN required", content = @Content),
      @ApiResponse(responseCode = "404", description = "Billing settings not found", content = @Content)
  })
  public ResponseEntity<BillingSettingsDtos.AdminBillingSettingsResponse> replace(
      @Parameter(description = "8-char alphanumeric tenantKey")
      @PathVariable @Pattern(regexp = "[a-z0-9]{8}", message = "tenantKey must be 8 lowercase alphanumeric characters") final String tenantKey,
      @Valid @RequestBody final BillingSettingsDtos.AdminReplaceBillingSettingsRequest request) {
    final var updated = billingSettingsService.replaceForPlatformAdmin(tenantKey, request);
    return ResponseEntity.ok(BillingSettingsDtoMapper.toAdminResponse(updated));
  }

  @PatchMapping
  @Operation(summary = "Patch billing settings for tenant",
      description = "Partially updates billing settings; only non-null JSON fields are applied.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Billing settings updated"),
      @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
      @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
      @ApiResponse(responseCode = "403", description = "Access denied — PLATFORM_ADMIN required", content = @Content),
      @ApiResponse(responseCode = "404", description = "Billing settings not found", content = @Content)
  })
  public ResponseEntity<BillingSettingsDtos.AdminBillingSettingsResponse> patch(
      @Parameter(description = "8-char alphanumeric tenantKey")
      @PathVariable @Pattern(regexp = "[a-z0-9]{8}", message = "tenantKey must be 8 lowercase alphanumeric characters") final String tenantKey,
      @Valid @RequestBody final BillingSettingsDtos.AdminPatchBillingSettingsRequest request) {
    final var updated = billingSettingsService.patchForPlatformAdmin(tenantKey, request);
    return ResponseEntity.ok(BillingSettingsDtoMapper.toAdminResponse(updated));
  }

  @DeleteMapping
  @Operation(summary = "Delete billing settings for tenant",
      description = "Permanently deletes the billing settings row for the tenant.")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Billing settings deleted"),
      @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
      @ApiResponse(responseCode = "403", description = "Access denied — PLATFORM_ADMIN required", content = @Content),
      @ApiResponse(responseCode = "404", description = "Billing settings not found", content = @Content)
  })
  public ResponseEntity<Void> delete(
      @Parameter(description = "8-char alphanumeric tenantKey")
      @PathVariable @Pattern(regexp = "[a-z0-9]{8}", message = "tenantKey must be 8 lowercase alphanumeric characters") final String tenantKey) {
    billingSettingsService.deleteForPlatformAdmin(tenantKey);
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }
}
