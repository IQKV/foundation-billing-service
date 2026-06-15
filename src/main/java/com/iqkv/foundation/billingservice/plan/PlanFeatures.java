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

/**
 * Typed feature set for a subscription plan.
 *
 * <p>Bound from {@code iqkv.billing.stripe.schema.products.<planCode>.features} in YAML.
 * Absent fields default to the most restrictive value ({@code false} / {@code 1}).
 *
 * <p>The {@link #NONE} constant is used as a safe fallback whenever no active plan is
 * found — it grants no boolean features and caps all quotas at 1.
 *
 * <p>{@code maxUsers} and {@code maxProjects} use {@code 0} to mean "unlimited".
 */
public record PlanFeatures(
    boolean prioritySupport,
    int maxUsers,
    int maxProjects
) {

  /** Safe fallback: no features, most restrictive quotas. */
  public static final PlanFeatures NONE = new PlanFeatures(false, 1, 1);

  public PlanFeatures {
    if (maxUsers < 0) {
      throw new IllegalArgumentException("maxUsers must be >= 0");
    }
    if (maxProjects < 0) {
      throw new IllegalArgumentException("maxProjects must be >= 0");
    }
  }

  /**
   * String-keyed feature lookup used by the gateway filter and plan catalog cache.
   *
   * @param feature the feature key (e.g. {@code "priority_support"})
   * @return {@code true} if this plan includes the named feature
   */
  public boolean has(final String feature) {
    return switch (feature) {
      case "priority_support" -> prioritySupport;
      default -> false;
    };
  }
}
