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

package com.iqkv.foundation.billingservice.userbilling;

import jakarta.validation.Valid;
import java.util.UUID;

import com.iqkv.foundation.billingservice.infrastructure.security.JwtClaimNames;
import com.iqkv.foundation.billingservice.settings.BillingSettingsDtos;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * REST resource for individual user billing settings (single-tenant mode).
 *
 * <p>Only active when {@code iqkv.platform.rollout-mode=SINGLE_TENANT}.
 */
@RestController
@RequestMapping("/api/v1/billing/user-settings")
@Tag(name = "User Billing Settings", description = "Individual user billing configuration management")
@SecurityRequirement(name = "bearerAuth")
@ConditionalOnProperty(name = "iqkv.platform.rollout-mode", havingValue = "SINGLE_TENANT")
public class UserBillingSettingsRestResource {

  private final UserBillingSettingsService userBillingSettingsService;

  public UserBillingSettingsRestResource(final UserBillingSettingsService userBillingSettingsService) {
    this.userBillingSettingsService = userBillingSettingsService;
  }

  @GetMapping
  @Operation(
      summary = "Get billing settings",
      description = "Returns billing settings for the authenticated user. Only active in SINGLE_TENANT mode.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Billing settings returned"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "404", description = "User billing settings not found")
  })
  public ResponseEntity<BillingSettingsDtos.BillingSettingsResponse> getSettings(
      @AuthenticationPrincipal final Jwt jwt) {
    final UUID userId = UUID.fromString(jwt.getClaimAsString(JwtClaimNames.USER_ID));
    final UserBillingSettings settings = userBillingSettingsService.getByUserId(userId);
    return ResponseEntity.ok(UserBillingSettingsDtoMapper.toResponse(settings));
  }

  @PostMapping
  @Operation(
      summary = "Create billing settings",
      description = "Creates billing settings for the authenticated user. Billing email and currency are required. "
                    + "Returns 409 if settings already exist. Only active in SINGLE_TENANT mode.")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "Billing settings created"),
      @ApiResponse(responseCode = "400", description = "Validation error"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "409", description = "Billing settings already exist for this user")
  })
  public ResponseEntity<BillingSettingsDtos.BillingSettingsResponse> createSettings(
      @Valid @RequestBody final BillingSettingsDtos.CreateBillingSettingsRequest request,
      @AuthenticationPrincipal final Jwt jwt) {
    final UUID userId = UUID.fromString(jwt.getClaimAsString(JwtClaimNames.USER_ID));
    final UserBillingSettings created = userBillingSettingsService.createForUser(userId, request);
    return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest().build().toUri())
        .body(UserBillingSettingsDtoMapper.toResponse(created));
  }

  @PatchMapping
  @Operation(
      summary = "Update billing settings",
      description = "Partially updates billing settings. Only non-null fields are applied. "
                    + "Only active in SINGLE_TENANT mode.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Billing settings updated"),
      @ApiResponse(responseCode = "400", description = "Validation error"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "404", description = "User billing settings not found")
  })
  public ResponseEntity<BillingSettingsDtos.BillingSettingsResponse> updateSettings(
      @Valid @RequestBody final BillingSettingsDtos.UpdateBillingSettingsRequest request,
      @AuthenticationPrincipal final Jwt jwt) {
    final UUID userId = UUID.fromString(jwt.getClaimAsString(JwtClaimNames.USER_ID));
    final UserBillingSettings settings = userBillingSettingsService.updateForUser(userId, request);
    return ResponseEntity.ok(UserBillingSettingsDtoMapper.toResponse(settings));
  }

  @PostMapping("/portal")
  @Operation(
      summary = "Create customer portal session",
      description = "Creates a Stripe Customer Portal session for the authenticated user and returns the URL for redirection. "
                    + "Only active in SINGLE_TENANT mode.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Portal session created"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "404", description = "User billing settings or customer ID not found")
  })
  public ResponseEntity<BillingSettingsDtos.PortalSessionResponse> createPortalSession(
      @AuthenticationPrincipal final Jwt jwt) {
    final UUID userId = UUID.fromString(jwt.getClaimAsString(JwtClaimNames.USER_ID));
    final String url = userBillingSettingsService.createPortalSession(userId);
    return ResponseEntity.ok(new BillingSettingsDtos.PortalSessionResponse(url));
  }
}
