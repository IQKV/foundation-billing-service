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

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Request body for plan catalog create and update operations.
 *
 * <p>All fields are required for POST (create). For PUT (update), the {@code planCode}
 * is taken from the path variable and the remaining fields replace the existing values.
 *
 * @param planCode      unique plan identifier (e.g. {@code pro-monthly})
 * @param displayName   human-readable plan name
 * @param billingPeriod billing cycle — {@code MONTHLY} or {@code ANNUAL}
 * @param priceMinor    price in the smallest currency unit (e.g. cents for USD)
 * @param currency      ISO 4217 currency code (e.g. {@code USD})
 * @param featureSet    JSON string describing plan features (nullable)
 * @param scope         subject scope — {@code TENANT} or {@code USER}
 * @param active        whether the plan is visible to users
 */
public record PlanRequest(
    @NotBlank String planCode,
    @NotBlank String displayName,
    String description,
    @NotBlank String billingPeriod,
    @NotNull @Positive Integer priceMinor,
    @NotBlank String currency,
    String featureSet,
    @NotBlank String scope,
    String externalProductId,
    String externalPriceId,
    Boolean active
) {
}
