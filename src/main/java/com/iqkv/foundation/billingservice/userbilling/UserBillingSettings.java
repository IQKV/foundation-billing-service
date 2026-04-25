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

package com.iqkv.foundation.billingservice.userbilling;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain class representing per-user billing settings used in single-tenant mode.
 *
 * <p>One record per user; bootstrapped on the user's first subscription action.
 * {@code userId} is a soft reference to {@code foundation_iam.users.id} —
 * no FK constraint because the {@code users} table lives in a separate database.
 *
 * <p>{@code billingAddress} is a JSON string stored as TEXT in the database.
 */
public class UserBillingSettings {

  private UUID id;
  private UUID userId;
  private String externalCustomerId;  // payment-gateway-agnostic (e.g. Stripe cus_xxx)
  private String billingEmail;
  private String companyName;
  private String billingAddress;      // JSON string stored as TEXT
  private String taxId;
  private String taxIdType;
  private String currency;            // default "USD"
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public UserBillingSettings() {
  }

  public UserBillingSettings(UUID id,
                              UUID userId,
                              String externalCustomerId,
                              String billingEmail,
                              String companyName,
                              String billingAddress,
                              String taxId,
                              String taxIdType,
                              String currency,
                              LocalDateTime createdAt,
                              LocalDateTime updatedAt) {
    this.id = id;
    this.userId = userId;
    this.externalCustomerId = externalCustomerId;
    this.billingEmail = billingEmail;
    this.companyName = companyName;
    this.billingAddress = billingAddress;
    this.taxId = taxId;
    this.taxIdType = taxIdType;
    this.currency = currency;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getUserId() {
    return userId;
  }

  public void setUserId(UUID userId) {
    this.userId = userId;
  }

  public String getExternalCustomerId() {
    return externalCustomerId;
  }

  public void setExternalCustomerId(String externalCustomerId) {
    this.externalCustomerId = externalCustomerId;
  }

  public String getBillingEmail() {
    return billingEmail;
  }

  public void setBillingEmail(String billingEmail) {
    this.billingEmail = billingEmail;
  }

  public String getCompanyName() {
    return companyName;
  }

  public void setCompanyName(String companyName) {
    this.companyName = companyName;
  }

  public String getBillingAddress() {
    return billingAddress;
  }

  public void setBillingAddress(String billingAddress) {
    this.billingAddress = billingAddress;
  }

  public String getTaxId() {
    return taxId;
  }

  public void setTaxId(String taxId) {
    this.taxId = taxId;
  }

  public String getTaxIdType() {
    return taxIdType;
  }

  public void setTaxIdType(String taxIdType) {
    this.taxIdType = taxIdType;
  }

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
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
