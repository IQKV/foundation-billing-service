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

import com.iqkv.foundation.billingservice.settings.BillingSettingsDtos;

/**
 * Maps {@link UserBillingSettings} domain objects to API response DTOs.
 */
public final class UserBillingSettingsDtoMapper {

  private UserBillingSettingsDtoMapper() {
  }

  public static BillingSettingsDtos.BillingSettingsResponse toResponse(final UserBillingSettings settings) {
    return new BillingSettingsDtos.BillingSettingsResponse(
        settings.getId(),
        null, // tenantKey is not applicable for user billing settings
        settings.getBillingEmail(),
        settings.getCompanyName(),
        settings.getBillingAddress(),
        settings.getTaxId(),
        settings.getTaxIdType(),
        settings.getCurrency(),
        settings.getCreatedAt() != null ? settings.getCreatedAt().toInstant(java.time.ZoneOffset.UTC) : null,
        settings.getUpdatedAt() != null ? settings.getUpdatedAt().toInstant(java.time.ZoneOffset.UTC) : null
    );
  }
}
