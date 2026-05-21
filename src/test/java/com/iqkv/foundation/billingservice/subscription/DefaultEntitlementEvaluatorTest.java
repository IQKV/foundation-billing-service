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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.iqkv.foundation.billingservice.infrastructure.persistence.PlanMapper;
import com.iqkv.foundation.billingservice.infrastructure.persistence.SubscriptionMapper;
import com.iqkv.foundation.billingservice.plan.Plan;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DefaultEntitlementEvaluator Unit Tests")
class DefaultEntitlementEvaluatorTest {

  @Mock
  private SubscriptionMapper subscriptionMapper;

  @Mock
  private PlanMapper planMapper;

  private MeterRegistry meterRegistry;

  private DefaultEntitlementEvaluator entitlementEvaluator;

  @BeforeEach
  void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    entitlementEvaluator = new DefaultEntitlementEvaluator(
        subscriptionMapper,
        planMapper,
        meterRegistry
    );
  }

  @Test
  @DisplayName("Should return entitlement details when active subscription exists with plan")
  void shouldReturnEntitlementDetailsWithPlan() {
    // Arrange
    final SubscriptionSubject subject = new SubscriptionSubject(SubjectType.TENANT, "tenant-123");
    final Subscription subscription = createSubscription("price_123", "active");
    final Plan plan = createPlan("price_123", "{\"feature1\": true, \"feature2\": false}");

    when(subscriptionMapper.findActiveBySubject("TENANT", "tenant-123")).thenReturn(Optional.of(subscription));
    when(planMapper.findByPlanCode("price_123")).thenReturn(Optional.of(plan));

    // Act
    final Optional<EntitlementDetails> result = entitlementEvaluator.evaluateEntitlements(subject);

    // Assert
    assertThat(result).isPresent();
    assertThat(result.get().subject()).isEqualTo(subject);
    assertThat(result.get().planId()).isEqualTo("price_123");
    assertThat(result.get().status()).isEqualTo("active");
    assertThat(result.get().featureSet()).isEqualTo("{\"feature1\": true, \"feature2\": false}");
    verify(subscriptionMapper).findActiveBySubject("TENANT", "tenant-123");
    verify(planMapper).findByPlanCode("price_123");
  }

  @Test
  @DisplayName("Should return entitlement details with null feature set when plan not found")
  void shouldReturnEntitlementDetailsWithNullFeatureSetWhenPlanNotFound() {
    // Arrange
    final SubscriptionSubject subject = new SubscriptionSubject(SubjectType.USER, "user-456");
    final Subscription subscription = createSubscription("price_legacy", "active");

    when(subscriptionMapper.findActiveBySubject("USER", "user-456")).thenReturn(Optional.of(subscription));
    when(planMapper.findByPlanCode("price_legacy")).thenReturn(Optional.empty());

    // Act
    final Optional<EntitlementDetails> result = entitlementEvaluator.evaluateEntitlements(subject);

    // Assert
    assertThat(result).isPresent();
    assertThat(result.get().subject()).isEqualTo(subject);
    assertThat(result.get().planId()).isEqualTo("price_legacy");
    assertThat(result.get().status()).isEqualTo("active");
    assertThat(result.get().featureSet()).isNull();
    verify(subscriptionMapper).findActiveBySubject("USER", "user-456");
    verify(planMapper).findByPlanCode("price_legacy");
  }

  @Test
  @DisplayName("Should return empty when no active subscription exists")
  void shouldReturnEmptyWhenNoActiveSubscription() {
    // Arrange
    final SubscriptionSubject subject = new SubscriptionSubject(SubjectType.TENANT, "tenant-999");
    when(subscriptionMapper.findActiveBySubject("TENANT", "tenant-999")).thenReturn(Optional.empty());

    // Act
    final Optional<EntitlementDetails> result = entitlementEvaluator.evaluateEntitlements(subject);

    // Assert
    assertThat(result).isEmpty();
    verify(subscriptionMapper).findActiveBySubject("TENANT", "tenant-999");
  }

  @Test
  @DisplayName("Should return entitlement details with null feature set when plan ID is null")
  void shouldReturnEntitlementDetailsWithNullFeatureSetWhenPlanIdIsNull() {
    // Arrange
    final SubscriptionSubject subject = new SubscriptionSubject(SubjectType.TENANT, "tenant-123");
    final Subscription subscription = createSubscription(null, "trialing");

    when(subscriptionMapper.findActiveBySubject("TENANT", "tenant-123")).thenReturn(Optional.of(subscription));

    // Act
    final Optional<EntitlementDetails> result = entitlementEvaluator.evaluateEntitlements(subject);

    // Assert
    assertThat(result).isPresent();
    assertThat(result.get().planId()).isNull();
    assertThat(result.get().featureSet()).isNull();
    verify(subscriptionMapper).findActiveBySubject("TENANT", "tenant-123");
  }

  private Subscription createSubscription(final String planId, final String status) {
    final var subscription = new Subscription();
    subscription.setId(UUID.randomUUID());
    subscription.setExternalSubscriptionId("sub_" + UUID.randomUUID());
    subscription.setPlanId(planId);
    subscription.setStatus(status);
    subscription.setCurrentPeriodEnd(Instant.now().plusSeconds(2592000));
    return subscription;
  }

  private Plan createPlan(final String planCode, final String featureSet) {
    final var plan = new Plan();
    plan.setId(UUID.randomUUID());
    plan.setPlanCode(planCode);
    plan.setDisplayName("Test Plan");
    plan.setScope("TENANT");
    plan.setFeatureSet(featureSet);
    return plan;
  }
}
