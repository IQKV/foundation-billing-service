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
import java.util.Map;
import java.util.UUID;

import com.iqkv.foundation.billingservice.gateway.command.CreateCustomerCommand;
import com.iqkv.foundation.billingservice.gateway.port.PaymentGatewayPort;
import com.iqkv.foundation.billingservice.infrastructure.config.StripeConfigurationProperties;
import com.iqkv.foundation.billingservice.infrastructure.persistence.UserBillingSettingsMapper;
import com.iqkv.foundation.billingservice.settings.BillingSettingsDtos;
import com.iqkv.foundation.billingservice.shared.exception.DuplicateResourceException;
import com.iqkv.foundation.billingservice.shared.exception.PaymentGatewayException;
import com.iqkv.foundation.billingservice.shared.exception.ResourceNotFoundException;
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
 *   <li>If not, creates a payment gateway customer (with no email — the user can update it later).</li>
 *   <li>Persists the new {@code user_billing_settings} row with the external customer ID.</li>
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
  private final PaymentGatewayPort paymentGatewayPort;
  private final StripeConfigurationProperties stripeConfig;

  public UserBillingSettingsServiceImpl(final UserBillingSettingsMapper userBillingSettingsMapper,
                                        final PaymentGatewayPort paymentGatewayPort,
                                        final StripeConfigurationProperties stripeConfig) {
    this.userBillingSettingsMapper = userBillingSettingsMapper;
    this.paymentGatewayPort = paymentGatewayPort;
    this.stripeConfig = stripeConfig;
  }

  /**
   * {@inheritDoc}
   *
   * <p>If settings already exist for the user, they are returned immediately without
   * contacting the payment gateway. Otherwise a new gateway customer is created and
   * the settings row is inserted.
   */
  @Override
  public UserBillingSettings getOrCreateUserBillingSettings(final UUID userId) {
    return userBillingSettingsMapper.findByUserId(userId)
        .orElseGet(() -> createUserBillingSettings(userId));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public UserBillingSettings getByUserId(final UUID userId) {
    return userBillingSettingsMapper.findByUserId(userId)
        .orElseThrow(() -> new ResourceNotFoundException("UserBillingSettings not found for userId=" + userId));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public UserBillingSettings createForUser(final UUID userId, final BillingSettingsDtos.CreateBillingSettingsRequest request) {
    if (userBillingSettingsMapper.existsByUserId(userId)) {
      throw new DuplicateResourceException("UserBillingSettings already exist for userId=" + userId);
    }

    final String externalCustomerId = paymentGatewayPort.createCustomer(
        new CreateCustomerCommand(
            request.companyName() != null ? request.companyName() : "user:" + userId,
            request.billingEmail(),
            Map.of("userId", userId.toString())));

    final LocalDateTime now = LocalDateTime.now();
    final UserBillingSettings settings = new UserBillingSettings(
        UUID.randomUUID(),
        userId,
        externalCustomerId,
        request.billingEmail(),
        request.companyName(),
        request.billingAddress(),
        request.taxId(),
        request.taxIdType(),
        request.currency(),
        "STRIPE",
        now,
        now
    );

    userBillingSettingsMapper.insert(settings);
    log.info("UserBillingSettings created: userId={}, externalCustomerId={}", userId, externalCustomerId);
    return settings;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public UserBillingSettings updateForUser(final UUID userId, final BillingSettingsDtos.UpdateBillingSettingsRequest request) {
    final UserBillingSettings settings = getByUserId(userId);

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
    userBillingSettingsMapper.update(settings);
    return settings;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String createPortalSession(final UUID userId) {
    final UserBillingSettings settings = userBillingSettingsMapper.findByUserId(userId)
        .orElseThrow(() -> new ResourceNotFoundException("UserBillingSettings not found for userId=" + userId));

    if (settings.getExternalCustomerId() == null || settings.getExternalCustomerId().isBlank()) {
      throw new ResourceNotFoundException("No external customer ID found for userId=" + userId);
    }

    return paymentGatewayPort.createPortalSession(settings.getExternalCustomerId(), stripeConfig.portalReturnUrl());
  }

  // ---------------------------------------------------------------------------
  // Private helpers
  // ---------------------------------------------------------------------------

  private UserBillingSettings createUserBillingSettings(final UUID userId) {
    log.debug("Creating user billing settings for userId={}", userId);

    // Create gateway customer with no email; the user can update billing details later.
    final String externalCustomerId;
    try {
      externalCustomerId = paymentGatewayPort.createCustomer(
          new CreateCustomerCommand("user:" + userId, null, Map.of("userId", userId.toString())));
    } catch (final PaymentGatewayException e) {
      throw new PaymentGatewayException(
          "Failed to create payment gateway customer for userId=" + userId, e);
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
        "STRIPE",
        now,
        now
    );

    userBillingSettingsMapper.insert(settings);
    log.info("UserBillingSettings created: userId={}, externalCustomerId={}", userId, externalCustomerId);
    return settings;
  }
}
