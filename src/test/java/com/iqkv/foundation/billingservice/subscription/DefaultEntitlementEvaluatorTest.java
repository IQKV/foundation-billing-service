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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.iqkv.foundation.billingservice.infrastructure.persistence.PlanMapper;
import com.iqkv.foundation.billingservice.infrastructure.persistence.SubscriptionMapper;
import com.iqkv.foundation.billingservice.plan.Plan;
import com.iqkv.foundation.billingservice.plan.PlanEntitlement;
import com.iqkv.foundation.billingservice.plan.PlanFeature;
import com.iqkv.foundation.billingservice.plan.PlanFeatureRegistry;
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

  @Mock
  private PlanFeatureRegistry planFeatureRegistry;

  private MeterRegistry meterRegistry;
  private DefaultEntitlementEvaluator entitlementEvaluator;

  @BeforeEach
  void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    entitlementEvaluator = new DefaultEntitlementEvaluator(
        subscriptionMapper,
        planMapper,
        planFeatureRegistry,
        meterRegistry
    );
  }

  @Test
  @DisplayName("Should return entitlements with typed entitlement when active subscription and known plan exist")
  void shouldReturnEntitlementsWithTypedFeaturesWhenPlanFound() {
    final SubscriptionSubject subject = new SubscriptionSubject(SubjectType.TENANT, "tenant-123");
    final Subscription subscription = createSubscription("price_pro123", "active");
    final Plan plan = createPlan("pro-monthly");
    final PlanEntitlement expectedEntitlement = new PlanEntitlement(50, 0, Map.of(
        "priority_support", new PlanFeature("priority_support", "Priority Support", "true", "Access to priority support channel")
    ), null);

    when(subscriptionMapper.findActiveBySubject("TENANT", "tenant-123")).thenReturn(Optional.of(subscription));
    when(planMapper.findByExternalPriceId("price_pro123")).thenReturn(Optional.of(plan));
    when(planFeatureRegistry.resolveEntitlement("pro-monthly")).thenReturn(expectedEntitlement);

    final Optional<EntitlementDetails> result = entitlementEvaluator.evaluateEntitlements(subject);

    assertThat(result).isPresent();
    assertThat(result.get().subject()).isEqualTo(subject);
    assertThat(result.get().planCode()).isEqualTo("pro-monthly");
    assertThat(result.get().status()).isEqualTo("active");
    assertThat(result.get().planEntitlement()).isEqualTo(expectedEntitlement);
    assertThat(result.get().planEntitlement().has("priority_support")).isTrue();
    verify(subscriptionMapper).findActiveBySubject("TENANT", "tenant-123");
    verify(planMapper).findByExternalPriceId("price_pro123");
    verify(planFeatureRegistry).resolveEntitlement("pro-monthly");
  }

  @Test
  @DisplayName("Should fall back to NONE entitlement when plan not in registry")
  void shouldFallBackToNoneFeaturesWhenPlanNotInRegistry() {
    final SubscriptionSubject subject = new SubscriptionSubject(SubjectType.USER, "user-456");
    final Subscription subscription = createSubscription("price_legacy", "active");

    when(subscriptionMapper.findActiveBySubject("USER", "user-456")).thenReturn(Optional.of(subscription));
    when(planMapper.findByExternalPriceId("price_legacy")).thenReturn(Optional.empty());
    when(planMapper.findByPlanCode("price_legacy")).thenReturn(Optional.empty());
    when(planFeatureRegistry.resolveEntitlement("price_legacy")).thenReturn(PlanEntitlement.NONE);

    final Optional<EntitlementDetails> result = entitlementEvaluator.evaluateEntitlements(subject);

    assertThat(result).isPresent();
    assertThat(result.get().planCode()).isEqualTo("price_legacy"); // raw ID fallback
    assertThat(result.get().planEntitlement()).isEqualTo(PlanEntitlement.NONE);
  }

  @Test
  @DisplayName("Should return free plan when no active subscription exists")
  void shouldReturnFreePlanWhenNoActiveSubscription() {
    final SubscriptionSubject subject = new SubscriptionSubject(SubjectType.TENANT, "tenant-999");
    when(subscriptionMapper.findActiveBySubject("TENANT", "tenant-999")).thenReturn(Optional.empty());

    final Optional<EntitlementDetails> result = entitlementEvaluator.evaluateEntitlements(subject);

    assertThat(result).isPresent();
    assertThat(result.get().subject()).isEqualTo(subject);
    assertThat(result.get().planCode()).isEqualTo("free");
    assertThat(result.get().status()).isEqualTo("active");
    assertThat(result.get().currentPeriodEnd()).isNull();
    assertThat(result.get().planEntitlement()).isEqualTo(PlanEntitlement.NONE);
    verify(subscriptionMapper).findActiveBySubject("TENANT", "tenant-999");
    verifyNoInteractions(planMapper, planFeatureRegistry);
  }

  @Test
  @DisplayName("Should return NONE entitlement when planId is null")
  void shouldReturnNoneFeaturesWhenPlanIdIsNull() {
    final SubscriptionSubject subject = new SubscriptionSubject(SubjectType.TENANT, "tenant-123");
    final Subscription subscription = createSubscription(null, "trialing");

    when(subscriptionMapper.findActiveBySubject("TENANT", "tenant-123")).thenReturn(Optional.of(subscription));
    when(planFeatureRegistry.resolveEntitlement(null)).thenReturn(PlanEntitlement.NONE);

    final Optional<EntitlementDetails> result = entitlementEvaluator.evaluateEntitlements(subject);

    assertThat(result).isPresent();
    assertThat(result.get().planCode()).isNull();
    assertThat(result.get().planEntitlement()).isEqualTo(PlanEntitlement.NONE);
    verifyNoInteractions(planMapper);
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

  private Plan createPlan(final String planCode) {
    final var plan = new Plan();
    plan.setId(UUID.randomUUID());
    plan.setPlanCode(planCode);
    plan.setDisplayName("Test Plan");
    plan.setScope("TENANT");
    return plan;
  }
}
