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

package com.iqkv.foundation.billingservice.subscription;

import java.time.Instant;

import com.iqkv.foundation.billingservice.plan.PlanFeatures;

/**
 * Represents the entitlement details derived from an active subscription.
 *
 * @param subject          the subscription subject (type + key)
 * @param planCode         the human-readable plan code (e.g. {@code "pro-monthly"})
 * @param status           the subscription status (active, trialing, past_due, etc.)
 * @param currentPeriodEnd when the current billing period ends
 * @param features         the typed feature set for the active plan; never {@code null}
 */
public record EntitlementDetails(
    SubscriptionSubject subject,
    String planCode,
    String status,
    Instant currentPeriodEnd,
    PlanFeatures features
) {
}
