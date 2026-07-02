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

package com.iqkv.foundation.billingservice.infrastructure.config;

import com.iqkv.foundation.billingservice.gateway.GatewayType;
import com.iqkv.foundation.billingservice.shared.exception.InvalidPlatformModeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Validates the platform rollout mode configuration at startup for the Billing service.
 * Runs with highest precedence to ensure validation completes before any business logic.
 *
 * <p>In SINGLE_TENANT mode, also validates that {@code iqkv.billing.default-contact-email}
 * is present when the payment gateway requires a non-null email for customer creation.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PlatformModeValidatorImpl implements PlatformModeValidator, ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(PlatformModeValidatorImpl.class);

  private final PlatformConfigurationProperties platformConfig;
  private final BillingConfigurationProperties billingConfig;
  private final PaymentGatewayConfigurationProperties paymentGatewayConfig;
  private RolloutMode validatedMode;

  public PlatformModeValidatorImpl(
      final PlatformConfigurationProperties platformConfig,
      final BillingConfigurationProperties billingConfig,
      final PaymentGatewayConfigurationProperties paymentGatewayConfig) {
    this.platformConfig = platformConfig;
    this.billingConfig = billingConfig;
    this.paymentGatewayConfig = paymentGatewayConfig;
  }

  @Override
  public void run(final ApplicationArguments args) {
    validate();
  }

  @Override
  public void validate() {
    if (platformConfig == null || platformConfig.rolloutMode() == null) {
      final String message = "Platform rollout mode is not configured. "
                             + "Please set 'iqkv.platform.rollout-mode' to either 'MULTI_TENANT' or 'SINGLE_TENANT'.";
      log.error("Platform mode validation failed: {}", message);
      throw new InvalidPlatformModeException(message);
    }

    validatedMode = platformConfig.rolloutMode();
    log.info("Platform rollout mode validated successfully: {}", validatedMode);

    if (validatedMode == RolloutMode.SINGLE_TENANT) {
      validateSingleTenantConfig();
    }
  }

  @Override
  public RolloutMode getMode() {
    if (validatedMode == null) {
      throw new IllegalStateException(
          "Platform mode has not been validated yet. Ensure validate() is called during startup.");
    }
    return validatedMode;
  }

  /**
   * Validates billing-specific configuration required in SINGLE_TENANT mode.
   * Logs a warning if {@code iqkv.billing.default-contact-email} is absent for STRIPE,
   * throws an exception if absent for LEMON_SQUEEZY (since LS requires email).
   */
  private void validateSingleTenantConfig() {
    final String defaultEmail = billingConfig != null ? billingConfig.defaultContactEmail() : null;
    if (defaultEmail == null || defaultEmail.isBlank()) {
      if (paymentGatewayConfig.type() == GatewayType.LEMON_SQUEEZY) {
        final String message = "Single-tenant mode with LEMON_SQUEEZY requires 'iqkv.billing.default-contact-email' to be configured.";
        log.error(message);
        throw new com.iqkv.foundation.billingservice.shared.exception.InvalidPlatformModeException(message);
      }
      log.warn(
          "Single-tenant mode is active but 'iqkv.billing.default-contact-email' is not configured. "
          + "Stripe customers created from bootstrap events will have no email address. "
          + "Set 'iqkv.billing.default-contact-email' to suppress this warning.");
    } else {
      log.info("Billing default contact email configured for single-tenant mode: {}", defaultEmail);
    }
  }
}
