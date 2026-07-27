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

package com.iqkv.foundation.billingservice.settings;

/**
 * Maps {@link BillingSettings} domain objects to API response DTOs.
 */
public final class BillingSettingsDtoMapper {

  private BillingSettingsDtoMapper() {
  }

  public static BillingSettingsDtos.BillingSettingsResponse toResponse(final BillingSettings settings) {
    return new BillingSettingsDtos.BillingSettingsResponse(
        settings.getId(),
        settings.getTenantKey(),
        settings.getBillingEmail(),
        settings.getCompanyName(),
        settings.getBillingAddress(),
        settings.getTaxId(),
        settings.getTaxIdType(),
        settings.getCurrency(),
        settings.getGatewayType(),
        settings.getCreatedAt() != null ? settings.getCreatedAt().toInstant(java.time.ZoneOffset.UTC) : null,
        settings.getUpdatedAt() != null ? settings.getUpdatedAt().toInstant(java.time.ZoneOffset.UTC) : null
    );
  }

  public static BillingSettingsDtos.AdminBillingSettingsResponse toAdminResponse(final BillingSettings settings) {
    return new BillingSettingsDtos.AdminBillingSettingsResponse(
        settings.getId(),
        settings.getTenantKey(),
        settings.getExternalCustomerId(),
        settings.getBillingEmail(),
        settings.getCompanyName(),
        settings.getBillingAddress(),
        settings.getTaxId(),
        settings.getTaxIdType(),
        settings.getCurrency(),
        settings.getProfileOwnerId(),
        settings.getGatewayType(),
        settings.getCreatedAt() != null ? settings.getCreatedAt().toInstant(java.time.ZoneOffset.UTC) : null,
        settings.getUpdatedAt() != null ? settings.getUpdatedAt().toInstant(java.time.ZoneOffset.UTC) : null
    );
  }
}
