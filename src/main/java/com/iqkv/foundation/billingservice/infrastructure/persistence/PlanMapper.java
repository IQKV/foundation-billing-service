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

package com.iqkv.foundation.billingservice.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.iqkv.foundation.billingservice.plan.Plan;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PlanMapper {

  /**
   * Returns the plan with the given ID, or empty if not found.
   */
  Optional<Plan> findById(UUID id);

  /**
   * Returns the plan with the given plan code, or empty if not found.
   */
  Optional<Plan> findByPlanCode(String planCode);

  /**
   * Returns the plan whose {@code external_price_id} matches the given value, or empty if not found.
   * Used to resolve a Stripe price ID back to the internal plan catalog entry.
   */
  Optional<Plan> findByExternalPriceId(String externalPriceId);

  /**
   * Returns all active plans ({@code active = true}), ordered by {@code plan_code ASC}.
   */
  List<Plan> findAllActive();

  /**
   * Returns all plans in the catalog (including inactive), ordered by {@code plan_code ASC}.
   */
  List<Plan> findAll();

  /**
   * Inserts a new plan into the catalog.
   */
  void insert(Plan plan);

  /**
   * Updates mutable fields of an existing plan identified by {@code plan_code}.
   */
  void update(Plan plan);

  /**
   * Deletes the plan with the given plan code.
   */
  void deleteByPlanCode(String planCode);
}
