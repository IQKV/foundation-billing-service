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

package com.iqkv.foundation.billingservice.plan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.iqkv.foundation.billingservice.infrastructure.persistence.PlanMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlanCatalogService Unit Tests")
class PlanCatalogServiceTest {

  @Mock
  private PlanMapper planMapper;

  @InjectMocks
  private PlanCatalogService planCatalogService;

  @Test
  @DisplayName("Should list all plans including inactive")
  void shouldListAllPlans() {
    final Plan p = samplePlan("a", true);
    when(planMapper.findAll()).thenReturn(List.of(p));

    assertThat(planCatalogService.listAllPlans()).containsExactly(p);
  }

  @Test
  @DisplayName("Should deactivate plan")
  void shouldDeactivate() {
    final Plan existing = samplePlan("p1", true);
    when(planMapper.findByPlanCode("p1")).thenReturn(Optional.of(existing));

    planCatalogService.deactivatePlan("p1");

    assertThat(existing.getActive()).isFalse();
    verify(planMapper).update(existing);
  }

  private static Plan samplePlan(final String code, final boolean active) {
    final var p = new Plan();
    p.setId(UUID.randomUUID());
    p.setPlanCode(code);
    p.setDisplayName("Display " + code);
    p.setBillingPeriod("MONTHLY");
    p.setPriceMinor(100);
    p.setCurrency("USD");
    p.setScope("TENANT");
    p.setActive(active);
    p.setCreatedAt(LocalDateTime.now());
    p.setUpdatedAt(LocalDateTime.now());
    return p;
  }
}
