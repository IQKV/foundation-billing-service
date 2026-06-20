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
 * <p>One tenant (or user in single-tenant mode) can have multiple subscription rows
 * (plan upgrades, add-ons). Populated and updated by {@code PaymentWebhookRestResource}
 * on subscription webhook events.
 *
 * <p>{@code subjectType} and {@code subjectKey} identify the subscription owner:
 * {@code TENANT}/{@code tenantKey} in multi-tenant mode, or {@code USER}/{@code userId}
 * in single-tenant mode.
 */
public class Subscription {

  private UUID id;
  private String tenantKey;
  private String externalSubscriptionId;  // payment-gateway-agnostic (e.g. Stripe sub_xxx)
  private String externalCustomerId;      // payment-gateway-agnostic (e.g. Stripe cus_xxx)
  private String status;                  // active | past_due | canceled | unpaid | trialing
  private String planId;                  // nullable — payment gateway price/plan reference
  private Long quantity;                  // nullable — number of seats/units
  private Instant trialStart;             // nullable
  private Instant trialEnd;               // nullable
  private Instant currentPeriodStart;
  private Instant currentPeriodEnd;
  private boolean cancelAtPeriodEnd;
  private Instant canceledAt;             // nullable
  private String subjectType;             // TENANT | USER
  private String subjectKey;              // tenantKey or userId depending on subjectType
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public Subscription() {
  }

  public Subscription(final UUID id,
                      final String tenantKey,
                      final String externalSubscriptionId,
                      final String externalCustomerId,
                      final String status,
                      final String planId,
                      final Long quantity,
                      final Instant trialStart,
                      final Instant trialEnd,
                      final Instant currentPeriodStart,
                      final Instant currentPeriodEnd,
                      final boolean cancelAtPeriodEnd,
                      final Instant canceledAt,
                      final String subjectType,
                      final String subjectKey,
                      final LocalDateTime createdAt,
                      final LocalDateTime updatedAt) {
    this.id = id;
    this.tenantKey = tenantKey;
    this.externalSubscriptionId = externalSubscriptionId;
    this.externalCustomerId = externalCustomerId;
    this.status = status;
    this.planId = planId;
    this.quantity = quantity;
    this.trialStart = trialStart;
    this.trialEnd = trialEnd;
    this.currentPeriodStart = currentPeriodStart;
    this.currentPeriodEnd = currentPeriodEnd;
    this.cancelAtPeriodEnd = cancelAtPeriodEnd;
    this.canceledAt = canceledAt;
    this.subjectType = subjectType;
    this.subjectKey = subjectKey;
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

  public Long getQuantity() {
    return quantity;
  }

  public void setQuantity(Long quantity) {
    this.quantity = quantity;
  }

  public Instant getTrialStart() {
    return trialStart;
  }

  public void setTrialStart(Instant trialStart) {
    this.trialStart = trialStart;
  }

  public Instant getTrialEnd() {
    return trialEnd;
  }

  public void setTrialEnd(Instant trialEnd) {
    this.trialEnd = trialEnd;
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

  public String getSubjectType() {
    return subjectType;
  }

  public void setSubjectType(String subjectType) {
    this.subjectType = subjectType;
  }

  public String getSubjectKey() {
    return subjectKey;
  }

  public void setSubjectKey(String subjectKey) {
    this.subjectKey = subjectKey;
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

  /**
   * Returns true if the subscription is currently in a trial period.
   */
  public boolean isInTrial() {
    if (trialStart == null || trialEnd == null) {
      return false;
    }
    final var now = Instant.now();
    return now.isAfter(trialStart) && now.isBefore(trialEnd);
  }
}
