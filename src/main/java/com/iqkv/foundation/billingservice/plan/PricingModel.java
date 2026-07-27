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

/**
 * Determines how a plan's {@code priceMinor} is applied at checkout.
 *
 * <ul>
 *   <li>{@link #FLAT} — the price is a fixed amount per billing period regardless of seat count.
 *       Stripe line item is always {@code quantity = 1}.</li>
 *   <li>{@link #PER_SEAT} — the price is multiplied by the number of purchased seats.
 *       Stripe line item uses {@code quantity = seatCount}. The plan's
 *       {@code PlanEntitlement.maxUsers} acts as the seat ceiling (0 = unlimited).</li>
 * </ul>
 *
 * <p>Both modes use a Stripe {@code UNIT_AMOUNT} recurring price — the distinction is
 * enforced in {@code SubscriptionService}, not at the Stripe price level.
 */
public enum PricingModel {
  FLAT,
  PER_SEAT,
  METERED
}
