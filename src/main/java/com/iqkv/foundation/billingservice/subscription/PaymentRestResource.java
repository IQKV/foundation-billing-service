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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST resource for payment operations (e.g. refunds).
 */
@RestController
@RequestMapping("/api/v1/billing/payments")
@Tag(name = "Payments", description = "Payment operations")
@SecurityRequirement(name = "bearerAuth")
@Validated
public class PaymentRestResource {

  private final SubscriptionService subscriptionService;

  public PaymentRestResource(final SubscriptionService subscriptionService) {
    this.subscriptionService = subscriptionService;
  }

  @PostMapping("/refund")
  @PreAuthorize("hasAuthority('TENANT_OWNER')")
  @Operation(
      summary = "Create refund",
      description = "Creates a refund for a payment. Requires TENANT_OWNER authority.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Refund created"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Access denied")
  })
  public ResponseEntity<SubscriptionDtos.RefundResponse> createRefund(
      @RequestBody final SubscriptionDtos.CreateRefundRequest request) {
    return ResponseEntity.ok(subscriptionService.createRefund(request));
  }
}
