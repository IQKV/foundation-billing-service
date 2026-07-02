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
 * Value object for a single named feature entry within a plan's feature map.
 *
 * <p>The feature code (e.g. {@code "priority_support"}) is also the map key on
 * {@link PlanEntitlement#features()}, so it is carried here as well to make the object
 * self-contained when iterating the map or serialising individual entries (e.g. REST
 * responses, pricing-page DTOs). New entitlement are added purely in YAML; no Java change required.
 *
 * @param code        machine-readable identifier, matches the map key
 *                    (e.g. {@code "priority_support"})
 * @param title       human-readable label shown on pricing pages (e.g. {@code "Priority Support"})
 * @param value       feature value as a string — {@code "true"}/{@code "false"} for boolean
 *                    entitlement, a number string for limits (e.g. {@code "10"})
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
   */
  public boolean isEnabled() {
    return "true".equalsIgnoreCase(value);
  }
}
