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
    final Long quantity = 5L;
    final Instant trialStart = Instant.now();
    final Instant trialEnd = Instant.now().plusSeconds(86400 * 7);
    final Instant periodStart = Instant.now();
    final Instant periodEnd = Instant.now().plusSeconds(2592000);
    final Instant canceledAt = Instant.now();

    // Act
    final var response = new SubscriptionDtos.SubscriptionResponse(
        id, tenantKey, externalSubscriptionId, "cus_123", status, planId,
        quantity, trialStart, trialEnd, false, null,
        periodStart, periodEnd, true, canceledAt, "STRIPE", "order_123"
    );

    // Assert
    assertThat(response.id()).isEqualTo(id);
    assertThat(response.tenantKey()).isEqualTo(tenantKey);
    assertThat(response.externalSubscriptionId()).isEqualTo(externalSubscriptionId);
    assertThat(response.externalCustomerId()).isEqualTo("cus_123");
    assertThat(response.status()).isEqualTo(status);
    assertThat(response.planId()).isEqualTo(planId);
    assertThat(response.quantity()).isEqualTo(quantity);
    assertThat(response.trialStart()).isEqualTo(trialStart);
    assertThat(response.trialEnd()).isEqualTo(trialEnd);
    assertThat(response.currentPeriodStart()).isEqualTo(periodStart);
    assertThat(response.currentPeriodEnd()).isEqualTo(periodEnd);
    assertThat(response.cancelAtPeriodEnd()).isTrue();
    assertThat(response.canceledAt()).isEqualTo(canceledAt);
    assertThat(response.gatewayType()).isEqualTo("STRIPE");
    assertThat(response.externalOrderId()).isEqualTo("order_123");
  }

  @Test
  @DisplayName("Should create SubscriptionResponse with null optional fields")
  void shouldCreateSubscriptionResponseWithNullOptionalFields() {
    // Arrange & Act
    final var response = new SubscriptionDtos.SubscriptionResponse(
        UUID.randomUUID(), "tenant-123", "sub_abc", null, "active", "price_123",
        null, null, null, false, null,
        Instant.now(), Instant.now().plusSeconds(2592000), false, null, null, null
    );

    // Assert
    assertThat(response.quantity()).isNull();
    assertThat(response.trialStart()).isNull();
    assertThat(response.trialEnd()).isNull();
    assertThat(response.cancelAtPeriodEnd()).isFalse();
    assertThat(response.canceledAt()).isNull();
    assertThat(response.externalCustomerId()).isNull();
    assertThat(response.gatewayType()).isNull();
    assertThat(response.externalOrderId()).isNull();
  }

  @Test
  @DisplayName("Should support record equality")
  void shouldSupportRecordEquality() {
    // Arrange
    final UUID id = UUID.randomUUID();
    final Instant now = Instant.now();
    final var response1 = new SubscriptionDtos.SubscriptionResponse(
        id, "tenant-123", "sub_abc", null, "active", "price_123",
        5L, null, null, false, null,
        now, now.plusSeconds(2592000), false, null, null, null
    );
    final var response2 = new SubscriptionDtos.SubscriptionResponse(
        id, "tenant-123", "sub_abc", null, "active", "price_123",
        5L, null, null, false, null,
        now, now.plusSeconds(2592000), false, null, null, null
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
        UUID.randomUUID(), "tenant-123", "sub_abc", null, "active", "price_123",
        1L, null, null, false, null,
        Instant.now(), Instant.now().plusSeconds(2592000), false, null, null, null
    );

    // Act
    final String toString = response.toString();

    // Assert
    assertThat(toString).contains("SubscriptionResponse");
    assertThat(toString).contains("tenant-123");
    assertThat(toString).contains("sub_abc");
    assertThat(toString).contains("active");
  }

  @Test
  @DisplayName("Should create CreateCheckoutSessionRequest with all fields")
  void shouldCreateCheckoutSessionRequest() {
    // Arrange & Act
    final var request = new SubscriptionDtos.CreateCheckoutSessionRequest(
        "plan-free", "https://example.com/success", "https://example.com/cancel",
        14, 2L, true
    );

    // Assert
    assertThat(request.planCode()).isEqualTo("plan-free");
    assertThat(request.successUrl()).isEqualTo("https://example.com/success");
    assertThat(request.cancelUrl()).isEqualTo("https://example.com/cancel");
    assertThat(request.trialPeriodDays()).isEqualTo(14);
    assertThat(request.quantity()).isEqualTo(2L);
    assertThat(request.allowPromotionCodes()).isTrue();
  }

  @Test
  @DisplayName("Should create CheckoutSessionResponse")
  void shouldCreateCheckoutSessionResponse() {
    // Arrange & Act
    final var response = new SubscriptionDtos.CheckoutSessionResponse("https://checkout.stripe.com/session_123");

    // Assert
    assertThat(response.checkoutUrl()).isEqualTo("https://checkout.stripe.com/session_123");
  }

  @Test
  @DisplayName("Should create UpdateSubscriptionRequest")
  void shouldCreateUpdateSubscriptionRequest() {
    // Arrange & Act
    final var request = new SubscriptionDtos.UpdateSubscriptionRequest(
        "plan-pro", 5L, "always_invoice"
    );

    // Assert
    assertThat(request.planCode()).isEqualTo("plan-pro");
    assertThat(request.quantity()).isEqualTo(5L);
    assertThat(request.prorationBehavior()).isEqualTo("always_invoice");
  }

  @Test
  @DisplayName("Should create CreateRefundRequest")
  void shouldCreateRefundRequest() {
    // Arrange & Act
    final var request = new SubscriptionDtos.CreateRefundRequest(
        "ch_123", 1000L, "requested_by_customer"
    );

    // Assert
    assertThat(request.paymentId()).isEqualTo("ch_123");
    assertThat(request.amount()).isEqualTo(1000L);
    assertThat(request.reason()).isEqualTo("requested_by_customer");
  }

  @Test
  @DisplayName("Should create RefundResponse")
  void shouldCreateRefundResponse() {
    // Arrange & Act
    final var response = new SubscriptionDtos.RefundResponse("re_123");

    // Assert
    assertThat(response.refundId()).isEqualTo("re_123");
  }

  @Test
  @DisplayName("Should create AdminRefundResponse with all fields")
  void shouldCreateAdminRefundResponse() {
    // Arrange
    final UUID id = UUID.randomUUID();
    final Instant occurredAt = Instant.now();
    final java.time.LocalDateTime createdAt = java.time.LocalDateTime.now();
    final java.time.LocalDateTime updatedAt = java.time.LocalDateTime.now();

    // Act
    final var response = new SubscriptionDtos.AdminRefundResponse(
        id, "tenant-123", "re_ext123", "ch_ext456", "cus_ext789",
        1500L, "usd", "succeeded", occurredAt, createdAt, updatedAt
    );

    // Assert
    assertThat(response.id()).isEqualTo(id);
    assertThat(response.tenantKey()).isEqualTo("tenant-123");
    assertThat(response.externalRefundId()).isEqualTo("re_ext123");
    assertThat(response.externalPaymentId()).isEqualTo("ch_ext456");
    assertThat(response.externalCustomerId()).isEqualTo("cus_ext789");
    assertThat(response.amount()).isEqualTo(1500L);
    assertThat(response.currency()).isEqualTo("usd");
    assertThat(response.status()).isEqualTo("succeeded");
  }

  @Test
  @DisplayName("Should create PagedRefundResponse")
  void shouldCreatePagedRefundResponse() {
    // Arrange
    final var refund1 = new SubscriptionDtos.AdminRefundResponse(
        UUID.randomUUID(), "tenant-123", "re_1", "ch_1", "cus_1",
        1000L, "usd", "succeeded", Instant.now(),
        java.time.LocalDateTime.now(), java.time.LocalDateTime.now()
    );
    final var content = java.util.List.of(refund1);

    // Act
    final var response = new SubscriptionDtos.PagedRefundResponse(content, 0, 20, 1L, 1);

    // Assert
    assertThat(response.content()).hasSize(1);
    assertThat(response.page()).isEqualTo(0);
    assertThat(response.size()).isEqualTo(20);
    assertThat(response.totalElements()).isEqualTo(1L);
    assertThat(response.totalPages()).isEqualTo(1);
  }

  @Test
  @DisplayName("Should create RefundListQuery with defaults")
  void shouldCreateRefundListQueryWithDefaults() {
    // Arrange & Act
    final var query = new SubscriptionDtos.RefundListQuery(null, null, null, null, null);

    // Assert
    assertThat(query.page()).isEqualTo(0);
    assertThat(query.size()).isEqualTo(20);
    assertThat(query.sortBy()).isEqualTo("occurredAt");
    assertThat(query.sortDir()).isEqualTo("desc");
    assertThat(query.tenantKey()).isNull();
  }

  @Test
  @DisplayName("Should create RefundListQuery with custom values")
  void shouldCreateRefundListQueryWithCustomValues() {
    // Arrange & Act
    final var query = new SubscriptionDtos.RefundListQuery(2, 50, "amount", "asc", "tenant-456");

    // Assert
    assertThat(query.page()).isEqualTo(2);
    assertThat(query.size()).isEqualTo(50);
    assertThat(query.sortBy()).isEqualTo("amount");
    assertThat(query.sortDir()).isEqualTo("asc");
    assertThat(query.tenantKey()).isEqualTo("tenant-456");
  }

  @Test
  @DisplayName("Should create AdminSubscriptionResponse")
  void shouldCreateAdminSubscriptionResponse() {
    // Arrange
    final UUID id = UUID.randomUUID();
    final Instant now = Instant.now();
    final java.time.LocalDateTime created = java.time.LocalDateTime.now();

    // Act
    final var response = new SubscriptionDtos.AdminSubscriptionResponse(
        id, "tenant-123", "sub_ext123", "cus_ext123", "active", "price_123", 2L,
        null, null, false, null, now, now.plusSeconds(2592000), false, null,
        "TENANT", "tenant-123", "STRIPE", "order_123", created, created
    );

    // Assert
    assertThat(response.id()).isEqualTo(id);
    assertThat(response.subjectType()).isEqualTo("TENANT");
    assertThat(response.subjectKey()).isEqualTo("tenant-123");
    assertThat(response.externalCustomerId()).isEqualTo("cus_ext123");
    assertThat(response.gatewayType()).isEqualTo("STRIPE");
    assertThat(response.externalOrderId()).isEqualTo("order_123");
  }

  @Test
  @DisplayName("Should create AdminUpdateSubscriptionRequest")
  void shouldCreateAdminUpdateSubscriptionRequest() {
    // Arrange & Act
    final var request = new SubscriptionDtos.AdminUpdateSubscriptionRequest(
        "paused", 3L, "price_new", null, null, null, true
    );

    // Assert
    assertThat(request.status()).isEqualTo("paused");
    assertThat(request.quantity()).isEqualTo(3L);
    assertThat(request.planId()).isEqualTo("price_new");
    assertThat(request.cancelAtPeriodEnd()).isTrue();
  }

  @Test
  @DisplayName("Should create SubscriptionCountResponse")
  void shouldCreateSubscriptionCountResponse() {
    // Arrange & Act
    final var response = new SubscriptionDtos.SubscriptionCountResponse(42L);

    // Assert
    assertThat(response.total()).isEqualTo(42L);
  }

  @Test
  @DisplayName("Should create PagedSubscriptionResponse")
  void shouldCreatePagedSubscriptionResponse() {
    // Arrange
    final var subscription = new SubscriptionDtos.AdminSubscriptionResponse(
        UUID.randomUUID(), "tenant-123", "sub_1", null, "active", "price_1", 1L,
        null, null, false, null, Instant.now(), Instant.now(), false, null,
        "TENANT", "tenant-123", null, null, java.time.LocalDateTime.now(), java.time.LocalDateTime.now()
    );
    final var content = java.util.List.of(subscription);

    // Act
    final var response = new SubscriptionDtos.PagedSubscriptionResponse(content, 0, 20, 1L, 1);

    // Assert
    assertThat(response.content()).hasSize(1);
    assertThat(response.totalElements()).isEqualTo(1L);
  }

  @Test
  @DisplayName("Should create SubscriptionListQuery with defaults")
  void shouldCreateSubscriptionListQueryWithDefaults() {
    // Arrange & Act
    final var query = new SubscriptionDtos.SubscriptionListQuery(
        null, null, null, null, null, null, null
    );

    // Assert
    assertThat(query.page()).isEqualTo(0);
    assertThat(query.size()).isEqualTo(20);
    assertThat(query.sortBy()).isEqualTo("createdAt");
    assertThat(query.sortDir()).isEqualTo("desc");
  }

  @Test
  @DisplayName("Should create SubscriptionListQuery with custom values")
  void shouldCreateSubscriptionListQueryWithCustomValues() {
    // Arrange & Act
    final var query = new SubscriptionDtos.SubscriptionListQuery(
        1, 50, "status", "asc", "search-term", "active", "tenant-789"
    );

    // Assert
    assertThat(query.page()).isEqualTo(1);
    assertThat(query.size()).isEqualTo(50);
    assertThat(query.sortBy()).isEqualTo("status");
    assertThat(query.sortDir()).isEqualTo("asc");
    assertThat(query.search()).isEqualTo("search-term");
    assertThat(query.status()).isEqualTo("active");
    assertThat(query.tenantKey()).isEqualTo("tenant-789");
  }
}
