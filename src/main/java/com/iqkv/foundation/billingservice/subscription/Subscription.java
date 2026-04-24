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
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain class representing a cached payment gateway subscription record.
 * Acts as a local cache of subscription state — readable without a payment gateway round-trip.
 *
 * <p>One tenant can have multiple subscription rows (plan upgrades, add-ons).
 * Populated and updated by {@code PaymentWebhookRestResource} on subscription webhook events.
 */
public class Subscription {

  private UUID id;
  private String tenantKey;
  private String externalSubscriptionId;  // payment-gateway-agnostic (e.g. Stripe sub_xxx)
  private String externalCustomerId;      // payment-gateway-agnostic (e.g. Stripe cus_xxx)
  private String status;                  // active | past_due | canceled | unpaid | trialing
  private String planId;                  // nullable — payment gateway price/plan reference
  private Instant currentPeriodStart;
  private Instant currentPeriodEnd;
  private boolean cancelAtPeriodEnd;
  private Instant canceledAt;             // nullable
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public Subscription() {
  }

  public Subscription(UUID id,
                      String tenantKey,
                      String externalSubscriptionId,
                      String externalCustomerId,
                      String status,
                      String planId,
                      Instant currentPeriodStart,
                      Instant currentPeriodEnd,
                      boolean cancelAtPeriodEnd,
                      Instant canceledAt,
                      LocalDateTime createdAt,
                      LocalDateTime updatedAt) {
    this.id = id;
    this.tenantKey = tenantKey;
    this.externalSubscriptionId = externalSubscriptionId;
    this.externalCustomerId = externalCustomerId;
    this.status = status;
    this.planId = planId;
    this.currentPeriodStart = currentPeriodStart;
    this.currentPeriodEnd = currentPeriodEnd;
    this.cancelAtPeriodEnd = cancelAtPeriodEnd;
    this.canceledAt = canceledAt;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getTenantKey() {
    return tenantKey;
  }

  public void setTenantKey(String tenantKey) {
    this.tenantKey = tenantKey;
  }

  public String getExternalSubscriptionId() {
    return externalSubscriptionId;
  }

  public void setExternalSubscriptionId(String externalSubscriptionId) {
    this.externalSubscriptionId = externalSubscriptionId;
  }

  public String getExternalCustomerId() {
    return externalCustomerId;
  }

  public void setExternalCustomerId(String externalCustomerId) {
    this.externalCustomerId = externalCustomerId;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getPlanId() {
    return planId;
  }

  public void setPlanId(String planId) {
    this.planId = planId;
  }

  public Instant getCurrentPeriodStart() {
    return currentPeriodStart;
  }

  public void setCurrentPeriodStart(Instant currentPeriodStart) {
    this.currentPeriodStart = currentPeriodStart;
  }

  public Instant getCurrentPeriodEnd() {
    return currentPeriodEnd;
  }

  public void setCurrentPeriodEnd(Instant currentPeriodEnd) {
    this.currentPeriodEnd = currentPeriodEnd;
  }

  public boolean isCancelAtPeriodEnd() {
    return cancelAtPeriodEnd;
  }

  public void setCancelAtPeriodEnd(boolean cancelAtPeriodEnd) {
    this.cancelAtPeriodEnd = cancelAtPeriodEnd;
  }

  public Instant getCanceledAt() {
    return canceledAt;
  }

  public void setCanceledAt(Instant canceledAt) {
    this.canceledAt = canceledAt;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }
}
