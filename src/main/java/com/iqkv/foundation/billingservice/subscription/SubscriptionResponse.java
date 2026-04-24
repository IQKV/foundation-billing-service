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

import com.iqkv.foundation.billingservice.subscription.Subscription;

/**
 * Read-only view of a cached subscription record.
 */
public class SubscriptionResponse {

  private UUID id;
  private String tenantKey;
  private String externalSubscriptionId;
  private String status;
  private String planId;
  private Instant currentPeriodStart;
  private Instant currentPeriodEnd;
  private boolean cancelAtPeriodEnd;
  private Instant canceledAt;

  public static SubscriptionResponse from(Subscription subscription) {
    final var response = new SubscriptionResponse();
    response.id = subscription.getId();
    response.tenantKey = subscription.getTenantKey();
    response.externalSubscriptionId = subscription.getExternalSubscriptionId();
    response.status = subscription.getStatus();
    response.planId = subscription.getPlanId();
    response.currentPeriodStart = subscription.getCurrentPeriodStart();
    response.currentPeriodEnd = subscription.getCurrentPeriodEnd();
    response.cancelAtPeriodEnd = subscription.isCancelAtPeriodEnd();
    response.canceledAt = subscription.getCanceledAt();
    return response;
  }

  public UUID getId() {
    return id;
  }

  public String getTenantKey() {
    return tenantKey;
  }

  public String getExternalSubscriptionId() {
    return externalSubscriptionId;
  }

  public String getStatus() {
    return status;
  }

  public String getPlanId() {
    return planId;
  }

  public Instant getCurrentPeriodStart() {
    return currentPeriodStart;
  }

  public Instant getCurrentPeriodEnd() {
    return currentPeriodEnd;
  }

  public boolean isCancelAtPeriodEnd() {
    return cancelAtPeriodEnd;
  }

  public Instant getCanceledAt() {
    return canceledAt;
  }
}
