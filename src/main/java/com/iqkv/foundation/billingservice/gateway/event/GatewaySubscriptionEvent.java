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
import java.util.Map;

/**
 * Gateway-agnostic subscription lifecycle event.
 *
 * <p>Represents subscription creation, updates, and cancellations from any payment gateway.
 */
public record GatewaySubscriptionEvent(
    String eventId,
    String eventType,
    Instant occurredAt,
    String gatewayType,
    String externalSubscriptionId,
    String externalCustomerId,
    String status,
    String planId,
    Long quantity,
    Instant trialStart,
    Instant trialEnd,
    Instant currentPeriodStart,
    Instant currentPeriodEnd,
    boolean cancelAtPeriodEnd,
    Instant canceledAt,
    Map<String, String> metadata
) implements GatewayWebhookEvent {

  /**
   * Determines if this is a subscription creation event.
   */
  public boolean isCreated() {
    return eventType.contains("created");
  }

  /**
   * Determines if this is a subscription update event.
   */
  public boolean isUpdated() {
    return eventType.contains("updated");
  }

  /**
   * Determines if this is a subscription deletion/cancellation event.
   */
  public boolean isDeleted() {
    return eventType.contains("deleted") || eventType.contains("canceled");
  }
}
