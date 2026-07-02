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

package com.iqkv.foundation.billingservice.plan;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import com.iqkv.foundation.billingservice.infrastructure.persistence.PlanMapper;
import com.iqkv.foundation.billingservice.subscription.SubjectType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlanEligibilityPolicyImpl Unit Tests")
class PlanEligibilityPolicyImplTest {

  @Mock
  private PlanMapper planMapper;

  @InjectMocks
  private PlanEligibilityPolicyImpl planEligibilityPolicy;

  @Test
  @DisplayName("Should validate plan eligibility successfully when scope matches")
  void shouldValidatePlanEligibilitySuccessfully() {
    // Arrange
    final String planCode = "price_tenant_plan";
    final SubjectType subjectType = SubjectType.TENANT;
    final Plan plan = createPlan(planCode, "TENANT");

    when(planMapper.findByPlanCode(planCode)).thenReturn(Optional.of(plan));

    // Act & Assert
    assertThatCode(() -> planEligibilityPolicy.validatePlanEligibility(planCode, subjectType))
        .doesNotThrowAnyException();
    verify(planMapper).findByPlanCode(planCode);
  }

  @Test
  @DisplayName("Should validate plan eligibility for USER scope")
  void shouldValidatePlanEligibilityForUserScope() {
    // Arrange
    final String planCode = "price_user_plan";
    final SubjectType subjectType = SubjectType.USER;
    final Plan plan = createPlan(planCode, "USER");

    when(planMapper.findByPlanCode(planCode)).thenReturn(Optional.of(plan));

    // Act & Assert
    assertThatCode(() -> planEligibilityPolicy.validatePlanEligibility(planCode, subjectType))
        .doesNotThrowAnyException();
    verify(planMapper).findByPlanCode(planCode);
  }

  @Test
  @DisplayName("Should throw PlanNotFoundException when plan does not exist")
  void shouldThrowExceptionWhenPlanNotFound() {
    // Arrange
    final String planCode = "price_nonexistent";
    final SubjectType subjectType = SubjectType.TENANT;

    when(planMapper.findByPlanCode(planCode)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> planEligibilityPolicy.validatePlanEligibility(planCode, subjectType))
        .isInstanceOf(PlanNotFoundException.class);
    verify(planMapper).findByPlanCode(planCode);
  }

  @Test
  @DisplayName("Should throw PlanScopeMismatchException when scope does not match")
  void shouldThrowExceptionWhenScopeMismatch() {
    // Arrange
    final String planCode = "price_tenant_plan";
    final SubjectType subjectType = SubjectType.USER;
    final Plan plan = createPlan(planCode, "TENANT");

    when(planMapper.findByPlanCode(planCode)).thenReturn(Optional.of(plan));

    // Act & Assert
    assertThatThrownBy(() -> planEligibilityPolicy.validatePlanEligibility(planCode, subjectType))
        .isInstanceOf(PlanScopeMismatchException.class);
    verify(planMapper).findByPlanCode(planCode);
  }

  @Test
  @DisplayName("Should throw PlanScopeMismatchException when TENANT plan used for USER")
  void shouldThrowExceptionWhenTenantPlanUsedForUser() {
    // Arrange
    final String planCode = "price_enterprise";
    final SubjectType subjectType = SubjectType.USER;
    final Plan plan = createPlan(planCode, "TENANT");

    when(planMapper.findByPlanCode(planCode)).thenReturn(Optional.of(plan));

    // Act & Assert
    assertThatThrownBy(() -> planEligibilityPolicy.validatePlanEligibility(planCode, subjectType))
        .isInstanceOf(PlanScopeMismatchException.class);
  }

  private Plan createPlan(final String planCode, final String scope) {
    final var plan = new Plan();
    plan.setId(UUID.randomUUID());
    plan.setPlanCode(planCode);
    plan.setDisplayName("Test Plan");
    plan.setScope(scope);
    plan.setEntitlement("{\"feature1\": true}");
    return plan;
  }
}
