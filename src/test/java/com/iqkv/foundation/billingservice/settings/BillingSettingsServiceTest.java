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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import com.iqkv.foundation.billingservice.infrastructure.messaging.MessagingService;
import com.iqkv.foundation.billingservice.infrastructure.messaging.NotificationEvent;
import com.iqkv.foundation.billingservice.infrastructure.persistence.BillingSettingsMapper;
import com.iqkv.foundation.billingservice.shared.exception.ResourceNotFoundException;
import com.iqkv.foundation.billingservice.shared.exception.TenantContextMismatchException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("BillingSettingsService Unit Tests")
class BillingSettingsServiceTest {

  @Mock
  private BillingSettingsMapper billingSettingsMapper;

  @Mock
  private MessagingService messagingService;

  @InjectMocks
  private BillingSettingsService billingSettingsService;

  @Test
  @DisplayName("Should return billing settings by tenant key")
  void shouldReturnBillingSettingsByTenantKey() {
    // Arrange
    final String tenantKey = "tenant-123";
    final BillingSettings settings = createBillingSettings(tenantKey);
    when(billingSettingsMapper.findByTenantKey(tenantKey)).thenReturn(Optional.of(settings));

    // Act
    final BillingSettings result = billingSettingsService.getByTenantKey(tenantKey);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getTenantKey()).isEqualTo(tenantKey);
    verify(billingSettingsMapper).findByTenantKey(tenantKey);
  }

  @Test
  @DisplayName("Should throw ResourceNotFoundException when billing settings not found")
  void shouldThrowExceptionWhenBillingSettingsNotFound() {
    // Arrange
    final String tenantKey = "tenant-999";
    when(billingSettingsMapper.findByTenantKey(tenantKey)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> billingSettingsService.getByTenantKey(tenantKey))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("BillingSettings not found for tenantKey=" + tenantKey);
  }

  @Test
  @DisplayName("Should update billing settings successfully")
  void shouldUpdateBillingSettingsSuccessfully() {
    // Arrange
    final String tenantKey = "tenant-123";
    final BillingSettings settings = createBillingSettings(tenantKey);
    final var request = new BillingSettingsDtos.UpdateBillingSettingsRequest(
        "new-billing@example.com",
        "New Company Name",
        "123 New Street",
        "TAX123",
        "VAT",
        "EUR"
    );

    when(billingSettingsMapper.findByTenantKey(tenantKey)).thenReturn(Optional.of(settings));

    // Act
    final BillingSettings result = billingSettingsService.update(tenantKey, tenantKey, request);

    // Assert
    assertThat(result.getBillingEmail()).isEqualTo("new-billing@example.com");
    assertThat(result.getCompanyName()).isEqualTo("New Company Name");
    assertThat(result.getBillingAddress()).isEqualTo("123 New Street");
    assertThat(result.getTaxId()).isEqualTo("TAX123");
    assertThat(result.getTaxIdType()).isEqualTo("VAT");
    assertThat(result.getCurrency()).isEqualTo("EUR");
    verify(billingSettingsMapper).update(settings);
    verify(messagingService).publishNotification(any(NotificationEvent.class));
  }

  @Test
  @DisplayName("Should update only non-null fields")
  void shouldUpdateOnlyNonNullFields() {
    // Arrange
    final String tenantKey = "tenant-123";
    final BillingSettings settings = createBillingSettings(tenantKey);
    final var request = new BillingSettingsDtos.UpdateBillingSettingsRequest(
        "updated@example.com",
        null,
        null,
        null,
        null,
        null
    );

    when(billingSettingsMapper.findByTenantKey(tenantKey)).thenReturn(Optional.of(settings));

    // Act
    final BillingSettings result = billingSettingsService.update(tenantKey, tenantKey, request);

    // Assert
    assertThat(result.getBillingEmail()).isEqualTo("updated@example.com");
    assertThat(result.getCompanyName()).isEqualTo("Test Company");
    assertThat(result.getCurrency()).isEqualTo("USD");
    verify(billingSettingsMapper).update(settings);
  }

  @Test
  @DisplayName("Should throw TenantContextMismatchException when tenant keys do not match")
  void shouldThrowExceptionWhenTenantKeysMismatch() {
    // Arrange
    final String tenantKey = "tenant-123";
    final String authenticatedTenantKey = "tenant-456";
    final var request = new BillingSettingsDtos.UpdateBillingSettingsRequest(
        "test@example.com", null, null, null, null, null
    );

    // Act & Assert
    assertThatThrownBy(() -> billingSettingsService.update(tenantKey, authenticatedTenantKey, request))
        .isInstanceOf(TenantContextMismatchException.class)
        .hasMessageContaining("Authenticated tenant 'tenant-456' does not match requested tenant 'tenant-123'");
  }

  @Test
  @DisplayName("Should publish notification when billing email is present")
  void shouldPublishNotificationWhenBillingEmailPresent() {
    // Arrange
    final String tenantKey = "tenant-123";
    final BillingSettings settings = createBillingSettings(tenantKey);
    final var request = new BillingSettingsDtos.UpdateBillingSettingsRequest(
        "billing@example.com", "Updated Company", null, null, null, null
    );

    when(billingSettingsMapper.findByTenantKey(tenantKey)).thenReturn(Optional.of(settings));

    // Act
    billingSettingsService.update(tenantKey, tenantKey, request);

    // Assert
    verify(messagingService).publishNotification(argThat(event ->
        event.getRecipientEmail().equals("billing@example.com")
    ));
  }

  private BillingSettings createBillingSettings(final String tenantKey) {
    final var settings = new BillingSettings();
    settings.setId(UUID.randomUUID());
    settings.setTenantKey(tenantKey);
    settings.setExternalCustomerId("cus_" + UUID.randomUUID());
    settings.setBillingEmail("billing@example.com");
    settings.setCompanyName("Test Company");
    settings.setBillingAddress("123 Test Street");
    settings.setCurrency("USD");
    settings.setCreatedAt(LocalDateTime.now());
    settings.setUpdatedAt(LocalDateTime.now());
    return settings;
  }
}
