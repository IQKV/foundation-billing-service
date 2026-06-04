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

package com.iqkv.foundation.billingservice.plan;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PlanPatchRequest Unit Tests")
class PlanPatchRequestTest {

  @Test
  @DisplayName("Should create PlanPatchRequest with all fields")
  void shouldCreatePlanPatchRequestWithAllFields() {
    // Arrange & Act
    final var request = new PlanPatchRequest(
        "Updated Plan Name",
        "ANNUAL",
        4999,
        "EUR",
        "{\"feature3\": true}",
        "USER",
        "prod_updated",
        "price_updated",
        false
    );

    // Assert
    assertThat(request.displayName()).isEqualTo("Updated Plan Name");
    assertThat(request.billingPeriod()).isEqualTo("ANNUAL");
    assertThat(request.priceMinor()).isEqualTo(4999);
    assertThat(request.currency()).isEqualTo("EUR");
    assertThat(request.featureSet()).isEqualTo("{\"feature3\": true}");
    assertThat(request.scope()).isEqualTo("USER");
    assertThat(request.externalProductId()).isEqualTo("prod_updated");
    assertThat(request.externalPriceId()).isEqualTo("price_updated");
    assertThat(request.active()).isFalse();
  }

  @Test
  @DisplayName("Should create PlanPatchRequest with all null fields")
  void shouldCreatePlanPatchRequestWithAllNullFields() {
    // Arrange & Act
    final var request = new PlanPatchRequest(
        null, null, null, null, null, null, null, null, null
    );

    // Assert
    assertThat(request.displayName()).isNull();
    assertThat(request.billingPeriod()).isNull();
    assertThat(request.priceMinor()).isNull();
    assertThat(request.currency()).isNull();
    assertThat(request.featureSet()).isNull();
    assertThat(request.scope()).isNull();
    assertThat(request.externalProductId()).isNull();
    assertThat(request.externalPriceId()).isNull();
    assertThat(request.active()).isNull();
  }

  @Test
  @DisplayName("Should create PlanPatchRequest with only displayName")
  void shouldCreatePlanPatchRequestWithOnlyDisplayName() {
    // Arrange & Act
    final var request = new PlanPatchRequest(
        "Only Name Updated", null, null, null, null, null, null, null, null
    );

    // Assert
    assertThat(request.displayName()).isEqualTo("Only Name Updated");
    assertThat(request.billingPeriod()).isNull();
    assertThat(request.priceMinor()).isNull();
  }

  @Test
  @DisplayName("Should create PlanPatchRequest with only price change")
  void shouldCreatePlanPatchRequestWithOnlyPriceChange() {
    // Arrange & Act
    final var request = new PlanPatchRequest(
        null, null, 3999, "USD", null, null, null, null, null
    );

    // Assert
    assertThat(request.priceMinor()).isEqualTo(3999);
    assertThat(request.currency()).isEqualTo("USD");
    assertThat(request.displayName()).isNull();
  }

  @Test
  @DisplayName("Should create PlanPatchRequest to toggle active status")
  void shouldCreatePlanPatchRequestToToggleActiveStatus() {
    // Arrange & Act
    final var request = new PlanPatchRequest(
        null, null, null, null, null, null, null, null, true
    );

    // Assert
    assertThat(request.active()).isTrue();
  }

  @Test
  @DisplayName("Should support record equality")
  void shouldSupportRecordEquality() {
    // Arrange
    final var request1 = new PlanPatchRequest(
        "Plan Name", "MONTHLY", 1999, "USD", null, "TENANT", null, null, true
    );
    final var request2 = new PlanPatchRequest(
        "Plan Name", "MONTHLY", 1999, "USD", null, "TENANT", null, null, true
    );

    // Assert
    assertThat(request1).isEqualTo(request2);
    assertThat(request1.hashCode()).isEqualTo(request2.hashCode());
  }

  @Test
  @DisplayName("Should have meaningful toString")
  void shouldHaveMeaningfulToString() {
    // Arrange
    final var request = new PlanPatchRequest(
        "Test Plan", "ANNUAL", 5999, "GBP", null, "USER", null, null, false
    );

    // Act
    final String toString = request.toString();

    // Assert
    assertThat(toString).contains("PlanPatchRequest");
    assertThat(toString).contains("Test Plan");
    assertThat(toString).contains("ANNUAL");
    assertThat(toString).contains("5999");
    assertThat(toString).contains("GBP");
  }

  @Test
  @DisplayName("Should create PlanPatchRequest for scope change")
  void shouldCreatePlanPatchRequestForScopeChange() {
    // Arrange & Act
    final var request = new PlanPatchRequest(
        null, null, null, null, null, "USER", null, null, null
    );

    // Assert
    assertThat(request.scope()).isEqualTo("USER");
  }

  @Test
  @DisplayName("Should create PlanPatchRequest for external IDs update")
  void shouldCreatePlanPatchRequestForExternalIdsUpdate() {
    // Arrange & Act
    final var request = new PlanPatchRequest(
        null, null, null, null, null, null, "prod_new_123", "price_new_456", null
    );

    // Assert
    assertThat(request.externalProductId()).isEqualTo("prod_new_123");
    assertThat(request.externalPriceId()).isEqualTo("price_new_456");
  }
}
