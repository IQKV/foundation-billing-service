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

import com.iqkv.foundation.billingservice.settings.BillingSettings;

/**
 * Read-only view of a tenant's billing settings.
 * Excludes internal fields (e.g. {@code externalCustomerId}) from the API surface.
 */
public class BillingSettingsResponse {

  private UUID id;
  private String tenantKey;
  private String billingEmail;
  private String companyName;
  private String billingAddress;
  private String taxId;
  private String taxIdType;
  private String currency;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public static BillingSettingsResponse from(BillingSettings settings) {
    final var response = new BillingSettingsResponse();
    response.id = settings.getId();
    response.tenantKey = settings.getTenantKey();
    response.billingEmail = settings.getBillingEmail();
    response.companyName = settings.getCompanyName();
    response.billingAddress = settings.getBillingAddress();
    response.taxId = settings.getTaxId();
    response.taxIdType = settings.getTaxIdType();
    response.currency = settings.getCurrency();
    response.createdAt = settings.getCreatedAt();
    response.updatedAt = settings.getUpdatedAt();
    return response;
  }

  public UUID getId() {
    return id;
  }

  public String getTenantKey() {
    return tenantKey;
  }

  public String getBillingEmail() {
    return billingEmail;
  }

  public String getCompanyName() {
    return companyName;
  }

  public String getBillingAddress() {
    return billingAddress;
  }

  public String getTaxId() {
    return taxId;
  }

  public String getTaxIdType() {
    return taxIdType;
  }

  public String getCurrency() {
    return currency;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }
}
