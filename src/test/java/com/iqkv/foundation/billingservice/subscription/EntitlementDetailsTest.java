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

package com.iqkv.foundation.billingservice.subscription;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("EntitlementDetails Unit Tests")
class EntitlementDetailsTest {

  @Test
  @DisplayName("Should create EntitlementDetails with all fields")
  void shouldCreateEntitlementDetailsWithAllFields() {
    // Arrange
    final SubscriptionSubject subject = new SubscriptionSubject(SubjectType.TENANT, "tenant-123");
    final String planId = "price_123";
    final String status = "active";
    final Instant periodEnd = Instant.now().plusSeconds(2592000);
    final String featureSet = "{\"feature1\": true, \"feature2\": false}";

    // Act
    final var details = new EntitlementDetails(subject, planId, status, periodEnd, featureSet);

    // Assert
    assertThat(details.subject()).isEqualTo(subject);
    assertThat(details.planId()).isEqualTo(planId);
    assertThat(details.status()).isEqualTo(status);
    assertThat(details.currentPeriodEnd()).isEqualTo(periodEnd);
    assertThat(details.featureSet()).isEqualTo(featureSet);
  }

  @Test
  @DisplayName("Should create EntitlementDetails with null featureSet")
  void shouldCreateEntitlementDetailsWithNullFeatureSet() {
    // Arrange
    final SubscriptionSubject subject = new SubscriptionSubject(SubjectType.USER, "user-456");
    final Instant periodEnd = Instant.now().plusSeconds(2592000);

    // Act
    final var details = new EntitlementDetails(subject, "price_legacy", "trialing", periodEnd, null);

    // Assert
    assertThat(details.subject().type()).isEqualTo(SubjectType.USER);
    assertThat(details.subject().key()).isEqualTo("user-456");
    assertThat(details.featureSet()).isNull();
  }

  @Test
  @DisplayName("Should support record equality")
  void shouldSupportRecordEquality() {
    // Arrange
    final SubscriptionSubject subject = new SubscriptionSubject(SubjectType.TENANT, "tenant-123");
    final Instant periodEnd = Instant.now();
    final var details1 = new EntitlementDetails(subject, "price_123", "active", periodEnd, null);
    final var details2 = new EntitlementDetails(subject, "price_123", "active", periodEnd, null);

    // Assert
    assertThat(details1).isEqualTo(details2);
    assertThat(details1.hashCode()).isEqualTo(details2.hashCode());
  }

  @Test
  @DisplayName("Should have meaningful toString")
  void shouldHaveMeaningfulToString() {
    // Arrange
    final SubscriptionSubject subject = new SubscriptionSubject(SubjectType.TENANT, "tenant-123");
    final var details = new EntitlementDetails(
        subject, "price_123", "active", Instant.now(), "{\"feature1\": true}"
    );

    // Act
    final String toString = details.toString();

    // Assert
    assertThat(toString).contains("EntitlementDetails");
    assertThat(toString).contains("price_123");
    assertThat(toString).contains("active");
  }

  @Test
  @DisplayName("Should create EntitlementDetails for different statuses")
  void shouldCreateEntitlementDetailsForDifferentStatuses() {
    // Arrange
    final SubscriptionSubject subject = new SubscriptionSubject(SubjectType.TENANT, "tenant-123");
    final Instant periodEnd = Instant.now();

    // Act
    final var activeDetails = new EntitlementDetails(subject, "price_123", "active", periodEnd, null);
    final var trialingDetails = new EntitlementDetails(subject, "price_123", "trialing", periodEnd, null);
    final var pastDueDetails = new EntitlementDetails(subject, "price_123", "past_due", periodEnd, null);

    // Assert
    assertThat(activeDetails.status()).isEqualTo("active");
    assertThat(trialingDetails.status()).isEqualTo("trialing");
    assertThat(pastDueDetails.status()).isEqualTo("past_due");
  }
}
