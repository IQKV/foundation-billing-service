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

package com.iqkv.foundation.billingservice.subscription;

import jakarta.validation.Valid;
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

/**
 * REST resource for administrative refund operations.
 */
@RestController
@RequestMapping("/api/v1/billing/admin/refunds")
@Tag(name = "Refund Admin", description = "Platform operator operations for refunds — requires PLATFORM_ADMIN authority")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
@Validated
public class RefundAdminRestResource {

  private final SubscriptionService subscriptionService;

  public RefundAdminRestResource(final SubscriptionService subscriptionService) {
    this.subscriptionService = subscriptionService;
  }

  @GetMapping
  @Operation(summary = "List refunds", description = "Returns a paginated, sorted, and optionally filtered list of refunds across all tenants.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Page of refunds returned"),
      @ApiResponse(responseCode = "400", description = "Invalid query parameters", content = @Content),
      @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
      @ApiResponse(responseCode = "403", description = "Access denied — PLATFORM_ADMIN required", content = @Content)
  })
  public ResponseEntity<SubscriptionDtos.PagedRefundResponse> listRefunds(
      @ModelAttribute @Valid SubscriptionDtos.RefundListQuery query) {
    return ResponseEntity.ok(subscriptionService.listRefunds(query));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get refund by ID")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Refund found"),
      @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
      @ApiResponse(responseCode = "403", description = "Access denied — PLATFORM_ADMIN required", content = @Content),
      @ApiResponse(responseCode = "404", description = "Refund not found", content = @Content)
  })
  public ResponseEntity<SubscriptionDtos.AdminRefundResponse> getRefund(
      @Parameter(description = "Refund UUID") @PathVariable UUID id) {
    return ResponseEntity.ok(subscriptionService.getRefundById(id));
  }
}
