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

package com.iqkv.foundation.billingservice.subscription;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SubscriptionSubject Unit Tests")
class SubscriptionSubjectTest {

  @Test
  @DisplayName("Should create SubscriptionSubject with TENANT type")
  void shouldCreateSubscriptionSubjectWithTenantType() {
    // Arrange & Act
    final var subject = new SubscriptionSubject(SubjectType.TENANT, "tenant-123");

    // Assert
    assertThat(subject.type()).isEqualTo(SubjectType.TENANT);
    assertThat(subject.key()).isEqualTo("tenant-123");
  }

  @Test
  @DisplayName("Should create SubscriptionSubject with USER type")
  void shouldCreateSubscriptionSubjectWithUserType() {
    // Arrange & Act
    final var subject = new SubscriptionSubject(SubjectType.USER, "user-456");

    // Assert
    assertThat(subject.type()).isEqualTo(SubjectType.USER);
    assertThat(subject.key()).isEqualTo("user-456");
  }

  @Test
  @DisplayName("Should support record equality")
  void shouldSupportRecordEquality() {
    // Arrange
    final var subject1 = new SubscriptionSubject(SubjectType.TENANT, "tenant-123");
    final var subject2 = new SubscriptionSubject(SubjectType.TENANT, "tenant-123");

    // Assert
    assertThat(subject1).isEqualTo(subject2);
    assertThat(subject1.hashCode()).isEqualTo(subject2.hashCode());
  }

  @Test
  @DisplayName("Should not be equal when types differ")
  void shouldNotBeEqualWhenTypesDiffer() {
    // Arrange
    final var tenantSubject = new SubscriptionSubject(SubjectType.TENANT, "key-123");
    final var userSubject = new SubscriptionSubject(SubjectType.USER, "key-123");

    // Assert
    assertThat(tenantSubject).isNotEqualTo(userSubject);
  }

  @Test
  @DisplayName("Should not be equal when keys differ")
  void shouldNotBeEqualWhenKeysDiffer() {
    // Arrange
    final var subject1 = new SubscriptionSubject(SubjectType.TENANT, "tenant-123");
    final var subject2 = new SubscriptionSubject(SubjectType.TENANT, "tenant-456");

    // Assert
    assertThat(subject1).isNotEqualTo(subject2);
  }

  @Test
  @DisplayName("Should have meaningful toString")
  void shouldHaveMeaningfulToString() {
    // Arrange
    final var subject = new SubscriptionSubject(SubjectType.TENANT, "tenant-123");

    // Act
    final String toString = subject.toString();

    // Assert
    assertThat(toString).contains("SubscriptionSubject");
    assertThat(toString).contains("TENANT");
    assertThat(toString).contains("tenant-123");
  }
}
