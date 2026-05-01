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
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SubscriptionDtos Unit Tests")
class SubscriptionDtosTest {

  @Test
  @DisplayName("Should create SubscriptionResponse with all fields")
  void shouldCreateSubscriptionResponseWithAllFields() {
    // Arrange
    final UUID id = UUID.randomUUID();
    final String tenantKey = "tenant-123";
    final String externalSubscriptionId = "sub_abc123";
    final String status = "active";
    final String planId = "price_123";
    final Instant periodStart = Instant.now();
    final Instant periodEnd = Instant.now().plusSeconds(2592000);
    final Instant canceledAt = Instant.now();

    // Act
    final var response = new SubscriptionDtos.SubscriptionResponse(
        id, tenantKey, externalSubscriptionId, status, planId,
        periodStart, periodEnd, true, canceledAt
    );

    // Assert
    assertThat(response.id()).isEqualTo(id);
    assertThat(response.tenantKey()).isEqualTo(tenantKey);
    assertThat(response.externalSubscriptionId()).isEqualTo(externalSubscriptionId);
    assertThat(response.status()).isEqualTo(status);
    assertThat(response.planId()).isEqualTo(planId);
    assertThat(response.currentPeriodStart()).isEqualTo(periodStart);
    assertThat(response.currentPeriodEnd()).isEqualTo(periodEnd);
    assertThat(response.cancelAtPeriodEnd()).isTrue();
    assertThat(response.canceledAt()).isEqualTo(canceledAt);
  }

  @Test
  @DisplayName("Should create SubscriptionResponse with null canceledAt")
  void shouldCreateSubscriptionResponseWithNullCanceledAt() {
    // Arrange & Act
    final var response = new SubscriptionDtos.SubscriptionResponse(
        UUID.randomUUID(), "tenant-123", "sub_abc", "active", "price_123",
        Instant.now(), Instant.now().plusSeconds(2592000), false, null
    );

    // Assert
    assertThat(response.cancelAtPeriodEnd()).isFalse();
    assertThat(response.canceledAt()).isNull();
  }

  @Test
  @DisplayName("Should support record equality")
  void shouldSupportRecordEquality() {
    // Arrange
    final UUID id = UUID.randomUUID();
    final Instant now = Instant.now();
    final var response1 = new SubscriptionDtos.SubscriptionResponse(
        id, "tenant-123", "sub_abc", "active", "price_123",
        now, now.plusSeconds(2592000), false, null
    );
    final var response2 = new SubscriptionDtos.SubscriptionResponse(
        id, "tenant-123", "sub_abc", "active", "price_123",
        now, now.plusSeconds(2592000), false, null
    );

    // Assert
    assertThat(response1).isEqualTo(response2);
    assertThat(response1.hashCode()).isEqualTo(response2.hashCode());
  }

  @Test
  @DisplayName("Should have meaningful toString")
  void shouldHaveMeaningfulToString() {
    // Arrange
    final var response = new SubscriptionDtos.SubscriptionResponse(
        UUID.randomUUID(), "tenant-123", "sub_abc", "active", "price_123",
        Instant.now(), Instant.now().plusSeconds(2592000), false, null
    );

    // Act
    final String toString = response.toString();

    // Assert
    assertThat(toString).contains("SubscriptionResponse");
    assertThat(toString).contains("tenant-123");
    assertThat(toString).contains("sub_abc");
    assertThat(toString).contains("active");
  }
}
