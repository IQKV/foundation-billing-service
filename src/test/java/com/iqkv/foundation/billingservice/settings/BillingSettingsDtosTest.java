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

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BillingSettingsDtos Unit Tests")
class BillingSettingsDtosTest {

  @Test
  @DisplayName("Should create UpdateBillingSettingsRequest with all fields")
  void shouldCreateUpdateRequestWithAllFields() {
    // Arrange & Act
    final var request = new BillingSettingsDtos.UpdateBillingSettingsRequest(
        "billing@example.com",
        "Test Company",
        "{\"street\": \"123 Main St\"}",
        "TAX123",
        "VAT",
        "USD"
    );

    // Assert
    assertThat(request.billingEmail()).isEqualTo("billing@example.com");
    assertThat(request.companyName()).isEqualTo("Test Company");
    assertThat(request.billingAddress()).isEqualTo("{\"street\": \"123 Main St\"}");
    assertThat(request.taxId()).isEqualTo("TAX123");
    assertThat(request.taxIdType()).isEqualTo("VAT");
    assertThat(request.currency()).isEqualTo("USD");
  }

  @Test
  @DisplayName("Should create UpdateBillingSettingsRequest with null fields")
  void shouldCreateUpdateRequestWithNullFields() {
    // Arrange & Act
    final var request = new BillingSettingsDtos.UpdateBillingSettingsRequest(
        "billing@example.com", null, null, null, null, "EUR"
    );

    // Assert
    assertThat(request.billingEmail()).isEqualTo("billing@example.com");
    assertThat(request.companyName()).isNull();
    assertThat(request.billingAddress()).isNull();
    assertThat(request.taxId()).isNull();
    assertThat(request.taxIdType()).isNull();
    assertThat(request.currency()).isEqualTo("EUR");
  }

  @Test
  @DisplayName("Should create BillingSettingsResponse with all fields")
  void shouldCreateBillingSettingsResponseWithAllFields() {
    // Arrange
    final UUID id = UUID.randomUUID();
    final Instant createdAt = Instant.now();
    final Instant updatedAt = Instant.now();

    // Act
    final var response = new BillingSettingsDtos.BillingSettingsResponse(
        id, "tenant-123", "billing@example.com", "Test Company",
        "{\"street\": \"123 Main St\"}", "TAX123", "VAT", "USD",
        createdAt, updatedAt
    );

    // Assert
    assertThat(response.id()).isEqualTo(id);
    assertThat(response.tenantKey()).isEqualTo("tenant-123");
    assertThat(response.billingEmail()).isEqualTo("billing@example.com");
    assertThat(response.companyName()).isEqualTo("Test Company");
    assertThat(response.billingAddress()).isEqualTo("{\"street\": \"123 Main St\"}");
    assertThat(response.taxId()).isEqualTo("TAX123");
    assertThat(response.taxIdType()).isEqualTo("VAT");
    assertThat(response.currency()).isEqualTo("USD");
    assertThat(response.createdAt()).isEqualTo(createdAt);
    assertThat(response.updatedAt()).isEqualTo(updatedAt);
  }

  @Test
  @DisplayName("Should support record equality for UpdateBillingSettingsRequest")
  void shouldSupportRecordEqualityForUpdateRequest() {
    // Arrange
    final var request1 = new BillingSettingsDtos.UpdateBillingSettingsRequest(
        "billing@example.com", "Company", null, null, null, "USD"
    );
    final var request2 = new BillingSettingsDtos.UpdateBillingSettingsRequest(
        "billing@example.com", "Company", null, null, null, "USD"
    );

    // Assert
    assertThat(request1).isEqualTo(request2);
    assertThat(request1.hashCode()).isEqualTo(request2.hashCode());
  }

  @Test
  @DisplayName("Should support record equality for BillingSettingsResponse")
  void shouldSupportRecordEqualityForResponse() {
    // Arrange
    final UUID id = UUID.randomUUID();
    final Instant now = Instant.now();
    final var response1 = new BillingSettingsDtos.BillingSettingsResponse(
        id, "tenant-123", "billing@example.com", "Company", null, null, null, "USD", now, now
    );
    final var response2 = new BillingSettingsDtos.BillingSettingsResponse(
        id, "tenant-123", "billing@example.com", "Company", null, null, null, "USD", now, now
    );

    // Assert
    assertThat(response1).isEqualTo(response2);
    assertThat(response1.hashCode()).isEqualTo(response2.hashCode());
  }

  @Test
  @DisplayName("Should have meaningful toString for UpdateBillingSettingsRequest")
  void shouldHaveMeaningfulToStringForUpdateRequest() {
    // Arrange
    final var request = new BillingSettingsDtos.UpdateBillingSettingsRequest(
        "billing@example.com", "Test Company", null, null, null, "USD"
    );

    // Act
    final String toString = request.toString();

    // Assert
    assertThat(toString).contains("UpdateBillingSettingsRequest");
    assertThat(toString).contains("billing@example.com");
    assertThat(toString).contains("Test Company");
  }
}
