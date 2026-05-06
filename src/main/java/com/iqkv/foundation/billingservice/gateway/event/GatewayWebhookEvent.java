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

package com.iqkv.foundation.billingservice.gateway.event;

import java.time.Instant;

/**
 * Gateway-agnostic webhook event interface.
 *
 * <p>All payment gateway webhook events are normalized to this interface,
 * allowing business logic to remain independent of specific gateway implementations.
 *
 * <p>Sealed interface with three permitted subtypes:
 * <ul>
 *   <li>{@link GatewaySubscriptionEvent} - subscription lifecycle events</li>
 *   <li>{@link GatewayInvoiceEvent} - invoice payment events</li>
 *   <li>{@link GatewayPaymentFailureEvent} - payment failure events</li>
 * </ul>
 */
public sealed interface GatewayWebhookEvent
    permits GatewaySubscriptionEvent, GatewayInvoiceEvent, GatewayPaymentFailureEvent {

  /**
   * Unique event identifier from the payment gateway.
   */
  String eventId();

  /**
   * Gateway-specific event type (e.g., "customer.subscription.created").
   */
  String eventType();

  /**
   * Timestamp when the event occurred.
   */
  Instant occurredAt();
}
