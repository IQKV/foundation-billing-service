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

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code PATCH /api/v1/billing/admin/plans/{planCode}} — only non-null fields are applied.
 */
public record PlanPatchRequest(
    @Size(max = 255)
    String displayName,

    @Pattern(regexp = "MONTHLY|ANNUAL", message = "must be MONTHLY or ANNUAL")
    String billingPeriod,

    @Positive
    Integer priceMinor,

    @Pattern(regexp = "[A-Z]{3}", message = "must be a 3-letter ISO 4217 currency code")
    String currency,

    String featureSet,

    @Pattern(regexp = "TENANT|USER", message = "must be TENANT or USER")
    String scope,

    String externalProductId,

    String externalPriceId,

    Boolean active
) {
}
