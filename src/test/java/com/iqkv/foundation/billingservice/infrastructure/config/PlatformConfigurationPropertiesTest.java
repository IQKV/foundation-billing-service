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

@DisplayName("PlatformConfigurationProperties Unit Tests")
class PlatformConfigurationPropertiesTest {

  @Test
  @DisplayName("Should create properties with MULTI_TENANT mode")
  void shouldCreatePropertiesWithMultiTenantMode() {
    // Arrange & Act
    final var properties = new PlatformConfigurationProperties(RolloutMode.MULTI_TENANT);

    // Assert
    assertThat(properties.rolloutMode()).isEqualTo(RolloutMode.MULTI_TENANT);
    assertThat(properties.getRolloutModeValue()).isEqualTo("MULTI_TENANT");
  }

  @Test
  @DisplayName("Should create properties with SINGLE_TENANT mode")
  void shouldCreatePropertiesWithSingleTenantMode() {
    // Arrange & Act
    final var properties = new PlatformConfigurationProperties(RolloutMode.SINGLE_TENANT);

    // Assert
    assertThat(properties.rolloutMode()).isEqualTo(RolloutMode.SINGLE_TENANT);
    assertThat(properties.getRolloutModeValue()).isEqualTo("SINGLE_TENANT");
  }

  @Test
  @DisplayName("Should support record equality")
  void shouldSupportRecordEquality() {
    // Arrange
    final var properties1 = new PlatformConfigurationProperties(RolloutMode.MULTI_TENANT);
    final var properties2 = new PlatformConfigurationProperties(RolloutMode.MULTI_TENANT);

    // Assert
    assertThat(properties1).isEqualTo(properties2);
    assertThat(properties1.hashCode()).isEqualTo(properties2.hashCode());
  }

  @Test
  @DisplayName("Should have meaningful toString")
  void shouldHaveMeaningfulToString() {
    // Arrange
    final var properties = new PlatformConfigurationProperties(RolloutMode.MULTI_TENANT);

    // Act
    final String toString = properties.toString();

    // Assert
    assertThat(toString).contains("PlatformConfigurationProperties");
    assertThat(toString).contains("MULTI_TENANT");
  }
}
