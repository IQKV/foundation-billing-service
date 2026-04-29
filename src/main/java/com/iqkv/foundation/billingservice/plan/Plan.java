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

package com.iqkv.foundation.billingservice.plan;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain class representing a pre-provisioned subscription plan in the plan catalog.
 *
 * <p>{@code scope} determines whether the plan is applicable to a tenant ({@code TENANT})
 * or a user ({@code USER}), matching the active rollout mode's subject scope.
 *
 * <p>{@code featureSet} is a JSON string describing the features included in the plan.
 * {@code priceMinor} is the price in the smallest currency unit (e.g. cents for USD).
 */
public class Plan {

  private UUID id;
  private String planCode;        // unique identifier for the plan (e.g. "pro-monthly")
  private String displayName;
  private String billingPeriod;   // MONTHLY | ANNUAL
  private Integer priceMinor;     // price in cents (or smallest currency unit)
  private String currency;        // default "USD"
  private String featureSet;      // JSON string of feature flags/limits
  private String scope;           // TENANT | USER
  private Boolean active;         // default true
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public Plan() {
  }

  public Plan(final UUID id,
              final String planCode,
              final String displayName,
              final String billingPeriod,
              final Integer priceMinor,
              final String currency,
              final String featureSet,
              final String scope,
              final Boolean active,
              final LocalDateTime createdAt,
              final LocalDateTime updatedAt) {
    this.id = id;
    this.planCode = planCode;
    this.displayName = displayName;
    this.billingPeriod = billingPeriod;
    this.priceMinor = priceMinor;
    this.currency = currency;
    this.featureSet = featureSet;
    this.scope = scope;
    this.active = active;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getPlanCode() {
    return planCode;
  }

  public void setPlanCode(String planCode) {
    this.planCode = planCode;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getBillingPeriod() {
    return billingPeriod;
  }

  public void setBillingPeriod(String billingPeriod) {
    this.billingPeriod = billingPeriod;
  }

  public Integer getPriceMinor() {
    return priceMinor;
  }

  public void setPriceMinor(Integer priceMinor) {
    this.priceMinor = priceMinor;
  }

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }

  public String getFeatureSet() {
    return featureSet;
  }

  public void setFeatureSet(String featureSet) {
    this.featureSet = featureSet;
  }

  public String getScope() {
    return scope;
  }

  public void setScope(String scope) {
    this.scope = scope;
  }

  public Boolean getActive() {
    return active;
  }

  public void setActive(Boolean active) {
    this.active = active;
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
