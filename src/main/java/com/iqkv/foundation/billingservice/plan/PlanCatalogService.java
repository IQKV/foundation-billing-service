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

import java.util.List;

import com.iqkv.foundation.billingservice.infrastructure.persistence.PlanMapper;
import com.iqkv.foundation.billingservice.shared.exception.DuplicateResourceException;
import org.springframework.stereotype.Service;

/**
 * Application service for plan catalog reads and platform-admin mutations.
 */
@Service
public class PlanCatalogService {

  private final PlanMapper planMapper;

  public PlanCatalogService(final PlanMapper planMapper) {
    this.planMapper = planMapper;
  }

  public List<Plan> listActivePlans() {
    return planMapper.findAllActive();
  }

  public List<Plan> listAllPlans() {
    return planMapper.findAll();
  }

  public Plan getPlanOrThrow(final String planCode) {
    return planMapper.findByPlanCode(planCode)
        .orElseThrow(() -> new PlanNotFoundException(planCode));
  }

  /**
   * Creates a plan. {@code request.planCode()} must be unique.
   */
  public Plan createPlan(final PlanRequest request) {
    if (planMapper.findByPlanCode(request.planCode()).isPresent()) {
      throw new DuplicateResourceException("Plan already exists for planCode=" + request.planCode());
    }
    final Plan plan = toPlan(request);
    planMapper.insert(plan);
    return planMapper.findByPlanCode(plan.getPlanCode())
        .orElseThrow(() -> new PlanNotFoundException(plan.getPlanCode()));
  }

  /**
   * Replaces mutable fields for {@code planCode} (path wins over body {@code planCode} if both present).
   */
  public Plan replacePlan(final String planCode, final PlanRequest request) {
    planMapper.findByPlanCode(planCode)
        .orElseThrow(() -> new PlanNotFoundException(planCode));
    final Plan plan = toPlan(request);
    plan.setPlanCode(planCode);
    planMapper.update(plan);
    return planMapper.findByPlanCode(planCode)
        .orElseThrow(() -> new PlanNotFoundException(planCode));
  }

  public Plan patchPlan(final String planCode, final PlanPatchRequest request) {
    final Plan plan = planMapper.findByPlanCode(planCode)
        .orElseThrow(() -> new PlanNotFoundException(planCode));
    if (request.displayName() != null) {
      plan.setDisplayName(request.displayName());
    }
    if (request.billingPeriod() != null) {
      plan.setBillingPeriod(request.billingPeriod());
    }
    if (request.priceMinor() != null) {
      plan.setPriceMinor(request.priceMinor());
    }
    if (request.currency() != null) {
      plan.setCurrency(request.currency());
    }
    if (request.featureSet() != null) {
      plan.setFeatureSet(request.featureSet());
    }
    if (request.scope() != null) {
      plan.setScope(request.scope());
    }
    if (request.externalProductId() != null) {
      plan.setExternalProductId(request.externalProductId());
    }
    if (request.externalPriceId() != null) {
      plan.setExternalPriceId(request.externalPriceId());
    }
    if (request.active() != null) {
      plan.setActive(request.active());
    }
    planMapper.update(plan);
    return planMapper.findByPlanCode(planCode)
        .orElseThrow(() -> new PlanNotFoundException(planCode));
  }

  /**
   * Soft-deactivates the plan ({@code active = false}).
   */
  public void deactivatePlan(final String planCode) {
    final Plan plan = planMapper.findByPlanCode(planCode)
        .orElseThrow(() -> new PlanNotFoundException(planCode));
    plan.setActive(false);
    planMapper.update(plan);
  }

  private static Plan toPlan(final PlanRequest request) {
    final Plan plan = new Plan();
    plan.setPlanCode(request.planCode());
    plan.setDisplayName(request.displayName());
    plan.setBillingPeriod(request.billingPeriod());
    plan.setPriceMinor(request.priceMinor());
    plan.setCurrency(request.currency() != null ? request.currency() : "USD");
    plan.setFeatureSet(request.featureSet());
    plan.setScope(request.scope());
    plan.setExternalProductId(request.externalProductId());
    plan.setExternalPriceId(request.externalPriceId());
    plan.setActive(request.active() != null ? request.active() : Boolean.TRUE);
    return plan;
  }
}
