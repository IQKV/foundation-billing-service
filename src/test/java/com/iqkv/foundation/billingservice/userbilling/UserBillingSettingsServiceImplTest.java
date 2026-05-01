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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import com.iqkv.foundation.billingservice.infrastructure.config.PaymentGatewayClient;
import com.iqkv.foundation.billingservice.infrastructure.persistence.UserBillingSettingsMapper;
import com.iqkv.foundation.billingservice.shared.exception.PaymentGatewayException;
import com.stripe.exception.StripeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserBillingSettingsServiceImpl Unit Tests")
class UserBillingSettingsServiceImplTest {

  @Mock
  private UserBillingSettingsMapper userBillingSettingsMapper;

  @Mock
  private PaymentGatewayClient paymentGatewayClient;

  @InjectMocks
  private UserBillingSettingsServiceImpl userBillingSettingsService;

  @Test
  @DisplayName("Should return existing user billing settings when they exist")
  void shouldReturnExistingUserBillingSettings() throws StripeException {
    // Arrange
    final UUID userId = UUID.randomUUID();
    final UserBillingSettings existingSettings = createUserBillingSettings(userId);
    when(userBillingSettingsMapper.findByUserId(userId)).thenReturn(Optional.of(existingSettings));

    // Act
    final UserBillingSettings result = userBillingSettingsService.getOrCreateUserBillingSettings(userId);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getUserId()).isEqualTo(userId);
    assertThat(result.getExternalCustomerId()).isEqualTo("cus_existing123");
    verify(userBillingSettingsMapper).findByUserId(userId);
    verifyNoInteractions(paymentGatewayClient);
  }

  @Test
  @DisplayName("Should create new user billing settings when they do not exist")
  void shouldCreateNewUserBillingSettings() throws StripeException {
    // Arrange
    final UUID userId = UUID.randomUUID();
    final String externalCustomerId = "cus_new123";

    when(userBillingSettingsMapper.findByUserId(userId)).thenReturn(Optional.empty());
    when(paymentGatewayClient.createCustomer("user:" + userId, null)).thenReturn(externalCustomerId);

    // Act
    final UserBillingSettings result = userBillingSettingsService.getOrCreateUserBillingSettings(userId);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getUserId()).isEqualTo(userId);
    assertThat(result.getExternalCustomerId()).isEqualTo(externalCustomerId);
    assertThat(result.getCurrency()).isEqualTo("USD");
    assertThat(result.getBillingEmail()).isNull();
    verify(userBillingSettingsMapper).findByUserId(userId);
    verify(paymentGatewayClient).createCustomer("user:" + userId, null);
    verify(userBillingSettingsMapper).insert(argThat(settings ->
        settings.getUserId().equals(userId)
            && settings.getExternalCustomerId().equals(externalCustomerId)
            && settings.getCurrency().equals("USD")
    ));
  }

  @Test
  @DisplayName("Should throw PaymentGatewayException when Stripe customer creation fails")
  void shouldThrowExceptionWhenStripeCustomerCreationFails() throws StripeException {
    // Arrange
    final UUID userId = UUID.randomUUID();
    final StripeException stripeException = new StripeException("Stripe API error", "req_123", "code", 500) {};

    when(userBillingSettingsMapper.findByUserId(userId)).thenReturn(Optional.empty());
    when(paymentGatewayClient.createCustomer("user:" + userId, null)).thenThrow(stripeException);

    // Act & Assert
    assertThatThrownBy(() -> userBillingSettingsService.getOrCreateUserBillingSettings(userId))
        .isInstanceOf(PaymentGatewayException.class)
        .hasMessageContaining("Failed to create Stripe customer for userId=" + userId)
        .hasCause(stripeException);
  }

  @Test
  @DisplayName("Should create user billing settings with correct metadata")
  void shouldCreateUserBillingSettingsWithCorrectMetadata() throws StripeException {
    // Arrange
    final UUID userId = UUID.randomUUID();
    final String externalCustomerId = "cus_metadata123";

    when(userBillingSettingsMapper.findByUserId(userId)).thenReturn(Optional.empty());
    when(paymentGatewayClient.createCustomer("user:" + userId, null)).thenReturn(externalCustomerId);

    // Act
    final UserBillingSettings result = userBillingSettingsService.getOrCreateUserBillingSettings(userId);

    // Assert
    assertThat(result.getId()).isNotNull();
    assertThat(result.getCreatedAt()).isNotNull();
    assertThat(result.getUpdatedAt()).isNotNull();
    assertThat(result.getCompanyName()).isNull();
    assertThat(result.getBillingAddress()).isNull();
    assertThat(result.getTaxId()).isNull();
    assertThat(result.getTaxIdType()).isNull();
    verify(paymentGatewayClient).createCustomer("user:" + userId, null);
  }

  private UserBillingSettings createUserBillingSettings(final UUID userId) {
    return new UserBillingSettings(
        UUID.randomUUID(),
        userId,
        "cus_existing123",
        "user@example.com",
        "User Company",
        "456 User Street",
        "TAX456",
        "VAT",
        "USD",
        LocalDateTime.now(),
        LocalDateTime.now()
    );
  }
}
