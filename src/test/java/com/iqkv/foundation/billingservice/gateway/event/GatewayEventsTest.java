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

package com.iqkv.foundation.billingservice.gateway.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Gateway Events Unit Tests")
class GatewayEventsTest {

  // ─── GatewaySubscriptionEvent ─────────────────────────────────────────────

  @Test
  @DisplayName("isCreated should return true for subscription.created event type")
  void shouldDetectCreatedEvent() {
    // Arrange
    final var event = subscriptionEvent("customer.subscription.created");

    // Assert
    assertThat(event.isCreated()).isTrue();
    assertThat(event.isUpdated()).isFalse();
    assertThat(event.isDeleted()).isFalse();
  }

  @Test
  @DisplayName("isUpdated should return true for subscription.updated event type")
  void shouldDetectUpdatedEvent() {
    // Arrange
    final var event = subscriptionEvent("customer.subscription.updated");

    // Assert
    assertThat(event.isUpdated()).isTrue();
    assertThat(event.isCreated()).isFalse();
    assertThat(event.isDeleted()).isFalse();
  }

  @Test
  @DisplayName("isDeleted should return true for subscription.deleted event type")
  void shouldDetectDeletedEvent() {
    // Arrange
    final var event = subscriptionEvent("customer.subscription.deleted");

    // Assert
    assertThat(event.isDeleted()).isTrue();
    assertThat(event.isCreated()).isFalse();
    assertThat(event.isUpdated()).isFalse();
  }

  @Test
  @DisplayName("isDeleted should return true for subscription.canceled event type")
  void shouldDetectCanceledEvent() {
    // Arrange
    final var event = subscriptionEvent("customer.subscription.canceled");

    // Assert
    assertThat(event.isDeleted()).isTrue();
  }

  @Test
  @DisplayName("Should create GatewaySubscriptionEvent and expose all fields via GatewayWebhookEvent")
  void shouldExposeGatewayWebhookEventFields() {
    // Arrange
    final var now = Instant.now();
    final var event = subscriptionEventAt("evt_123", "customer.subscription.created", now);

    // Assert — interface contract
    assertThat(event.eventId()).isEqualTo("evt_123");
    assertThat(event.eventType()).isEqualTo("customer.subscription.created");
    assertThat(event.occurredAt()).isEqualTo(now);
  }

  @Test
  @DisplayName("Should create GatewaySubscriptionEvent with all fields")
  void shouldCreateSubscriptionEventWithAllFields() {
    // Arrange
    final var now = Instant.now();
    final var periodEnd = now.plusSeconds(2592000);
    final var metadata = Map.of("tenantKey", "tenant-123");

    // Act
    final var event = new GatewaySubscriptionEvent(
        "evt_sub_1", "customer.subscription.created", now, "STRIPE",
        "sub_ext123", "cus_ext456", "active", "price_789",
        2L, now, now.plusSeconds(604800), now, periodEnd,
        false, null, metadata
    );

    // Assert
    assertThat(event.externalSubscriptionId()).isEqualTo("sub_ext123");
    assertThat(event.externalCustomerId()).isEqualTo("cus_ext456");
    assertThat(event.status()).isEqualTo("active");
    assertThat(event.planId()).isEqualTo("price_789");
    assertThat(event.quantity()).isEqualTo(2L);
    assertThat(event.cancelAtPeriodEnd()).isFalse();
    assertThat(event.canceledAt()).isNull();
    assertThat(event.metadata()).containsEntry("tenantKey", "tenant-123");
  }

  @Test
  @DisplayName("Should support record equality for GatewaySubscriptionEvent")
  void shouldSupportSubscriptionEventEquality() {
    // Arrange
    final var now = Instant.now();
    final var e1 = new GatewaySubscriptionEvent(
        "evt_1", "type", now, "STRIPE", "sub_1", "cus_1", "active", "price_1",
        1L, null, null, now, now, false, null, Map.of()
    );
    final var e2 = new GatewaySubscriptionEvent(
        "evt_1", "type", now, "STRIPE", "sub_1", "cus_1", "active", "price_1",
        1L, null, null, now, now, false, null, Map.of()
    );

    // Assert
    assertThat(e1).isEqualTo(e2);
    assertThat(e1.hashCode()).isEqualTo(e2.hashCode());
  }

  // ─── GatewayInvoiceEvent ──────────────────────────────────────────────────

  @Test
  @DisplayName("Should create GatewayInvoiceEvent with all fields")
  void shouldCreateInvoiceEvent() {
    // Arrange
    final var now = Instant.now();

    // Act
    final var event = new GatewayInvoiceEvent(
        "evt_inv_1", "invoice.payment_succeeded", now, "STRIPE",
        "in_ext123", "cus_ext456", "sub_ext789", null,
        5000L, 5000L, "usd"
    );

    // Assert
    assertThat(event.eventId()).isEqualTo("evt_inv_1");
    assertThat(event.eventType()).isEqualTo("invoice.payment_succeeded");
    assertThat(event.occurredAt()).isEqualTo(now);
    assertThat(event.externalInvoiceId()).isEqualTo("in_ext123");
    assertThat(event.externalCustomerId()).isEqualTo("cus_ext456");
    assertThat(event.externalSubscriptionId()).isEqualTo("sub_ext789");
    assertThat(event.amountPaid()).isEqualTo(5000L);
    assertThat(event.amountDue()).isEqualTo(5000L);
    assertThat(event.currency()).isEqualTo("usd");
  }

  @Test
  @DisplayName("GatewayInvoiceEvent should implement GatewayWebhookEvent")
  void shouldImplementGatewayWebhookEvent() {
    // Arrange
    final var event = new GatewayInvoiceEvent(
        "evt_1", "invoice.paid", Instant.now(), "STRIPE",
        "in_1", "cus_1", "sub_1", null, 1000L, 1000L, "usd"
    );

    // Assert
    assertThat(event).isInstanceOf(GatewayWebhookEvent.class);
  }

  // ─── GatewayRefundEvent ───────────────────────────────────────────────────

  @Test
  @DisplayName("Should create GatewayRefundEvent with all fields")
  void shouldCreateRefundEvent() {
    // Arrange
    final var now = Instant.now();

    // Act
    final var event = new GatewayRefundEvent(
        "evt_re_1", "charge.refunded", now, "STRIPE",
        "re_ext123", "ch_ext456", "cus_ext789",
        1500L, "usd", "succeeded"
    );

    // Assert
    assertThat(event.eventId()).isEqualTo("evt_re_1");
    assertThat(event.eventType()).isEqualTo("charge.refunded");
    assertThat(event.externalRefundId()).isEqualTo("re_ext123");
    assertThat(event.externalPaymentId()).isEqualTo("ch_ext456");
    assertThat(event.externalCustomerId()).isEqualTo("cus_ext789");
    assertThat(event.amountRefunded()).isEqualTo(1500L);
    assertThat(event.currency()).isEqualTo("usd");
    assertThat(event.status()).isEqualTo("succeeded");
  }

  @Test
  @DisplayName("GatewayRefundEvent should implement GatewayWebhookEvent")
  void shouldRefundImplementGatewayWebhookEvent() {
    // Arrange
    final var event = new GatewayRefundEvent(
        "evt_1", "charge.refunded", Instant.now(), "STRIPE",
        "re_1", "ch_1", "cus_1", 500L, "eur", "succeeded"
    );

    // Assert
    assertThat(event).isInstanceOf(GatewayWebhookEvent.class);
  }

  // ─── GatewayPaymentFailureEvent ───────────────────────────────────────────

  @Test
  @DisplayName("Should create GatewayPaymentFailureEvent with all fields")
  void shouldCreatePaymentFailureEvent() {
    // Arrange
    final var now = Instant.now();

    // Act
    final var event = new GatewayPaymentFailureEvent(
        "evt_pf_1", "invoice.payment_failed", now, "STRIPE",
        "in_ext123", "cus_ext456", "sub_ext789",
        2000L, "usd", "insufficient_funds"
    );

    // Assert
    assertThat(event.eventId()).isEqualTo("evt_pf_1");
    assertThat(event.eventType()).isEqualTo("invoice.payment_failed");
    assertThat(event.externalInvoiceId()).isEqualTo("in_ext123");
    assertThat(event.externalCustomerId()).isEqualTo("cus_ext456");
    assertThat(event.externalSubscriptionId()).isEqualTo("sub_ext789");
    assertThat(event.amountDue()).isEqualTo(2000L);
    assertThat(event.currency()).isEqualTo("usd");
    assertThat(event.failureReason()).isEqualTo("insufficient_funds");
  }

  @Test
  @DisplayName("GatewayPaymentFailureEvent should implement GatewayWebhookEvent")
  void shouldPaymentFailureImplementGatewayWebhookEvent() {
    // Arrange
    final var event = new GatewayPaymentFailureEvent(
        "evt_1", "invoice.payment_failed", Instant.now(), "STRIPE",
        "in_1", "cus_1", "sub_1", 1000L, "usd", "card_declined"
    );

    // Assert
    assertThat(event).isInstanceOf(GatewayWebhookEvent.class);
  }

  // ─── Helpers ──────────────────────────────────────────────────────────────

  private GatewaySubscriptionEvent subscriptionEvent(final String eventType) {
    return subscriptionEventAt("evt_test", eventType, Instant.now());
  }

  private GatewaySubscriptionEvent subscriptionEventAt(final String eventId,
                                                        final String eventType,
                                                        final Instant occurredAt) {
    final var now = Instant.now();
    return new GatewaySubscriptionEvent(
        eventId, eventType, occurredAt, "STRIPE",
        "sub_ext", "cus_ext", "active", "price_1",
        1L, null, null, now, now.plusSeconds(2592000),
        false, null, Map.of()
    );
  }
}
