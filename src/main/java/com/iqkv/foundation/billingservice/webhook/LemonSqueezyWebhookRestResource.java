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

import com.iqkv.foundation.billingservice.gateway.GatewayType;
import com.iqkv.foundation.billingservice.gateway.adapter.lemonsqueezy.LemonSqueezyGatewayAdapter;
import com.iqkv.foundation.billingservice.infrastructure.config.ConditionalOnGateway;
import com.iqkv.foundation.billingservice.shared.exception.WebhookProcessingException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/billing/webhooks")
@ConditionalOnGateway(GatewayType.LEMON_SQUEEZY)
public class LemonSqueezyWebhookRestResource {

  private static final Logger log = LoggerFactory.getLogger(LemonSqueezyWebhookRestResource.class);

  private final LemonSqueezyGatewayAdapter lsAdapter;
  private final WebhookProcessingService webhookProcessingService;

  public LemonSqueezyWebhookRestResource(
      final LemonSqueezyGatewayAdapter lsAdapter,
      final WebhookProcessingService webhookProcessingService) {
    this.lsAdapter = lsAdapter;
    this.webhookProcessingService = webhookProcessingService;
  }

  @PostMapping("/lemon-squeezy")
  @Operation(summary = "Receive Lemon Squeezy webhook",
      description = "Verifies X-Signature header and processes the event idempotently.")
  @Parameter(name = "X-Signature", in = ParameterIn.HEADER, required = true,
      description = "HMAC-SHA256 hex digest of the raw payload body")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Event received and processed"),
      @ApiResponse(responseCode = "400", description = "Invalid signature")
  })
  public ResponseEntity<Void> handleLemonSqueezyWebhook(
      @RequestBody final String payload,
      @RequestHeader("X-Signature") final String signature) {

    try {
      lsAdapter.verifyAndParseWebhookEvent(payload, signature)
          .ifPresent(webhookProcessingService::process);
    } catch (final WebhookProcessingException e) {
      log.warn("Rejected Lemon Squeezy webhook — {}", e.getMessage());
      return ResponseEntity.badRequest().build();
    }
    return ResponseEntity.ok().build();
  }
}
