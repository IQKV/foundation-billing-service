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
 * All types are immutable records. Tenant self-service responses omit internal gateway identifiers;
 * admin responses expose them for platform operations.
 */
public final class BillingSettingsDtos {

  private BillingSettingsDtos() {
  }

  /**
   * POST request body for tenant self-service billing settings creation.
   * Does not expose {@code externalCustomerId} — that is managed by the platform operator.
   */
  public record CreateBillingSettingsRequest(
      @NotBlank @Email
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
  ) {
  }

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
  ) {
  }

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
      String gatewayType,
      Instant createdAt,
      Instant updatedAt
  ) {
  }

  /**
   * Response body for the customer portal session request.
   *
   * @param url the URL of the portal session
   */
  public record PortalSessionResponse(String url) {
  }

  // ─── Admin DTOs (PLATFORM_ADMIN) ───────────────────────────────────────────

  /**
   * Full billing settings view for platform operators (includes gateway customer id and profile owner).
   */
  public record AdminBillingSettingsResponse(
      UUID id,
      String tenantKey,
      String externalCustomerId,
      String billingEmail,
      String companyName,
      String billingAddress,
      String taxId,
      String taxIdType,
      String currency,
      UUID profileOwnerId,
      String gatewayType,
      Instant createdAt,
      Instant updatedAt
  ) {
  }

  /**
   * Request body for {@code POST /admin/tenants/{tenantKey}/billing-settings}.
   */
  public record AdminCreateBillingSettingsRequest(
      @NotBlank @Size(max = 255)
      String externalCustomerId,

      @NotBlank @Email
      String billingEmail,

      @Size(max = 255)
      String companyName,

      String billingAddress,

      @Size(max = 100)
      String taxId,

      @Size(max = 50)
      String taxIdType,

      @NotBlank @Pattern(regexp = "[A-Z]{3}", message = "must be a 3-letter ISO 4217 currency code")
      String currency,

      UUID profileOwnerId
  ) {
  }

  /**
   * Request body for {@code PUT} — replaces mutable fields; use {@code null} on optional fields to clear them.
   */
  public record AdminReplaceBillingSettingsRequest(
      @NotBlank @Size(max = 255)
      String externalCustomerId,

      @NotBlank @Email
      String billingEmail,

      @Size(max = 255)
      String companyName,

      String billingAddress,

      @Size(max = 100)
      String taxId,

      @Size(max = 50)
      String taxIdType,

      @NotBlank @Pattern(regexp = "[A-Z]{3}", message = "must be a 3-letter ISO 4217 currency code")
      String currency,

      UUID profileOwnerId
  ) {
  }

  /**
   * Request body for {@code PATCH} — only non-null fields are applied.
   */
  public record AdminPatchBillingSettingsRequest(
      @Size(max = 255)
      String externalCustomerId,

      @Email
      String billingEmail,

      @Size(max = 255)
      String companyName,

      String billingAddress,

      @Size(max = 100)
      String taxId,

      @Size(max = 50)
      String taxIdType,

      @Pattern(regexp = "[A-Z]{3}", message = "must be a 3-letter ISO 4217 currency code")
      String currency,

      UUID profileOwnerId
  ) {
  }
}
