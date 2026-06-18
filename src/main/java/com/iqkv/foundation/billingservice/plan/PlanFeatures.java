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

import java.util.Collections;
import java.util.Map;

/**
 * Feature set for a subscription plan.
 *
 * <h3>Design — middle path</h3>
 * <ul>
 *   <li><b>Typed quota fields</b> ({@code maxUsers}, {@code maxProjects}) — kept as named
 *       {@code int} fields for compile-time safety. These are enforced at write time by IAM and
 *       other services; a typo in the field name would be a silent runtime bug, so they stay
 *       typed.</li>
 *   <li><b>Open feature map</b> ({@link #features}) — an extensible
 *       {@code Map<String, PlanFeature>} keyed by feature code (e.g. {@code "priority_support"}).
 *       O(1) lookup, no duplicate codes, insertion order preserved via {@code LinkedHashMap}
 *       (Spring Boot's default for YAML map binding). Adding a new feature requires only a
 *       YAML change — no recompilation of any service.</li>
 * </ul>
 *
 * <p>Bound from {@code iqkv.billing.stripe.schema.products.<planCode>.features} in YAML.
 * Absent quota fields default to the most restrictive value ({@code 1}).
 * An absent or empty {@code features} map is treated as an empty map.
 *
 * <p>{@link #NONE} is the safe fallback — most restrictive quotas, empty feature map.
 *
 * <p>{@code maxUsers} and {@code maxProjects} use {@code 0} to mean "unlimited".
 */
public record PlanFeatures(
    int maxUsers,
    int maxProjects,
    Map<String, PlanFeature> features
) {

  /** Safe fallback: most restrictive quotas, no display features. */
  public static final PlanFeatures NONE = new PlanFeatures(1, 1, Collections.emptyMap());

  public PlanFeatures {
    if (maxUsers < 0) {
      throw new IllegalArgumentException("maxUsers must be >= 0");
    }
    if (maxProjects < 0) {
      throw new IllegalArgumentException("maxProjects must be >= 0");
    }
    features = features != null ? Collections.unmodifiableMap(features) : Collections.emptyMap();
  }

  /**
   * Returns {@code true} if the feature map contains an entry for the given code
   * whose value is {@code "true"} (case-insensitive). O(1) lookup.
   *
   * <p>Use this for display-only boolean features. For quota enforcement, use the
   * typed fields {@link #maxUsers()} and {@link #maxProjects()} directly.
   *
   * @param code the feature code (e.g. {@code "priority_support"})
   */
  public boolean has(final String code) {
    if (code == null || code.isBlank()) {
      return false;
    }
    final PlanFeature feature = features.get(code);
    return feature != null && feature.isEnabled();
  }
}
