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

import com.iqkv.foundation.billingservice.infrastructure.persistence.BillingSettingsMapper;
import com.iqkv.foundation.billingservice.settings.BillingSettings;
import com.iqkv.foundation.billingservice.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Application service for billing settings management.
 *
 * <p>Reads and updates the tenant's billing profile. Updates are applied to the local
 * record; Stripe customer sync is handled separately via the outbox pattern.
 */
@Service
public class BillingSettingsService {

  private final BillingSettingsMapper billingSettingsMapper;

  public BillingSettingsService(BillingSettingsMapper billingSettingsMapper) {
    this.billingSettingsMapper = billingSettingsMapper;
  }

  /**
   * Returns billing settings for the given tenant.
   *
   * @throws ResourceNotFoundException if no settings exist for the tenant
   */
  public BillingSettings getByTenantKey(String tenantKey) {
    return billingSettingsMapper.findByTenantKey(tenantKey)
        .orElseThrow(() -> new ResourceNotFoundException(
            "BillingSettings not found for tenantKey=" + tenantKey));
  }

  /**
   * Applies non-null fields from the request to the tenant's billing settings.
   *
   * @throws ResourceNotFoundException if no settings exist for the tenant
   */
  public BillingSettings update(String tenantKey, BillingSettingsRequest request) {
    final BillingSettings settings = getByTenantKey(tenantKey);

    if (request.getBillingEmail() != null) {
      settings.setBillingEmail(request.getBillingEmail());
    }
    if (request.getCompanyName() != null) {
      settings.setCompanyName(request.getCompanyName());
    }
    if (request.getBillingAddress() != null) {
      settings.setBillingAddress(request.getBillingAddress());
    }
    if (request.getTaxId() != null) {
      settings.setTaxId(request.getTaxId());
    }
    if (request.getTaxIdType() != null) {
      settings.setTaxIdType(request.getTaxIdType());
    }
    if (request.getCurrency() != null) {
      settings.setCurrency(request.getCurrency());
    }

    settings.setUpdatedAt(LocalDateTime.now());
    billingSettingsMapper.update(settings);
    return settings;
  }
}
