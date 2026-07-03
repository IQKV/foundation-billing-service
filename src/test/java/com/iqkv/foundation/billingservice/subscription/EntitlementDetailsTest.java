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
import java.util.Map;

import com.iqkv.foundation.billingservice.plan.PlanEntitlement;
import com.iqkv.foundation.billingservice.plan.PlanFeature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("EntitlementDetails Unit Tests")
class EntitlementDetailsTest {

  @Test
  @DisplayName("Should create EntitlementDetails with all fields")
  void shouldCreateEntitlementDetailsWithAllFields() {
    final SubscriptionSubject subject = new SubscriptionSubject(SubjectType.TENANT, "tenant-123");
    final String planCode = "pro-monthly";
    final String status = "active";
    final Instant periodEnd = Instant.now().plusSeconds(2592000);
    final PlanEntitlement planEntitlement = new PlanEntitlement(50, 0, Map.of(
        "priority_support", new PlanFeature("priority_support", "Priority Support", "true", "Access to priority support channel")
    ));

    final var details = new EntitlementDetails(subject, planCode, status, periodEnd, planEntitlement);

    assertThat(details.subject()).isEqualTo(subject);
    assertThat(details.planCode()).isEqualTo(planCode);
    assertThat(details.status()).isEqualTo(status);
    assertThat(details.currentPeriodEnd()).isEqualTo(periodEnd);
    assertThat(details.features()).isEqualTo(features);
    assertThat(details.features().has("priority_support")).isTrue();
    assertThat(details.features().maxUsers()).isEqualTo(50);
  }

  @Test
  @DisplayName("Should use NONE entitlement as fallback")
  void shouldUseNoneFeaturesAsFallback() {
    final SubscriptionSubject subject = new SubscriptionSubject(SubjectType.USER, "user-456");
    final Instant periodEnd = Instant.now().plusSeconds(2592000);

    final var details = new EntitlementDetails(subject, "basic-monthly", "trialing", periodEnd, PlanEntitlement.NONE);

    assertThat(details.subject().type()).isEqualTo(SubjectType.USER);
    assertThat(details.subject().key()).isEqualTo("user-456");
    assertThat(details.features()).isEqualTo(PlanEntitlement.NONE);
    assertThat(details.features().has("priority_support")).isFalse();
    assertThat(details.features().maxUsers()).isEqualTo(1);
  }

  @Test
  @DisplayName("Should support record equality")
  void shouldSupportRecordEquality() {
    final SubscriptionSubject subject = new SubscriptionSubject(SubjectType.TENANT, "tenant-123");
    final Instant periodEnd = Instant.now();
    final PlanEntitlement planEntitlement = new PlanEntitlement(5, 3, Map.of());
    final var details1 = new EntitlementDetails(subject, "basic-monthly", "active", periodEnd, planEntitlement);
    final var details2 = new EntitlementDetails(subject, "basic-monthly", "active", periodEnd, planEntitlement);

    assertThat(details1).isEqualTo(details2);
    assertThat(details1.hashCode()).isEqualTo(details2.hashCode());
  }

  @Test
  @DisplayName("Should have meaningful toString")
  void shouldHaveMeaningfulToString() {
    final SubscriptionSubject subject = new SubscriptionSubject(SubjectType.TENANT, "tenant-123");
    final var details = new EntitlementDetails(
        subject, "pro-monthly", "active", Instant.now(), PlanEntitlement.NONE
    );

    final String toString = details.toString();

    assertThat(toString).contains("EntitlementDetails");
    assertThat(toString).contains("pro-monthly");
    assertThat(toString).contains("active");
  }

  @Test
  @DisplayName("Should create EntitlementDetails for different subscription statuses")
  void shouldCreateEntitlementDetailsForDifferentStatuses() {
    final SubscriptionSubject subject = new SubscriptionSubject(SubjectType.TENANT, "tenant-123");
    final Instant periodEnd = Instant.now();
    final PlanEntitlement planEntitlement = PlanEntitlement.NONE;

    final var activeDetails = new EntitlementDetails(subject, "pro-monthly", "active", periodEnd, planEntitlement);
    final var trialingDetails = new EntitlementDetails(subject, "pro-monthly", "trialing", periodEnd, planEntitlement);
    final var pastDueDetails = new EntitlementDetails(subject, "pro-monthly", "past_due", periodEnd, planEntitlement);

    assertThat(activeDetails.status()).isEqualTo("active");
    assertThat(trialingDetails.status()).isEqualTo("trialing");
    assertThat(pastDueDetails.status()).isEqualTo("past_due");
  }
}
