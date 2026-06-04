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

package com.iqkv.foundation.billingservice.gateway.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Gateway Commands Unit Tests")
class GatewayCommandsTest {

  // ─── CreateCustomerCommand Tests ───────────────────────────────────────────

  @Test
  @DisplayName("Should create CreateCustomerCommand with all fields")
  void shouldCreateCustomerCommandWithAllFields() {
    // Arrange
    final Map<String, String> metadata = Map.of("tenantKey", "tenant-123", "tier", "premium");

    // Act
    final var command = new CreateCustomerCommand("Acme Corp", "billing@acme.com", metadata);

    // Assert
    assertThat(command.name()).isEqualTo("Acme Corp");
    assertThat(command.email()).isEqualTo("billing@acme.com");
    assertThat(command.metadata()).containsEntry("tenantKey", "tenant-123");
    assertThat(command.metadata()).containsEntry("tier", "premium");
  }

  @Test
  @DisplayName("Should create CreateCustomerCommand with null email")
  void shouldCreateCustomerCommandWithNullEmail() {
    // Arrange & Act
    final var command = new CreateCustomerCommand("Acme Corp", null, null);

    // Assert
    assertThat(command.name()).isEqualTo("Acme Corp");
    assertThat(command.email()).isNull();
    assertThat(command.metadata()).isEmpty();
  }

  @Test
  @DisplayName("Should throw exception when name is null")
  void shouldThrowExceptionWhenNameIsNull() {
    // Arrange & Act & Assert
    assertThatThrownBy(() -> new CreateCustomerCommand(null, "email@example.com", null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Customer name is required");
  }

  @Test
  @DisplayName("Should throw exception when name is blank")
  void shouldThrowExceptionWhenNameIsBlank() {
    // Arrange & Act & Assert
    assertThatThrownBy(() -> new CreateCustomerCommand("   ", "email@example.com", null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Customer name is required");
  }

  @Test
  @DisplayName("Should create immutable copy of metadata for CreateCustomerCommand")
  void shouldCreateImmutableCopyOfMetadataForCustomer() {
    // Arrange
    final var mutableMetadata = new java.util.HashMap<String, String>();
    mutableMetadata.put("key1", "value1");

    // Act
    final var command = new CreateCustomerCommand("Test", "test@test.com", mutableMetadata);
    mutableMetadata.put("key2", "value2");

    // Assert
    assertThat(command.metadata()).containsOnlyKeys("key1");
    assertThat(command.metadata()).doesNotContainKey("key2");
  }

  // ─── CreateCheckoutSessionCommand Tests ────────────────────────────────────

  @Test
  @DisplayName("Should create CreateCheckoutSessionCommand with all fields")
  void shouldCreateCheckoutSessionCommandWithAllFields() {
    // Arrange
    final Map<String, String> metadata = Map.of("campaign", "summer2024");

    // Act
    final var command = new CreateCheckoutSessionCommand(
        "cus_123", "price_456",
        "https://example.com/success", "https://example.com/cancel",
        14, 3L, true, metadata
    );

    // Assert
    assertThat(command.customerId()).isEqualTo("cus_123");
    assertThat(command.priceId()).isEqualTo("price_456");
    assertThat(command.successUrl()).isEqualTo("https://example.com/success");
    assertThat(command.cancelUrl()).isEqualTo("https://example.com/cancel");
    assertThat(command.trialPeriodDays()).isEqualTo(14);
    assertThat(command.quantity()).isEqualTo(3L);
    assertThat(command.allowPromotionCodes()).isTrue();
    assertThat(command.metadata()).containsEntry("campaign", "summer2024");
  }

  @Test
  @DisplayName("Should create CreateCheckoutSessionCommand with required fields only")
  void shouldCreateCheckoutSessionCommandWithRequiredFieldsOnly() {
    // Arrange & Act
    final var command = new CreateCheckoutSessionCommand(
        "cus_789", "price_101",
        "https://example.com/success", "https://example.com/cancel",
        null, null, null, null
    );

    // Assert
    assertThat(command.customerId()).isEqualTo("cus_789");
    assertThat(command.priceId()).isEqualTo("price_101");
    assertThat(command.trialPeriodDays()).isNull();
    assertThat(command.quantity()).isNull();
    assertThat(command.allowPromotionCodes()).isNull();
    assertThat(command.metadata()).isEmpty();
  }

  @Test
  @DisplayName("Should throw exception when customerId is null")
  void shouldThrowExceptionWhenCustomerIdIsNull() {
    // Arrange & Act & Assert
    assertThatThrownBy(() -> new CreateCheckoutSessionCommand(
        null, "price_123", "https://success.com", "https://cancel.com", null, null, null, null
    ))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Customer ID is required");
  }

  @Test
  @DisplayName("Should throw exception when priceId is blank")
  void shouldThrowExceptionWhenPriceIdIsBlank() {
    // Arrange & Act & Assert
    assertThatThrownBy(() -> new CreateCheckoutSessionCommand(
        "cus_123", "  ", "https://success.com", "https://cancel.com", null, null, null, null
    ))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Price ID is required");
  }

  @Test
  @DisplayName("Should throw exception when successUrl is null")
  void shouldThrowExceptionWhenSuccessUrlIsNull() {
    // Arrange & Act & Assert
    assertThatThrownBy(() -> new CreateCheckoutSessionCommand(
        "cus_123", "price_123", null, "https://cancel.com", null, null, null, null
    ))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Success URL is required");
  }

  @Test
  @DisplayName("Should throw exception when cancelUrl is blank")
  void shouldThrowExceptionWhenCancelUrlIsBlank() {
    // Arrange & Act & Assert
    assertThatThrownBy(() -> new CreateCheckoutSessionCommand(
        "cus_123", "price_123", "https://success.com", "", null, null, null, null
    ))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Cancel URL is required");
  }

  // ─── UpdateSubscriptionCommand Tests ───────────────────────────────────────

  @Test
  @DisplayName("Should create UpdateSubscriptionCommand with all fields")
  void shouldCreateUpdateSubscriptionCommandWithAllFields() {
    // Arrange
    final Map<String, String> metadata = Map.of("reason", "upgrade");

    // Act
    final var command = new UpdateSubscriptionCommand(
        "sub_123", "price_new", 5L, "always_invoice", metadata
    );

    // Assert
    assertThat(command.subscriptionId()).isEqualTo("sub_123");
    assertThat(command.priceId()).isEqualTo("price_new");
    assertThat(command.quantity()).isEqualTo(5L);
    assertThat(command.prorationBehavior()).isEqualTo("always_invoice");
    assertThat(command.metadata()).containsEntry("reason", "upgrade");
  }

  @Test
  @DisplayName("Should create UpdateSubscriptionCommand with only subscription ID")
  void shouldCreateUpdateSubscriptionCommandWithOnlySubscriptionId() {
    // Arrange & Act
    final var command = new UpdateSubscriptionCommand("sub_456", null, null, null, null);

    // Assert
    assertThat(command.subscriptionId()).isEqualTo("sub_456");
    assertThat(command.priceId()).isNull();
    assertThat(command.quantity()).isNull();
    assertThat(command.prorationBehavior()).isNull();
    assertThat(command.metadata()).isEmpty();
  }

  @Test
  @DisplayName("Should throw exception when subscriptionId is null")
  void shouldThrowExceptionWhenSubscriptionIdIsNull() {
    // Arrange & Act & Assert
    assertThatThrownBy(() -> new UpdateSubscriptionCommand(null, "price_123", 1L, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Subscription ID is required");
  }

  @Test
  @DisplayName("Should throw exception when subscriptionId is blank")
  void shouldThrowExceptionWhenSubscriptionIdIsBlank() {
    // Arrange & Act & Assert
    assertThatThrownBy(() -> new UpdateSubscriptionCommand("", "price_123", 1L, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Subscription ID is required");
  }

  // ─── CreateRefundCommand Tests ─────────────────────────────────────────────

  @Test
  @DisplayName("Should create CreateRefundCommand with all fields")
  void shouldCreateRefundCommandWithAllFields() {
    // Arrange
    final Map<String, String> metadata = Map.of("processor", "stripe");

    // Act
    final var command = new CreateRefundCommand(
        "ch_123", "cus_456", 1500L, "requested_by_customer", metadata
    );

    // Assert
    assertThat(command.paymentId()).isEqualTo("ch_123");
    assertThat(command.externalCustomerId()).isEqualTo("cus_456");
    assertThat(command.amount()).isEqualTo(1500L);
    assertThat(command.reason()).isEqualTo("requested_by_customer");
    assertThat(command.metadata()).containsEntry("processor", "stripe");
  }

  @Test
  @DisplayName("Should create CreateRefundCommand using alternate constructor")
  void shouldCreateRefundCommandUsingAlternateConstructor() {
    // Arrange
    final Map<String, String> metadata = Map.of("type", "full");

    // Act
    final var command = new CreateRefundCommand("ch_789", 2000L, "duplicate", metadata);

    // Assert
    assertThat(command.paymentId()).isEqualTo("ch_789");
    assertThat(command.externalCustomerId()).isNull();
    assertThat(command.amount()).isEqualTo(2000L);
    assertThat(command.reason()).isEqualTo("duplicate");
  }

  @Test
  @DisplayName("Should create CreateRefundCommand for full refund")
  void shouldCreateRefundCommandForFullRefund() {
    // Arrange & Act
    final var command = new CreateRefundCommand("ch_999", null, null, null, null);

    // Assert
    assertThat(command.paymentId()).isEqualTo("ch_999");
    assertThat(command.amount()).isNull();
    assertThat(command.reason()).isNull();
    assertThat(command.metadata()).isEmpty();
  }

  @Test
  @DisplayName("Should throw exception when paymentId is null")
  void shouldThrowExceptionWhenPaymentIdIsNull() {
    // Arrange & Act & Assert
    assertThatThrownBy(() -> new CreateRefundCommand(null, "cus_123", 1000L, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Payment ID is required");
  }

  @Test
  @DisplayName("Should throw exception when paymentId is blank")
  void shouldThrowExceptionWhenPaymentIdIsBlank() {
    // Arrange & Act & Assert
    assertThatThrownBy(() -> new CreateRefundCommand("  ", 1000L, "reason", null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Payment ID is required");
  }

  @Test
  @DisplayName("Should create immutable copy of metadata for all commands")
  void shouldCreateImmutableCopyOfMetadataForAllCommands() {
    // Arrange
    final var mutableMetadata = new java.util.HashMap<String, String>();
    mutableMetadata.put("original", "value");

    // Act
    final var refundCommand = new CreateRefundCommand("ch_123", 1000L, null, mutableMetadata);
    mutableMetadata.put("modified", "value");

    // Assert
    assertThat(refundCommand.metadata()).containsOnlyKeys("original");
  }
}
