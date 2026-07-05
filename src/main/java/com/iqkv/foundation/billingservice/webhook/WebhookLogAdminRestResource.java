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

package com.iqkv.foundation.billingservice.webhook;

import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/billing/admin/webhook-logs")
@Tag(name = "Webhook Log Admin", description = "Platform operator read-only access to webhook logs — requires PLATFORM_ADMIN authority")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
@Validated
public class WebhookLogAdminRestResource {

  private final WebhookLogService webhookLogService;

  public WebhookLogAdminRestResource(final WebhookLogService webhookLogService) {
    this.webhookLogService = webhookLogService;
  }

  @GetMapping
  @Operation(summary = "List webhook logs", description = "Returns a paginated, sorted, and optionally filtered list of webhook logs across all tenants.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Page of webhook logs returned"),
      @ApiResponse(responseCode = "400", description = "Invalid query parameters", content = @Content),
      @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
      @ApiResponse(responseCode = "403", description = "Access denied — PLATFORM_ADMIN required", content = @Content)
  })
  public ResponseEntity<WebhookLogDtos.PagedWebhookLogResponse> listWebhookLogs(
      @ModelAttribute @Validated WebhookLogDtos.WebhookLogListQuery query) {
    return ResponseEntity.ok(webhookLogService.listWebhookLogs(query));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get webhook log by ID")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Webhook log found"),
      @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
      @ApiResponse(responseCode = "403", description = "Access denied — PLATFORM_ADMIN required", content = @Content),
      @ApiResponse(responseCode = "404", description = "Webhook log not found", content = @Content)
  })
  public ResponseEntity<WebhookLogDtos.AdminWebhookLogResponse> getWebhookLog(
      @Parameter(description = "Webhook log UUID") @PathVariable UUID id) {
    return ResponseEntity.ok(webhookLogService.getById(id));
  }
}
