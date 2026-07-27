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

package com.iqkv.foundation.billingservice.subscription;

import java.util.UUID;

/**
 * Resolves the subscription subject based on the active platform rollout mode.
 *
 * <ul>
 *   <li>In {@code MULTI_TENANT} mode: subject is {@code TENANT / tenantKey}</li>
 *   <li>In {@code SINGLE_TENANT} mode: subject is {@code USER / userId.toString()}</li>
 * </ul>
 */
public interface SubscriptionSubjectResolver {

  /**
   * Resolves the subscription subject for the given request context.
   *
   * @param tenantKey the tenant key from the request context (may be null in single-tenant mode)
   * @param userId    the user ID from the JWT claims
   * @return the resolved {@link SubscriptionSubject}
   */
  SubscriptionSubject resolveSubject(String tenantKey, UUID userId);
}
