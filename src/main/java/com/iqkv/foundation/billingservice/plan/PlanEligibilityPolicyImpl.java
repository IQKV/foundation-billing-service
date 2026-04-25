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

import com.iqkv.foundation.billingservice.infrastructure.persistence.PlanMapper;
import com.iqkv.foundation.billingservice.subscription.SubjectType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Default implementation of {@link PlanEligibilityPolicy}.
 *
 * <p>Queries the {@code plan_catalog} table via {@link PlanMapper} and validates
 * that the plan's {@code scope} matches the active {@link SubjectType}.
 */
@Service
public class PlanEligibilityPolicyImpl implements PlanEligibilityPolicy {

  private static final Logger log = LoggerFactory.getLogger(PlanEligibilityPolicyImpl.class);

  private final PlanMapper planMapper;

  public PlanEligibilityPolicyImpl(final PlanMapper planMapper) {
    this.planMapper = planMapper;
  }

  @Override
  public void validatePlanEligibility(final String planCode, final SubjectType subjectType) {
    final Plan plan = planMapper.findByPlanCode(planCode)
        .orElseThrow(() -> new PlanNotFoundException(planCode));

    if (!subjectType.name().equals(plan.getScope())) {
      log.warn("Plan scope mismatch: planCode={} planScope={} subjectType={}",
          planCode, plan.getScope(), subjectType);
      throw new PlanScopeMismatchException(planCode, subjectType);
    }

    log.debug("Plan eligibility validated: planCode={} subjectType={}", planCode, subjectType);
  }
}
