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

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoint for inbound Stripe webhook events.
 *
 * <p>Verifies the Stripe signature, then delegates to {@link WebhookProcessingService}
 * for idempotent processing. Always returns HTTP 200 after the idempotency check to
 * prevent Stripe from retrying on business logic failures.
 */
@RestController
@RequestMapping("/api/v1/billing/webhooks")
public class StripeWebhookRestResource {

  private static final Logger log = LoggerFactory.getLogger(StripeWebhookRestResource.class);

  private final WebhookProcessingService webhookProcessingService;

  @Value("${iqkv.stripe.webhook-secret}")
  private String webhookSecret;

  public StripeWebhookRestResource(WebhookProcessingService webhookProcessingService) {
    this.webhookProcessingService = webhookProcessingService;
  }

  @PostMapping("/stripe")
  public ResponseEntity<Void> handleStripeWebhook(
      @RequestBody String payload,
      @RequestHeader("Stripe-Signature") String sigHeader) {

    final Event event;
    try {
      event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
    } catch (SignatureVerificationException e) {
      log.warn("Invalid Stripe webhook signature: {}", e.getMessage());
      return ResponseEntity.badRequest().build();
    }

    webhookProcessingService.process(event);
    return ResponseEntity.ok().build();
  }
}
