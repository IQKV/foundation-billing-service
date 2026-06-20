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

package com.iqkv.foundation.billingservice.infrastructure.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import com.iqkv.foundation.billingservice.plan.PlanFeatures;

/**
 * Configuration for a single product in the Stripe catalog schema.
 *
 * <p>Bound from {@code iqkv.billing.stripe.schema.products.<key>} in YAML.
 * {@code features} is optional — {@link PlanFeatures#NONE} is used when absent.
 */
public record StripeProductSchema(
    @NotBlank String planCode,
    @NotBlank String displayName,
    String description,
    @NotBlank String billingPeriod,
    @NotNull @Positive Integer priceMinor,
    @NotBlank String currency,
    PlanFeatures features,
    @NotBlank String scope,
    Boolean active,
    Integer trialPeriodDays
) {
}
