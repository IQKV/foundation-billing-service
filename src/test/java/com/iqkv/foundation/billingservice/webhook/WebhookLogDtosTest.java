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

package com.iqkv.foundation.billingservice.webhook;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("WebhookLogDtos Unit Tests")
class WebhookLogDtosTest {

  @Test
  @DisplayName("Should create AdminWebhookLogResponse with all fields")
  void shouldCreateAdminWebhookLogResponseWithAllFields() {
    // Arrange
    final UUID id = UUID.randomUUID();
    final String externalEventId = "evt_123";
    final String eventType = "invoice.paid";
    final String tenantKey = "tenant-123";
    final String status = "PROCESSED";
    final String errorMessage = null;
    final Instant receivedAt = Instant.now();
    final Instant processedAt = Instant.now().plusSeconds(5);

    // Act
    final var response = new WebhookLogDtos.AdminWebhookLogResponse(
        id, externalEventId, eventType, tenantKey, status, errorMessage, receivedAt, processedAt
    );

    // Assert
    assertThat(response.id()).isEqualTo(id);
    assertThat(response.externalEventId()).isEqualTo(externalEventId);
    assertThat(response.eventType()).isEqualTo(eventType);
    assertThat(response.tenantKey()).isEqualTo(tenantKey);
    assertThat(response.status()).isEqualTo(status);
    assertThat(response.errorMessage()).isNull();
    assertThat(response.receivedAt()).isEqualTo(receivedAt);
    assertThat(response.processedAt()).isEqualTo(processedAt);
  }

  @Test
  @DisplayName("Should create AdminWebhookLogResponse with error message")
  void shouldCreateAdminWebhookLogResponseWithErrorMessage() {
    // Arrange
    final UUID id = UUID.randomUUID();
    final String externalEventId = "evt_456";
    final String eventType = "invoice.payment_failed";
    final String tenantKey = "tenant-456";
    final String status = "FAILED";
    final String errorMessage = "Something went wrong";
    final Instant receivedAt = Instant.now();
    final Instant processedAt = Instant.now().plusSeconds(10);

    // Act
    final var response = new WebhookLogDtos.AdminWebhookLogResponse(
        id, externalEventId, eventType, tenantKey, status, errorMessage, receivedAt, processedAt
    );

    // Assert
    assertThat(response.id()).isEqualTo(id);
    assertThat(response.externalEventId()).isEqualTo(externalEventId);
    assertThat(response.eventType()).isEqualTo(eventType);
    assertThat(response.tenantKey()).isEqualTo(tenantKey);
    assertThat(response.status()).isEqualTo(status);
    assertThat(response.errorMessage()).isEqualTo(errorMessage);
    assertThat(response.receivedAt()).isEqualTo(receivedAt);
    assertThat(response.processedAt()).isEqualTo(processedAt);
  }

  @Test
  @DisplayName("Should create PagedWebhookLogResponse")
  void shouldCreatePagedWebhookLogResponse() {
    // Arrange
    final var log1 = new WebhookLogDtos.AdminWebhookLogResponse(
        UUID.randomUUID(), "evt_1", "invoice.paid", "tenant-1", "PROCESSED",
        null, Instant.now(), Instant.now()
    );
    final var log2 = new WebhookLogDtos.AdminWebhookLogResponse(
        UUID.randomUUID(), "evt_2", "invoice.payment_failed", "tenant-2", "FAILED",
        "Error", Instant.now(), Instant.now()
    );
    final var content = List.of(log1, log2);

    // Act
    final var response = new WebhookLogDtos.PagedWebhookLogResponse(content, 0, 20, 2L, 1);

    // Assert
    assertThat(response.content()).hasSize(2);
    assertThat(response.page()).isEqualTo(0);
    assertThat(response.size()).isEqualTo(20);
    assertThat(response.totalElements()).isEqualTo(2L);
    assertThat(response.totalPages()).isEqualTo(1);
  }

  @Test
  @DisplayName("Should create WebhookLogListQuery with defaults")
  void shouldCreateWebhookLogListQueryWithDefaults() {
    // Arrange & Act
    final var query = new WebhookLogDtos.WebhookLogListQuery(null, null, null, null, null, null, null);

    // Assert
    assertThat(query.page()).isEqualTo(0);
    assertThat(query.size()).isEqualTo(20);
    assertThat(query.sortBy()).isEqualTo("receivedAt");
    assertThat(query.sortDir()).isEqualTo("desc");
    assertThat(query.search()).isNull();
    assertThat(query.status()).isNull();
    assertThat(query.tenantKey()).isNull();
  }

  @Test
  @DisplayName("Should create WebhookLogListQuery with custom values")
  void shouldCreateWebhookLogListQueryWithCustomValues() {
    // Arrange & Act
    final var query = new WebhookLogDtos.WebhookLogListQuery(
        1, 50, "eventType", "asc", "invoice", "PROCESSED", "tenant-789"
    );

    // Assert
    assertThat(query.page()).isEqualTo(1);
    assertThat(query.size()).isEqualTo(50);
    assertThat(query.sortBy()).isEqualTo("eventType");
    assertThat(query.sortDir()).isEqualTo("asc");
    assertThat(query.search()).isEqualTo("invoice");
    assertThat(query.status()).isEqualTo("PROCESSED");
    assertThat(query.tenantKey()).isEqualTo("tenant-789");
  }

  @Test
  @DisplayName("Should support record equality for AdminWebhookLogResponse")
  void shouldSupportRecordEqualityForAdminWebhookLogResponse() {
    // Arrange
    final UUID id = UUID.randomUUID();
    final Instant now = Instant.now();
    final var log1 = new WebhookLogDtos.AdminWebhookLogResponse(
        id, "evt_1", "invoice.paid", "tenant-1", "PROCESSED", null, now, now
    );
    final var log2 = new WebhookLogDtos.AdminWebhookLogResponse(
        id, "evt_1", "invoice.paid", "tenant-1", "PROCESSED", null, now, now
    );

    // Assert
    assertThat(log1).isEqualTo(log2);
    assertThat(log1.hashCode()).isEqualTo(log2.hashCode());
  }

  @Test
  @DisplayName("Should have meaningful toString for AdminWebhookLogResponse")
  void shouldHaveMeaningfulToStringForAdminWebhookLogResponse() {
    // Arrange
    final var response = new WebhookLogDtos.AdminWebhookLogResponse(
        UUID.randomUUID(), "evt_123", "invoice.paid", "tenant-123", "PROCESSED",
        null, Instant.now(), Instant.now()
    );

    // Act
    final String toString = response.toString();

    // Assert
    assertThat(toString).contains("AdminWebhookLogResponse");
    assertThat(toString).contains("evt_123");
    assertThat(toString).contains("invoice.paid");
    assertThat(toString).contains("tenant-123");
    assertThat(toString).contains("PROCESSED");
  }
}
