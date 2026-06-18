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
 * A single named feature entry in a plan's feature list.
 *
 * <p>Used in the open-ended {@code features} list on {@link PlanFeatures} to carry
 * display-oriented and entitlement flags without requiring a Java record change per feature.
 * New features (e.g. {@code custom_domain}, {@code sso}, {@code api_calls_per_month}) are
 * added purely in YAML — no recompilation required.
 *
 * <p>Quota-enforced features ({@code max_users}, {@code max_projects}) remain as typed
 * fields on {@link PlanFeatures} for compile-time safety.
 *
 * @param code        machine-readable identifier (e.g. {@code "priority_support"})
 * @param title       human-readable label shown on pricing pages (e.g. {@code "Priority Support"})
 * @param value       feature value as a string — {@code "true"}/{@code "false"} for boolean
 *                    features, a number string for limits (e.g. {@code "10"})
 * @param description optional longer description shown as a tooltip or subtitle on pricing pages
 */
public record PlanFeature(
    String code,
    String title,
    String value,
    String description
) {

  /**
   * Returns {@code true} if this feature's value is the string {@code "true"} (case-insensitive).
   * Convenience method for boolean feature checks.
   */
  public boolean isEnabled() {
    return "true".equalsIgnoreCase(value);
  }
}
