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

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

/**
 * DTOs for the billing settings API surface.
 * All types are immutable records; internal fields (e.g. {@code externalCustomerId}) are excluded.
 */
public final class BillingSettingsDtos {

  private BillingSettingsDtos() {}

  /**
   * PATCH request body — all fields are optional; only non-null values are applied.
   */
  public record UpdateBillingSettingsRequest(
      @Email
      String billingEmail,

      @Size(max = 255)
      String companyName,

      String billingAddress,  // JSONB — free-form address object

      @Size(max = 50)
      String taxId,

      @Size(max = 50)
      String taxIdType,

      @NotBlank @Pattern(regexp = "[A-Z]{3}", message = "must be a 3-letter ISO 4217 currency code")
      String currency
  ) {}

  /**
   * Read-only view of a tenant's billing settings.
   * Excludes internal fields (e.g. {@code externalCustomerId}) from the API surface.
   */
  public record BillingSettingsResponse(
      UUID id,
      String tenantKey,
      String billingEmail,
      String companyName,
      String billingAddress,
      String taxId,
      String taxIdType,
      String currency,
      Instant createdAt,
      Instant updatedAt
  ) {}
}
