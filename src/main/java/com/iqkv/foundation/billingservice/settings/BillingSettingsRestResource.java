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

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST resource for tenant billing settings.
 *
 * <p>Requires {@code TENANT_OWNER} authority — enforced at the security layer.
 * The {@code tenantKey} path variable must match the authenticated tenant's key.
 */
@RestController
@RequestMapping("/api/v1/billing/settings")
public class BillingSettingsRestResource {

  private final BillingSettingsService billingSettingsService;

  public BillingSettingsRestResource(BillingSettingsService billingSettingsService) {
    this.billingSettingsService = billingSettingsService;
  }

  /**
   * Returns billing settings for the given tenant.
   *
   * @param tenantKey the tenant identifier
   * @return 200 with billing settings, or 404 if not found
   */
  @GetMapping("/{tenantKey}")
  public ResponseEntity<BillingSettingsResponse> getSettings(@PathVariable String tenantKey) {
    final var settings = billingSettingsService.getByTenantKey(tenantKey);
    return ResponseEntity.ok(BillingSettingsResponse.from(settings));
  }

  /**
   * Partially updates billing settings for the given tenant.
   * Only non-null fields in the request body are applied.
   *
   * @param tenantKey the tenant identifier
   * @param request   the fields to update
   * @return 200 with updated billing settings, or 404 if not found
   */
  @PatchMapping("/{tenantKey}")
  public ResponseEntity<BillingSettingsResponse> updateSettings(
      @PathVariable String tenantKey,
      @Valid @RequestBody BillingSettingsRequest request) {
    final var updated = billingSettingsService.update(tenantKey, request);
    return ResponseEntity.ok(BillingSettingsResponse.from(updated));
  }
}
