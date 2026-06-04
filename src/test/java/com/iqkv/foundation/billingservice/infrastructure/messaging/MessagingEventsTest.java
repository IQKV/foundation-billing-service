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

package com.iqkv.foundation.billingservice.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Messaging Events Unit Tests")
class MessagingEventsTest {

  // ─── SubscriptionEvent ────────────────────────────────────────────────────

  @Test
  @DisplayName("Should create SubscriptionEvent via all-args constructor")
  void shouldCreateSubscriptionEvent() {
    // Arrange
    final var now = Instant.now();

    // Act
    final var event = new SubscriptionEvent(
        "tenant-123", "sub_ext456",
        SubscriptionEvent.EventType.SUBSCRIPTION_CREATED, now,
        "TENANT", "tenant-123"
    );

    // Assert
    assertThat(event.getTenantKey()).isEqualTo("tenant-123");
    assertThat(event.getExternalSubscriptionId()).isEqualTo("sub_ext456");
    assertThat(event.getEventType()).isEqualTo(SubscriptionEvent.EventType.SUBSCRIPTION_CREATED);
    assertThat(event.getOccurredAt()).isEqualTo(now);
    assertThat(event.getSubjectType()).isEqualTo("TENANT");
    assertThat(event.getSubjectKey()).isEqualTo("tenant-123");
  }

  @Test
  @DisplayName("Should create SubscriptionEvent via no-args constructor and setters")
  void shouldCreateSubscriptionEventViaSetters() {
    // Arrange
    final var now = Instant.now();
    final var event = new SubscriptionEvent();

    // Act
    event.setTenantKey("tenant-456");
    event.setExternalSubscriptionId("sub_ext789");
    event.setEventType(SubscriptionEvent.EventType.SUBSCRIPTION_CANCELLED);
    event.setOccurredAt(now);
    event.setSubjectType("USER");
    event.setSubjectKey("user-uuid-123");

    // Assert
    assertThat(event.getTenantKey()).isEqualTo("tenant-456");
    assertThat(event.getExternalSubscriptionId()).isEqualTo("sub_ext789");
    assertThat(event.getEventType()).isEqualTo(SubscriptionEvent.EventType.SUBSCRIPTION_CANCELLED);
    assertThat(event.getSubjectType()).isEqualTo("USER");
    assertThat(event.getSubjectKey()).isEqualTo("user-uuid-123");
  }

  @Test
  @DisplayName("Should have all SubscriptionEvent.EventType values")
  void shouldHaveAllSubscriptionEventTypes() {
    assertThat(SubscriptionEvent.EventType.values()).containsExactlyInAnyOrder(
        SubscriptionEvent.EventType.SUBSCRIPTION_CREATED,
        SubscriptionEvent.EventType.SUBSCRIPTION_CANCELLED
    );
  }

  // ─── InvoiceEvent ─────────────────────────────────────────────────────────

  @Test
  @DisplayName("Should create InvoiceEvent via all-args constructor")
  void shouldCreateInvoiceEvent() {
    // Arrange
    final var now = Instant.now();

    // Act
    final var event = new InvoiceEvent(
        "tenant-123", "in_ext456", "cus_ext789", "sub_extabc",
        InvoiceEvent.EventType.INVOICE_PAID, 5000L, "usd",
        now, "TENANT", "tenant-123"
    );

    // Assert
    assertThat(event.getTenantKey()).isEqualTo("tenant-123");
    assertThat(event.getExternalInvoiceId()).isEqualTo("in_ext456");
    assertThat(event.getExternalCustomerId()).isEqualTo("cus_ext789");
    assertThat(event.getExternalSubscriptionId()).isEqualTo("sub_extabc");
    assertThat(event.getEventType()).isEqualTo(InvoiceEvent.EventType.INVOICE_PAID);
    assertThat(event.getAmountPaid()).isEqualTo(5000L);
    assertThat(event.getCurrency()).isEqualTo("usd");
    assertThat(event.getSubjectType()).isEqualTo("TENANT");
  }

  @Test
  @DisplayName("Should create InvoiceEvent via no-args constructor and setters")
  void shouldCreateInvoiceEventViaSetters() {
    // Arrange
    final var event = new InvoiceEvent();

    // Act
    event.setTenantKey("tenant-789");
    event.setExternalInvoiceId("in_abc");
    event.setExternalCustomerId("cus_abc");
    event.setExternalSubscriptionId("sub_abc");
    event.setEventType(InvoiceEvent.EventType.INVOICE_CREATED);
    event.setAmountPaid(1000L);
    event.setCurrency("eur");
    event.setOccurredAt(Instant.now());
    event.setSubjectType("USER");
    event.setSubjectKey("user-abc");

    // Assert
    assertThat(event.getEventType()).isEqualTo(InvoiceEvent.EventType.INVOICE_CREATED);
    assertThat(event.getCurrency()).isEqualTo("eur");
    assertThat(event.getSubjectType()).isEqualTo("USER");
  }

  @Test
  @DisplayName("Should have all InvoiceEvent.EventType values")
  void shouldHaveAllInvoiceEventTypes() {
    assertThat(InvoiceEvent.EventType.values()).containsExactlyInAnyOrder(
        InvoiceEvent.EventType.INVOICE_PAID,
        InvoiceEvent.EventType.INVOICE_CREATED,
        InvoiceEvent.EventType.INVOICE_FINALIZED,
        InvoiceEvent.EventType.INVOICE_UPDATED
    );
  }

  // ─── RefundEvent ──────────────────────────────────────────────────────────

  @Test
  @DisplayName("Should create RefundEvent via all-args constructor")
  void shouldCreateRefundEvent() {
    // Arrange
    final var now = Instant.now();

    // Act
    final var event = new RefundEvent(
        "tenant-123", "re_ext456", "ch_ext789", "cus_extabc",
        RefundEvent.EventType.REFUND_CREATED, 1500L, "usd",
        "succeeded", now, "TENANT", "tenant-123"
    );

    // Assert
    assertThat(event.getTenantKey()).isEqualTo("tenant-123");
    assertThat(event.getExternalRefundId()).isEqualTo("re_ext456");
    assertThat(event.getExternalPaymentId()).isEqualTo("ch_ext789");
    assertThat(event.getExternalCustomerId()).isEqualTo("cus_extabc");
    assertThat(event.getEventType()).isEqualTo(RefundEvent.EventType.REFUND_CREATED);
    assertThat(event.getAmountRefunded()).isEqualTo(1500L);
    assertThat(event.getCurrency()).isEqualTo("usd");
    assertThat(event.getStatus()).isEqualTo("succeeded");
    assertThat(event.getSubjectType()).isEqualTo("TENANT");
    assertThat(event.getSubjectKey()).isEqualTo("tenant-123");
  }

  @Test
  @DisplayName("Should create RefundEvent via no-args constructor and setters")
  void shouldCreateRefundEventViaSetters() {
    // Arrange
    final var event = new RefundEvent();

    // Act
    event.setTenantKey("tenant-setters");
    event.setExternalRefundId("re_setters");
    event.setExternalPaymentId("ch_setters");
    event.setExternalCustomerId("cus_setters");
    event.setEventType(RefundEvent.EventType.REFUND_CREATED);
    event.setAmountRefunded(2000L);
    event.setCurrency("gbp");
    event.setStatus("pending");
    event.setOccurredAt(Instant.now());
    event.setSubjectType("USER");
    event.setSubjectKey("user-setters");

    // Assert
    assertThat(event.getAmountRefunded()).isEqualTo(2000L);
    assertThat(event.getCurrency()).isEqualTo("gbp");
    assertThat(event.getStatus()).isEqualTo("pending");
  }

  // ─── PaymentEvent ─────────────────────────────────────────────────────────

  @Test
  @DisplayName("Should create PaymentEvent via all-args constructor")
  void shouldCreatePaymentEvent() {
    // Arrange
    final var now = Instant.now();

    // Act
    final var event = new PaymentEvent(
        "tenant-123", "in_ext456", "cus_ext789", "sub_extabc",
        PaymentEvent.EventType.PAYMENT_FAILED, 3000L, "usd",
        "insufficient_funds", now, "TENANT", "tenant-123"
    );

    // Assert
    assertThat(event.getTenantKey()).isEqualTo("tenant-123");
    assertThat(event.getExternalInvoiceId()).isEqualTo("in_ext456");
    assertThat(event.getExternalCustomerId()).isEqualTo("cus_ext789");
    assertThat(event.getExternalSubscriptionId()).isEqualTo("sub_extabc");
    assertThat(event.getEventType()).isEqualTo(PaymentEvent.EventType.PAYMENT_FAILED);
    assertThat(event.getAmountDue()).isEqualTo(3000L);
    assertThat(event.getCurrency()).isEqualTo("usd");
    assertThat(event.getFailureReason()).isEqualTo("insufficient_funds");
    assertThat(event.getSubjectType()).isEqualTo("TENANT");
  }

  @Test
  @DisplayName("Should create PaymentEvent via no-args constructor and setters")
  void shouldCreatePaymentEventViaSetters() {
    // Arrange
    final var event = new PaymentEvent();

    // Act
    event.setTenantKey("tenant-pay");
    event.setExternalInvoiceId("in_pay");
    event.setExternalCustomerId("cus_pay");
    event.setExternalSubscriptionId("sub_pay");
    event.setEventType(PaymentEvent.EventType.PAYMENT_FAILED);
    event.setAmountDue(999L);
    event.setCurrency("chf");
    event.setFailureReason("card_declined");
    event.setOccurredAt(Instant.now());
    event.setSubjectType("USER");
    event.setSubjectKey("user-pay");
    event.setActor(null);

    // Assert
    assertThat(event.getAmountDue()).isEqualTo(999L);
    assertThat(event.getFailureReason()).isEqualTo("card_declined");
    assertThat(event.getActor()).isNull();
  }

  // ─── TenantEvent ──────────────────────────────────────────────────────────

  @Test
  @DisplayName("Should create TenantEvent via all-args constructor")
  void shouldCreateTenantEvent() {
    // Arrange
    final var now = Instant.now();

    // Act
    final var event = new TenantEvent(
        "tenant-123", "Acme Corp", "owner@acme.com",
        "John", TenantEvent.EventType.TENANT_CREATED, now
    );

    // Assert
    assertThat(event.getTenantKey()).isEqualTo("tenant-123");
    assertThat(event.getTenantName()).isEqualTo("Acme Corp");
    assertThat(event.getOwnerEmail()).isEqualTo("owner@acme.com");
    assertThat(event.getOwnerFirstName()).isEqualTo("John");
    assertThat(event.getEventType()).isEqualTo(TenantEvent.EventType.TENANT_CREATED);
    assertThat(event.getOccurredAt()).isEqualTo(now);
  }

  @Test
  @DisplayName("Should create TenantEvent via no-args constructor and setters")
  void shouldCreateTenantEventViaSetters() {
    // Arrange
    final var event = new TenantEvent();

    // Act
    event.setTenantKey("tenant-s");
    event.setTenantName("Setters Corp");
    event.setOwnerEmail(null);
    event.setOwnerFirstName(null);
    event.setEventType(TenantEvent.EventType.TENANT_DELETED);
    event.setOccurredAt(Instant.now());

    // Assert
    assertThat(event.getTenantKey()).isEqualTo("tenant-s");
    assertThat(event.getEventType()).isEqualTo(TenantEvent.EventType.TENANT_DELETED);
    assertThat(event.getOwnerEmail()).isNull();
  }

  @Test
  @DisplayName("Should have all TenantEvent.EventType values")
  void shouldHaveAllTenantEventTypes() {
    assertThat(TenantEvent.EventType.values()).containsExactlyInAnyOrder(
        TenantEvent.EventType.TENANT_CREATED,
        TenantEvent.EventType.TENANT_PROVISIONED,
        TenantEvent.EventType.TENANT_PROVISIONING_FAILED,
        TenantEvent.EventType.TENANT_UPDATED,
        TenantEvent.EventType.TENANT_DELETED,
        TenantEvent.EventType.TENANT_SUSPENDED
    );
  }

  // ─── UserEvent ────────────────────────────────────────────────────────────

  @Test
  @DisplayName("Should create UserEvent via all-args constructor")
  void shouldCreateUserEvent() {
    // Arrange
    final var userId = UUID.randomUUID();
    final var now = Instant.now();

    // Act
    final var event = new UserEvent(
        userId, "tenant-123", "user@example.com",
        UserEvent.EventType.USER_CREATED, now
    );

    // Assert
    assertThat(event.getUserId()).isEqualTo(userId);
    assertThat(event.getTenantId()).isEqualTo("tenant-123");
    assertThat(event.getEmail()).isEqualTo("user@example.com");
    assertThat(event.getEventType()).isEqualTo(UserEvent.EventType.USER_CREATED);
    assertThat(event.getOccurredAt()).isEqualTo(now);
  }

  @Test
  @DisplayName("Should create UserEvent via no-args constructor and setters")
  void shouldCreateUserEventViaSetters() {
    // Arrange
    final var userId = UUID.randomUUID();
    final var event = new UserEvent();

    // Act
    event.setUserId(userId);
    event.setTenantId("tenant-s");
    event.setEmail("setter@example.com");
    event.setEventType(UserEvent.EventType.USER_DELETED);
    event.setOccurredAt(Instant.now());

    // Assert
    assertThat(event.getUserId()).isEqualTo(userId);
    assertThat(event.getEventType()).isEqualTo(UserEvent.EventType.USER_DELETED);
    assertThat(event.getEmail()).isEqualTo("setter@example.com");
  }

  @Test
  @DisplayName("Should have all UserEvent.EventType values")
  void shouldHaveAllUserEventTypes() {
    assertThat(UserEvent.EventType.values()).containsExactlyInAnyOrder(
        UserEvent.EventType.USER_CREATED,
        UserEvent.EventType.USER_UPDATED,
        UserEvent.EventType.USER_DELETED,
        UserEvent.EventType.USER_REMOVED
    );
  }

  // ─── NotificationEvent ────────────────────────────────────────────────────

  @Test
  @DisplayName("Should create NotificationEvent via all-args constructor")
  void shouldCreateNotificationEvent() {
    // Arrange
    final var now = Instant.now();
    final var payload = Map.<String, Object>of("subscriptionId", "sub_123", "daysLeft", 3);

    // Act
    final var event = new NotificationEvent(
        "recipient@example.com", "en",
        NotificationEventType.TRIAL_ENDING, payload, now
    );

    // Assert
    assertThat(event.getRecipientEmail()).isEqualTo("recipient@example.com");
    assertThat(event.getLocale()).isEqualTo("en");
    assertThat(event.getType()).isEqualTo(NotificationEventType.TRIAL_ENDING);
    assertThat(event.getPayload()).containsEntry("subscriptionId", "sub_123");
    assertThat(event.getOccurredAt()).isEqualTo(now);
  }

  @Test
  @DisplayName("Should create NotificationEvent via no-args constructor and setters")
  void shouldCreateNotificationEventViaSetters() {
    // Arrange
    final var event = new NotificationEvent();

    // Act
    event.setRecipientEmail("setter@example.com");
    event.setLocale("fr");
    event.setType(NotificationEventType.PAYMENT_OVERDUE);
    event.setPayload(Map.of("invoiceId", "in_456"));
    event.setOccurredAt(Instant.now());

    // Assert
    assertThat(event.getRecipientEmail()).isEqualTo("setter@example.com");
    assertThat(event.getLocale()).isEqualTo("fr");
    assertThat(event.getType()).isEqualTo(NotificationEventType.PAYMENT_OVERDUE);
    assertThat(event.getPayload()).containsEntry("invoiceId", "in_456");
  }
}
