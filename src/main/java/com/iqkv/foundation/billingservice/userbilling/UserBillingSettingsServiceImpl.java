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

import java.time.LocalDateTime;
import java.util.UUID;

import com.iqkv.foundation.billingservice.infrastructure.config.PaymentGatewayClient;
import com.iqkv.foundation.billingservice.infrastructure.persistence.UserBillingSettingsMapper;
import com.iqkv.foundation.billingservice.shared.exception.PaymentGatewayException;
import com.stripe.exception.StripeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * {@link UserBillingSettingsService} implementation active only in {@code SINGLE_TENANT} mode.
 *
 * <p>On the first subscription action for a user, this service:
 * <ol>
 *   <li>Checks whether a {@code user_billing_settings} row already exists for the user.</li>
 *   <li>If not, creates a Stripe customer (with no email — the user can update it later).</li>
 *   <li>Persists the new {@code user_billing_settings} row with the Stripe customer ID.</li>
 * </ol>
 *
 * <p>The bean is only registered when {@code iqkv.platform.rollout-mode=SINGLE_TENANT},
 * so it is never instantiated in multi-tenant deployments.
 */
@Service
@ConditionalOnProperty(name = "iqkv.platform.rollout-mode", havingValue = "SINGLE_TENANT")
public class UserBillingSettingsServiceImpl implements UserBillingSettingsService {

  private static final Logger log = LoggerFactory.getLogger(UserBillingSettingsServiceImpl.class);

  private final UserBillingSettingsMapper userBillingSettingsMapper;
  private final PaymentGatewayClient paymentGatewayClient;

  public UserBillingSettingsServiceImpl(final UserBillingSettingsMapper userBillingSettingsMapper,
                                        final PaymentGatewayClient paymentGatewayClient) {
    this.userBillingSettingsMapper = userBillingSettingsMapper;
    this.paymentGatewayClient = paymentGatewayClient;
  }

  /**
   * {@inheritDoc}
   *
   * <p>If settings already exist for the user, they are returned immediately without
   * contacting Stripe. Otherwise a new Stripe customer is created and the settings row
   * is inserted.
   */
  @Override
  public UserBillingSettings getOrCreateUserBillingSettings(final UUID userId) {
    return userBillingSettingsMapper.findByUserId(userId)
        .orElseGet(() -> createUserBillingSettings(userId));
  }

  // ---------------------------------------------------------------------------
  // Private helpers
  // ---------------------------------------------------------------------------

  private UserBillingSettings createUserBillingSettings(final UUID userId) {
    log.debug("Creating user billing settings for userId={}", userId);

    final String externalCustomerId;
    try {
      // Create Stripe customer with no email; the user can update billing details later.
      externalCustomerId = paymentGatewayClient.createCustomer("user:" + userId, null);
    } catch (final StripeException e) {
      throw new PaymentGatewayException(
          "Failed to create Stripe customer for userId=" + userId, e);
    }

    final LocalDateTime now = LocalDateTime.now();
    final UserBillingSettings settings = new UserBillingSettings(
        UUID.randomUUID(),
        userId,
        externalCustomerId,
        null,   // billingEmail — not known at creation time
        null,   // companyName
        null,   // billingAddress
        null,   // taxId
        null,   // taxIdType
        "USD",
        now,
        now
    );

    userBillingSettingsMapper.insert(settings);
    log.info("UserBillingSettings created: userId={}, externalCustomerId={}", userId, externalCustomerId);
    return settings;
  }
}
