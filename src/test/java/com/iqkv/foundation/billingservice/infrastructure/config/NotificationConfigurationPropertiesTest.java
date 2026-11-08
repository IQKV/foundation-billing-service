/*
 * Copyright 2026 iQKV Foundation Team.
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

@DisplayName("NotificationConfigurationProperties Unit Tests")
class NotificationConfigurationPropertiesTest {

  @Test
  @DisplayName("Should create properties with all fields")
  void shouldCreatePropertiesWithAllFields() {
    // Act
    final var properties = new NotificationConfigurationProperties(
        "en", "https://example.com"
    );

    // Assert
    assertThat(properties.defaultLocale()).isEqualTo("en");
    assertThat(properties.baseUrl()).isEqualTo("https://example.com");
  }

  @Test
  @DisplayName("Should support locale with country code")
  void shouldSupportLocaleWithCountryCode() {
    // Act
    final var properties = new NotificationConfigurationProperties(
        "en-US", "https://example.com"
    );

    // Assert
    assertThat(properties.defaultLocale()).isEqualTo("en-US");
  }

  @Test
  @DisplayName("Should support http and https URLs")
  void shouldSupportHttpAndHttpsUrls() {
    // Act
    final var httpsProperties = new NotificationConfigurationProperties(
        "en", "https://example.com"
    );
    final var httpProperties = new NotificationConfigurationProperties(
        "en", "http://localhost:8080"
    );

    // Assert
    assertThat(httpsProperties.baseUrl()).isEqualTo("https://example.com");
    assertThat(httpProperties.baseUrl()).isEqualTo("http://localhost:8080");
  }

  @Test
  @DisplayName("Should support record equality")
  void shouldSupportRecordEquality() {
    // Arrange
    final var properties1 = new NotificationConfigurationProperties(
        "en", "https://example.com"
    );
    final var properties2 = new NotificationConfigurationProperties(
        "en", "https://example.com"
    );

    // Assert
    assertThat(properties1).isEqualTo(properties2);
    assertThat(properties1.hashCode()).isEqualTo(properties2.hashCode());
  }

  @Test
  @DisplayName("Should have meaningful toString")
  void shouldHaveMeaningfulToString() {
    // Arrange
    final var properties = new NotificationConfigurationProperties(
        "en", "https://example.com"
    );

    // Act
    final String toString = properties.toString();

    // Assert
    assertThat(toString).contains("NotificationConfigurationProperties");
    assertThat(toString).contains("en");
    assertThat(toString).contains("https://example.com");
  }
}
