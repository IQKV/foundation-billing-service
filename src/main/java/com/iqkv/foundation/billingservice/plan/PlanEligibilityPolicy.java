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

import com.iqkv.foundation.billingservice.subscription.SubjectType;

/**
 * Validates that a plan is eligible for assignment to the given subscription subject type.
 *
 * <p>A plan is eligible when its {@code scope} matches the active {@link SubjectType}
 * (e.g. a {@code TENANT}-scoped plan can only be assigned to a {@code TENANT} subject).
 */
public interface PlanEligibilityPolicy {

  /**
   * Validates that the plan identified by {@code planCode} is eligible for the given
   * {@code subjectType}.
   *
   * @param planCode    the plan code to validate
   * @param subjectType the active subscription subject type (TENANT or USER)
   * @throws PlanNotFoundException       if no plan with the given {@code planCode} exists
   * @throws PlanScopeMismatchException  if the plan's scope does not match {@code subjectType}
   */
  void validatePlanEligibility(String planCode, SubjectType subjectType);
}
