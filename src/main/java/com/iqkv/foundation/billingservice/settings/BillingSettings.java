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

package com.iqkv.foundation.billingservice.settings;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain class representing a tenant's billing settings.
 * One record per tenant; bootstrapped when a {@code tenant.created} event is received.
 *
 * <p>{@code profileOwnerId} is a soft reference to {@code foundation_iam.users.id} —
 * no FK constraint because the {@code users} table lives in a separate database.
 */
public class BillingSettings {

  private UUID id;
  private String tenantKey;
  private String externalCustomerId;  // payment-gateway-agnostic (e.g. Stripe cus_xxx)
  private String billingEmail;
  private String companyName;
  private String billingAddress;      // JSONB stored as String
  private String taxId;
  private String taxIdType;
  private String currency;            // default "USD"
  private UUID profileOwnerId;        // nullable — soft ref to IAM users.id, no FK constraint
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public BillingSettings() {
  }

  public BillingSettings(UUID id,
                         String tenantKey,
                         String externalCustomerId,
                         String billingEmail,
                         String companyName,
                         String billingAddress,
                         String taxId,
                         String taxIdType,
                         String currency,
                         UUID profileOwnerId,
                         LocalDateTime createdAt,
                         LocalDateTime updatedAt) {
    this.id = id;
    this.tenantKey = tenantKey;
    this.externalCustomerId = externalCustomerId;
    this.billingEmail = billingEmail;
    this.companyName = companyName;
    this.billingAddress = billingAddress;
    this.taxId = taxId;
    this.taxIdType = taxIdType;
    this.currency = currency;
    this.profileOwnerId = profileOwnerId;
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

  public UUID getProfileOwnerId() {
    return profileOwnerId;
  }

  public void setProfileOwnerId(UUID profileOwnerId) {
    this.profileOwnerId = profileOwnerId;
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
