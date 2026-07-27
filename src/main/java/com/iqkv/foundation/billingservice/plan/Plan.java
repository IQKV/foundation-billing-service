/*
 * Copyright 2026 iQKV Foundation Team.
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

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Domain class representing a pre-provisioned subscription plan in the plan catalog.
 *
 * <p>{@code scope} determines whether the plan is applicable to a tenant ({@code TENANT})
 * or a user ({@code USER}), matching the active rollout mode's subject scope.
 *
 * <p>{@code entitlement} is a JSON string describing the entitlement included in the plan.
 * {@code priceMinor} is the price in the smallest currency unit (e.g. cents for USD).
 */
public class Plan {

  private UUID id;
  private String planCode;        // unique identifier for the plan (e.g. "pro-monthly")
  private String displayName;
  private String description;     // plan description for checkout, portal, quotes
  private String billingPeriod;   // MONTHLY | ANNUAL
  private Integer priceMinor;     // price in cents (or smallest currency unit)
  private String currency;        // default "USD"
  private String entitlement;      // JSON string of feature flags/limits
  private String scope;           // TENANT | USER
  private String externalProductId;
  private String externalPriceId;
  private Boolean active;         // default true
  private Integer trialPeriodDays; // 0 means no trial, > 0 means number of days
  private String pricingModel;    // FLAT (default) | PER_SEAT
  private String gatewayType;     // STRIPE | LEMON_SQUEEZY
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public Plan() {
  }

  public Plan(final UUID id,
              final String planCode,
              final String displayName,
              final String description,
              final String billingPeriod,
              final Integer priceMinor,
              final String currency,
              final String entitlement,
              final String scope,
              final String externalProductId,
              final String externalPriceId,
              final Boolean active,
              final Integer trialPeriodDays,
              final String pricingModel,
              final String gatewayType,
              final LocalDateTime createdAt,
              final LocalDateTime updatedAt) {
    this.id = id;
    this.planCode = planCode;
    this.displayName = displayName;
    this.description = description;
    this.billingPeriod = billingPeriod;
    this.priceMinor = priceMinor;
    this.currency = currency;
    this.entitlement = entitlement;
    this.scope = scope;
    this.externalProductId = externalProductId;
    this.externalPriceId = externalPriceId;
    this.active = active;
    this.trialPeriodDays = trialPeriodDays;
    this.pricingModel = pricingModel;
    this.gatewayType = gatewayType;
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

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
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

  public String getEntitlement() {
    return entitlement;
  }

  public void setEntitlement(String entitlement) {
    this.entitlement = entitlement;
  }

  public String getScope() {
    return scope;
  }

  public void setScope(String scope) {
    this.scope = scope;
  }

  public String getExternalProductId() {
    return externalProductId;
  }

  public void setExternalProductId(String externalProductId) {
    this.externalProductId = externalProductId;
  }

  public String getExternalPriceId() {
    return externalPriceId;
  }

  public void setExternalPriceId(String externalPriceId) {
    this.externalPriceId = externalPriceId;
  }

  public Boolean getActive() {
    return active;
  }

  public void setActive(Boolean active) {
    this.active = active;
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public Integer getTrialPeriodDays() {
    return trialPeriodDays;
  }

  public void setTrialPeriodDays(Integer trialPeriodDays) {
    this.trialPeriodDays = trialPeriodDays;
  }

  public String getPricingModel() {
    return pricingModel;
  }

  public void setPricingModel(String pricingModel) {
    this.pricingModel = pricingModel;
  }

  public String getGatewayType() {
    return gatewayType;
  }

  public void setGatewayType(String gatewayType) {
    this.gatewayType = gatewayType;
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
