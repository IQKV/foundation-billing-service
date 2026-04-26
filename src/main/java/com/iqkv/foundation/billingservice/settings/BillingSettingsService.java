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

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;

import com.iqkv.foundation.billingservice.infrastructure.messaging.MessagingService;
import com.iqkv.foundation.billingservice.infrastructure.messaging.NotificationEvent;
import com.iqkv.foundation.billingservice.infrastructure.messaging.NotificationEventType;
import com.iqkv.foundation.billingservice.infrastructure.persistence.BillingSettingsMapper;
import com.iqkv.foundation.billingservice.shared.exception.ResourceNotFoundException;
import com.iqkv.foundation.billingservice.shared.exception.TenantContextMismatchException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Application service for billing settings management.
 *
 * <p>Reads and updates the tenant's billing profile. Updates are applied to the local
 * record; Stripe customer sync is handled separately via the outbox pattern.
 */
@Service
public class BillingSettingsService {

  private static final Logger log = LoggerFactory.getLogger(BillingSettingsService.class);

  private final BillingSettingsMapper billingSettingsMapper;
  private final MessagingService messagingService;

  public BillingSettingsService(final BillingSettingsMapper billingSettingsMapper,
                                final MessagingService messagingService) {
    this.billingSettingsMapper = billingSettingsMapper;
    this.messagingService = messagingService;
  }

  /**
   * Returns billing settings for the given tenant.
   *
   * @throws ResourceNotFoundException if no settings exist for the tenant
   */
  public BillingSettings getByTenantKey(final String tenantKey) {
    return billingSettingsMapper.findByTenantKey(tenantKey)
        .orElseThrow(() -> new ResourceNotFoundException(
            "BillingSettings not found for tenantKey=" + tenantKey));
  }

  /**
   * Applies non-null fields from the request to the tenant's billing settings.
   * Verifies that {@code authenticatedTenantKey} matches the requested {@code tenantKey}
   * to prevent cross-tenant data access.
   *
   * @throws TenantContextMismatchException if the authenticated tenant does not match the path
   * @throws ResourceNotFoundException      if no settings exist for the tenant
   */
  public BillingSettings update(final String tenantKey,
                                final String authenticatedTenantKey,
                                final BillingSettingsDtos.UpdateBillingSettingsRequest request) {
    if (!tenantKey.equals(authenticatedTenantKey)) {
      throw new TenantContextMismatchException(
          "Authenticated tenant '" + authenticatedTenantKey + "' does not match requested tenant '" + tenantKey + "'");
    }

    final BillingSettings settings = getByTenantKey(tenantKey);

    if (request.billingEmail() != null) {
      settings.setBillingEmail(request.billingEmail());
    }
    if (request.companyName() != null) {
      settings.setCompanyName(request.companyName());
    }
    if (request.billingAddress() != null) {
      settings.setBillingAddress(request.billingAddress());
    }
    if (request.taxId() != null) {
      settings.setTaxId(request.taxId());
    }
    if (request.taxIdType() != null) {
      settings.setTaxIdType(request.taxIdType());
    }
    if (request.currency() != null) {
      settings.setCurrency(request.currency());
    }

    settings.setUpdatedAt(LocalDateTime.now());
    billingSettingsMapper.update(settings);

    // Send billing settings updated notification
    final String email = resolveEmail(settings);
    if (email != null) {
      try {
        messagingService.publishNotification(new NotificationEvent(
            email,
            "en", // TODO: get from user preferences or tenant settings
            NotificationEventType.BILLING_UPDATED,
            Map.of(
                "companyName", settings.getCompanyName() != null ? settings.getCompanyName() : "",
                "tenantKey", tenantKey,
                "updatedAt", settings.getUpdatedAt().toString()
            ),
            Instant.now()));
      } catch (final Exception e) {
        log.warn("Failed to send billing updated notification for tenant {}: {}", tenantKey, e.getMessage());
      }
    }

    return settings;
  }

  /**
   * Resolves the email address for billing notifications.
   * Returns billingEmail if set, otherwise null.
   */
  private String resolveEmail(final BillingSettings settings) {
    if (settings.getBillingEmail() != null && !settings.getBillingEmail().isBlank()) {
      return settings.getBillingEmail();
    }
    return null;
  }
}
