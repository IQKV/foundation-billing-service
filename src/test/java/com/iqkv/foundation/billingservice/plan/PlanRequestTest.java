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

@DisplayName("PlanRequest Unit Tests")
class PlanRequestTest {

  @Test
  @DisplayName("Should create PlanRequest with all fields")
  void shouldCreatePlanRequestWithAllFields() {
    // Arrange & Act
    final var request = new PlanRequest(
        "pro-monthly",
        "Professional Monthly",
        "Professional plan with priority support",
        "MONTHLY",
        2999,
        "USD",
        "{\"feature1\": true, \"feature2\": false}",
        "TENANT",
        "prod_123",
        "price_123",
        true
    );

    // Assert
    assertThat(request.planCode()).isEqualTo("pro-monthly");
    assertThat(request.displayName()).isEqualTo("Professional Monthly");
    assertThat(request.description()).isEqualTo("Professional plan with priority support");
    assertThat(request.billingPeriod()).isEqualTo("MONTHLY");
    assertThat(request.priceMinor()).isEqualTo(2999);
    assertThat(request.currency()).isEqualTo("USD");
    assertThat(request.featureSet()).isEqualTo("{\"feature1\": true, \"feature2\": false}");
    assertThat(request.scope()).isEqualTo("TENANT");
    assertThat(request.externalProductId()).isEqualTo("prod_123");
    assertThat(request.externalPriceId()).isEqualTo("price_123");
    assertThat(request.active()).isTrue();
  }

  @Test
  @DisplayName("Should create PlanRequest with null featureSet")
  void shouldCreatePlanRequestWithNullFeatureSet() {
    // Arrange & Act
    final var request = new PlanRequest(
        "basic-monthly",
        "Basic Monthly",
        null,
        "MONTHLY",
        999,
        "USD",
        null,
        "USER",
        null,
        null,
        true
    );

    // Assert
    assertThat(request.featureSet()).isNull();
    assertThat(request.scope()).isEqualTo("USER");
  }

  @Test
  @DisplayName("Should create PlanRequest for annual billing")
  void shouldCreatePlanRequestForAnnualBilling() {
    // Arrange & Act
    final var request = new PlanRequest(
        "enterprise-annual",
        "Enterprise Annual",
        "Enterprise plan with unlimited features",
        "ANNUAL",
        29999,
        "EUR",
        "{\"unlimited\": true}",
        "TENANT",
        null,
        null,
        true
    );

    // Assert
    assertThat(request.billingPeriod()).isEqualTo("ANNUAL");
    assertThat(request.priceMinor()).isEqualTo(29999);
    assertThat(request.currency()).isEqualTo("EUR");
  }

  @Test
  @DisplayName("Should support record equality")
  void shouldSupportRecordEquality() {
    // Arrange
    final var request1 = new PlanRequest(
        "pro-monthly", "Professional", null, "MONTHLY", 2999, "USD", null, "TENANT", null, null, true
    );
    final var request2 = new PlanRequest(
        "pro-monthly", "Professional", null, "MONTHLY", 2999, "USD", null, "TENANT", null, null, true
    );

    // Assert
    assertThat(request1).isEqualTo(request2);
    assertThat(request1.hashCode()).isEqualTo(request2.hashCode());
  }

  @Test
  @DisplayName("Should have meaningful toString")
  void shouldHaveMeaningfulToString() {
    // Arrange
    final var request = new PlanRequest(
        "pro-monthly", "Professional", "Test Description", "MONTHLY", 2999, "USD", null, "TENANT", null, null, true
    );

    // Act
    final String toString = request.toString();

    // Assert
    assertThat(toString).contains("PlanRequest");
    assertThat(toString).contains("pro-monthly");
    assertThat(toString).contains("Professional");
    assertThat(toString).contains("Test Description");
    assertThat(toString).contains("MONTHLY");
    assertThat(toString).contains("2999");
  }

  @Test
  @DisplayName("Should create inactive plan")
  void shouldCreateInactivePlan() {
    // Arrange & Act
    final var request = new PlanRequest(
        "legacy-plan", "Legacy Plan", null, "MONTHLY", 1999, "USD", null, "TENANT", null, null, false
    );

    // Assert
    assertThat(request.active()).isFalse();
  }
}
