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

package com.iqkv.foundation.billingservice.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MessagingConfigurationProperties Unit Tests")
class MessagingConfigurationPropertiesTest {

  @Test
  @DisplayName("Should create properties with RabbitMQ enabled")
  void shouldCreatePropertiesWithRabbitMqEnabled() {
    // Arrange
    final var rabbitmq = new MessagingConfigurationProperties.Rabbitmq(true);

    // Act
    final var properties = new MessagingConfigurationProperties(rabbitmq);

    // Assert
    assertThat(properties.rabbitmq()).isEqualTo(rabbitmq);
    assertThat(properties.rabbitmq().enabled()).isTrue();
  }

  @Test
  @DisplayName("Should create properties with RabbitMQ disabled")
  void shouldCreatePropertiesWithRabbitMqDisabled() {
    // Arrange
    final var rabbitmq = new MessagingConfigurationProperties.Rabbitmq(false);

    // Act
    final var properties = new MessagingConfigurationProperties(rabbitmq);

    // Assert
    assertThat(properties.rabbitmq().enabled()).isFalse();
  }

  @Test
  @DisplayName("Should support record equality for Rabbitmq")
  void shouldSupportRecordEqualityForRabbitmq() {
    // Arrange
    final var rabbitmq1 = new MessagingConfigurationProperties.Rabbitmq(true);
    final var rabbitmq2 = new MessagingConfigurationProperties.Rabbitmq(true);

    // Assert
    assertThat(rabbitmq1).isEqualTo(rabbitmq2);
    assertThat(rabbitmq1.hashCode()).isEqualTo(rabbitmq2.hashCode());
  }

  @Test
  @DisplayName("Should support record equality for MessagingConfigurationProperties")
  void shouldSupportRecordEqualityForProperties() {
    // Arrange
    final var rabbitmq = new MessagingConfigurationProperties.Rabbitmq(true);
    final var properties1 = new MessagingConfigurationProperties(rabbitmq);
    final var properties2 = new MessagingConfigurationProperties(rabbitmq);

    // Assert
    assertThat(properties1).isEqualTo(properties2);
    assertThat(properties1.hashCode()).isEqualTo(properties2.hashCode());
  }

  @Test
  @DisplayName("Should have meaningful toString")
  void shouldHaveMeaningfulToString() {
    // Arrange
    final var rabbitmq = new MessagingConfigurationProperties.Rabbitmq(true);
    final var properties = new MessagingConfigurationProperties(rabbitmq);

    // Act
    final String toString = properties.toString();

    // Assert
    assertThat(toString).contains("MessagingConfigurationProperties");
    assertThat(toString).contains("Rabbitmq");
  }
}
