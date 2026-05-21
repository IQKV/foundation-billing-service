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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/billing/admin/subscriptions")
@Tag(name = "Subscription Admin", description = "Platform operator CRUD operations for subscriptions — requires PLATFORM_ADMIN authority")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
@Validated
public class SubscriptionAdminRestResource {

  private final SubscriptionService subscriptionService;

  public SubscriptionAdminRestResource(final SubscriptionService subscriptionService) {
    this.subscriptionService = subscriptionService;
  }

  @GetMapping
  @Operation(summary = "List subscriptions", description = "Returns a paginated, sorted, and optionally filtered list of subscriptions across all tenants.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Page of subscriptions returned"),
      @ApiResponse(responseCode = "400", description = "Invalid query parameters", content = @Content),
      @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
      @ApiResponse(responseCode = "403", description = "Access denied — PLATFORM_ADMIN required", content = @Content)
  })
  public ResponseEntity<SubscriptionDtos.PagedSubscriptionResponse> listSubscriptions(
      @ModelAttribute @Valid SubscriptionDtos.SubscriptionListQuery query) {
    return ResponseEntity.ok(subscriptionService.listSubscriptions(query));
  }

  @GetMapping("/count")
  @Operation(summary = "Count subscriptions", description = "Returns the total number of subscription records across all tenants.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Total subscription count returned",
                   content = @Content(schema = @io.swagger.v3.oas.annotations.media.Schema(
                       implementation = SubscriptionDtos.SubscriptionCountResponse.class))),
      @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
      @ApiResponse(responseCode = "403", description = "Access denied — PLATFORM_ADMIN required", content = @Content)
  })
  public ResponseEntity<SubscriptionDtos.SubscriptionCountResponse> countSubscriptions() {
    return ResponseEntity.ok(subscriptionService.countSubscriptions());
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get subscription by ID")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Subscription found"),
      @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
      @ApiResponse(responseCode = "403", description = "Access denied — PLATFORM_ADMIN required", content = @Content),
      @ApiResponse(responseCode = "404", description = "Subscription not found", content = @Content)
  })
  public ResponseEntity<SubscriptionDtos.AdminSubscriptionResponse> getSubscription(
      @Parameter(description = "Subscription UUID") @PathVariable UUID id) {
    return ResponseEntity.ok(subscriptionService.getSubscriptionById(id));
  }

  @PatchMapping("/{id}")
  @Operation(summary = "Partially update subscription", description = "Updates only the provided fields. Omitted fields are left unchanged.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Subscription patched"),
      @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
      @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
      @ApiResponse(responseCode = "403", description = "Access denied — PLATFORM_ADMIN required", content = @Content),
      @ApiResponse(responseCode = "404", description = "Subscription not found", content = @Content)
  })
  public ResponseEntity<SubscriptionDtos.AdminSubscriptionResponse> patchSubscription(
      @Parameter(description = "Subscription UUID") @PathVariable UUID id,
      @RequestBody SubscriptionDtos.AdminUpdateSubscriptionRequest request) {
    return ResponseEntity.ok(subscriptionService.patchSubscription(id, request));
  }

  @PostMapping("/{id}/cancel")
  @Operation(summary = "Cancel subscription", description = "Cancels the subscription via the payment gateway.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Subscription canceled"),
      @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
      @ApiResponse(responseCode = "403", description = "Access denied — PLATFORM_ADMIN required", content = @Content),
      @ApiResponse(responseCode = "404", description = "Subscription not found", content = @Content)
  })
  public ResponseEntity<SubscriptionDtos.AdminSubscriptionResponse> cancelSubscription(
      @Parameter(description = "Subscription UUID") @PathVariable UUID id,
      @org.springframework.web.bind.annotation.RequestParam(defaultValue = "true") boolean cancelAtPeriodEnd) {
    return ResponseEntity.ok(subscriptionService.cancelSubscription(id, cancelAtPeriodEnd));
  }

  @PostMapping("/{id}/pause")
  @Operation(summary = "Pause subscription", description = "Pauses the subscription via the payment gateway.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Subscription paused"),
      @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
      @ApiResponse(responseCode = "403", description = "Access denied — PLATFORM_ADMIN required", content = @Content),
      @ApiResponse(responseCode = "404", description = "Subscription not found", content = @Content)
  })
  public ResponseEntity<SubscriptionDtos.AdminSubscriptionResponse> pauseSubscription(
      @Parameter(description = "Subscription UUID") @PathVariable UUID id) {
    return ResponseEntity.ok(subscriptionService.pauseSubscription(id));
  }

  @PostMapping("/{id}/reactivate")
  @Operation(summary = "Reactivate subscription", description = "Reactivates a paused subscription via the payment gateway.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Subscription reactivated"),
      @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
      @ApiResponse(responseCode = "403", description = "Access denied — PLATFORM_ADMIN required", content = @Content),
      @ApiResponse(responseCode = "404", description = "Subscription not found", content = @Content)
  })
  public ResponseEntity<SubscriptionDtos.AdminSubscriptionResponse> reactivateSubscription(
      @Parameter(description = "Subscription UUID") @PathVariable UUID id) {
    return ResponseEntity.ok(subscriptionService.reactivateSubscription(id));
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Delete subscription", description = "Permanently deletes the subscription record.")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Subscription deleted"),
      @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
      @ApiResponse(responseCode = "403", description = "Access denied — PLATFORM_ADMIN required", content = @Content),
      @ApiResponse(responseCode = "404", description = "Subscription not found", content = @Content)
  })
  public ResponseEntity<Void> deleteSubscription(
      @Parameter(description = "Subscription UUID") @PathVariable UUID id) {
    subscriptionService.deleteSubscription(id);
    return ResponseEntity.noContent().build();
  }
}
