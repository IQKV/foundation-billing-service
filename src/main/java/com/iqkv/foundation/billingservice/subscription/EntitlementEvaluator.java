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

import java.util.Optional;

/**
 * Evaluates active subscription entitlements for a given subject.
 *
 * <p>Queries the local subscription cache by {@code (subject_type, subject_key)} and
 * enriches the result with plan feature data from the plan catalog.
 */
public interface EntitlementEvaluator {

  /**
   * Evaluates the entitlements for the given subscription subject.
   *
   * @param subject the subscription subject (type + key)
   * @return the entitlement details if an active subscription exists, or empty
   */
  Optional<EntitlementDetails> evaluateEntitlements(SubscriptionSubject subject);
}
