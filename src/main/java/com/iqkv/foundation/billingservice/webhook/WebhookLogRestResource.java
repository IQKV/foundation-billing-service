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

package com.iqkv.foundation.billingservice.webhook;

import java.util.UUID;

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
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/billing/webhook-logs")
@Tag(name = "Webhook Logs", description = "Tenant owner webhook log access — requires TENANT_OWNER authority")
@SecurityRequirement(name = "bearerAuth")
@Validated
public class WebhookLogRestResource {

  private final WebhookLogService webhookLogService;

  public WebhookLogRestResource(final WebhookLogService webhookLogService) {
    this.webhookLogService = webhookLogService;
  }

  @GetMapping("/me")
  @PreAuthorize("hasAnyAuthority('TENANT_OWNER', 'ADMIN', 'MEMBER')")
  @Operation(
      summary = "List webhook logs for current subject",
      description = "Returns a paginated, sorted, and optionally filtered list of webhook logs "
                    + "for the current subject (tenant in multi-tenant mode, user in single-tenant mode). "
                    + "Requires TENANT_OWNER, ADMIN, or MEMBER authority.")
  @Parameter(name = "X-Tenant-ID", in = ParameterIn.HEADER, required = true,
             description = "8-char alphanumeric tenantKey (e.g. xk7f2b9a)")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Page of webhook logs returned"),
      @ApiResponse(responseCode = "400", description = "Invalid query parameters", content = @Content),
      @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
      @ApiResponse(responseCode = "403", description = "Access denied — TENANT_OWNER/ADMIN/MEMBER required", content = @Content)
  })
  public ResponseEntity<WebhookLogDtos.PagedWebhookLogResponse> listWebhookLogsForMe(
      @ModelAttribute @Validated WebhookLogDtos.WebhookLogListQuery query,
      @AuthenticationPrincipal Jwt jwt) {
    final String tenantKey = jwt.getClaimAsString(JwtClaimNames.TENANT_ID);
    final UUID userId = UUID.fromString(jwt.getClaimAsString(JwtClaimNames.USER_ID));
    return ResponseEntity.ok(webhookLogService.listWebhookLogsForSubject(query, tenantKey, userId));
  }

  @GetMapping("/me/{id}")
  @PreAuthorize("hasAnyAuthority('TENANT_OWNER', 'ADMIN', 'MEMBER')")
  @Operation(
      summary = "Get webhook log by ID for current subject",
      description = "Returns a single webhook log for the current subject (tenant in multi-tenant mode, "
                    + "user in single-tenant mode). Requires TENANT_OWNER, ADMIN, or MEMBER authority.")
  @Parameter(name = "X-Tenant-ID", in = ParameterIn.HEADER, required = true,
             description = "8-char alphanumeric tenantKey (e.g. xk7f2b9a)")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Webhook log found"),
      @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
      @ApiResponse(responseCode = "403", description = "Access denied — TENANT_OWNER/ADMIN/MEMBER required", content = @Content),
      @ApiResponse(responseCode = "404", description = "Webhook log not found or not accessible to current subject", content = @Content)
  })
  public ResponseEntity<WebhookLogDtos.AdminWebhookLogResponse> getWebhookLogForMe(
      @Parameter(description = "Webhook log UUID") @PathVariable UUID id,
      @AuthenticationPrincipal Jwt jwt) {
    final String tenantKey = jwt.getClaimAsString(JwtClaimNames.TENANT_ID);
    final UUID userId = UUID.fromString(jwt.getClaimAsString(JwtClaimNames.USER_ID));
    return ResponseEntity.ok(webhookLogService.getByIdForSubject(id, tenantKey, userId));
  }
}
