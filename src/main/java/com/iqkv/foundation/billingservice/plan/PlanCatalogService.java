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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Application service for plan catalog reads and platform-admin mutations.
 */
@Service
public class PlanCatalogService {

  private static final Logger log = LoggerFactory.getLogger(PlanCatalogService.class);

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
   * Soft-deactivates the plan ({@code active = false}).
   */
  public void deactivatePlan(final String planCode) {
    final Plan plan = planMapper.findByPlanCode(planCode)
        .orElseThrow(() -> {
          log.warn("Deactivate plan failed: plan not found for planCode={}", planCode);
          return new PlanNotFoundException(planCode);
        });
    plan.setActive(false);
    planMapper.update(plan);
    log.info("Plan deactivated: planCode={}", planCode);
  }
}
