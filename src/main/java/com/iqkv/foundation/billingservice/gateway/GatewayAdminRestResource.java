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

package com.iqkv.foundation.billingservice.gateway;

import com.iqkv.foundation.billingservice.infrastructure.config.PaymentGatewayConfigurationProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Platform-operator read-only view of the active payment gateway configuration.
 *
 * <p>Exposes the deploy-time gateway selection ({@code iqkv.payment.gateway.type}) so that
 * platform admins and the admin UI can display the active gateway without relying on
 * environment-variable inspection or log scraping.
 */
@RestController
@RequestMapping("/api/v1/billing/admin/gateway")
@Tag(name = "Gateway Admin", description = "Platform operator read-only view of active payment gateway configuration — requires PLATFORM_ADMIN authority")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
public class GatewayAdminRestResource {

  private final PaymentGatewayConfigurationProperties gatewayConfig;

  public GatewayAdminRestResource(final PaymentGatewayConfigurationProperties gatewayConfig) {
    this.gatewayConfig = gatewayConfig;
  }

  /**
   * Active gateway configuration response.
   *
   * @param activeGateway the deploy-time gateway type (e.g. {@code STRIPE} or {@code LEMON_SQUEEZY})
   */
  public record GatewayConfigResponse(
      @Schema(description = "Active payment gateway for this deployment", example = "STRIPE",
              allowableValues = {"STRIPE", "LEMON_SQUEEZY"})
      String activeGateway
  ) {
  }

  @GetMapping
  @Operation(
      summary = "Get active payment gateway",
      description = "Returns the payment gateway type active for this deployment "
                    + "(iqkv.payment.gateway.type). Deploy-time constant — does not change at runtime.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Active gateway configuration returned",
                   content = @Content(schema = @Schema(implementation = GatewayConfigResponse.class))),
      @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
      @ApiResponse(responseCode = "403", description = "Access denied — PLATFORM_ADMIN required", content = @Content)
  })
  public ResponseEntity<GatewayConfigResponse> getActiveGateway() {
    return ResponseEntity.ok(new GatewayConfigResponse(gatewayConfig.type().name()));
  }
}
