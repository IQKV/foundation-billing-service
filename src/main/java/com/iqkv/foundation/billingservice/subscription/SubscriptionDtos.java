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

import java.time.Instant;
import java.util.UUID;

/**
 * DTOs for the subscription API surface.
 * All types are immutable records; internal fields (e.g. {@code externalCustomerId}) are excluded.
 */
public final class SubscriptionDtos {

  private SubscriptionDtos() {}

  /**
   * Read-only view of a cached subscription record.
   * Excludes internal fields (e.g. {@code externalCustomerId}) from the API surface.
   */
  public record SubscriptionResponse(
      UUID id,
      String tenantKey,
      String externalSubscriptionId,
      String status,
      String planId,
      Instant currentPeriodStart,
      Instant currentPeriodEnd,
      boolean cancelAtPeriodEnd,
      Instant canceledAt
  ) {}
}
