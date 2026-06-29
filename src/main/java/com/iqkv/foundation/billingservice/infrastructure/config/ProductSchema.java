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
import com.iqkv.foundation.billingservice.plan.PricingModel;

/**
 * Gateway-neutral configuration for a single product entry in the plan catalog.
 *
 * <p>Bound from {@code iqkv.billing.plan-catalog.products.<key>} in YAML.
 * {@code features} is optional — {@link PlanFeatures#NONE} is used when absent.
 * {@code pricingModel} is optional — defaults to {@link PricingModel#FLAT} when absent,
 * preserving backward compatibility for all existing plan definitions.
 *
 * <p>Gateway-specific fields:
 * <ul>
 *   <li>{@code externalVariantId} — Lemon Squeezy variant ID.  Operators must pre-create
 *       products/variants in the LS dashboard and configure the variant ID here before
 *       deployment.  Ignored by the Stripe adapter.</li>
 * </ul>
 */
public record ProductSchema(
    @NotBlank String planCode,
    @NotBlank String displayName,
    String description,
    @NotBlank String billingPeriod,
    @NotNull @Positive Integer priceMinor,
    @NotBlank String currency,
    PlanFeatures features,
    @NotBlank String scope,
    Boolean active,
    Integer trialPeriodDays,
    PricingModel pricingModel,
    /**
     * Lemon Squeezy variant ID.
     * Populated by LS operators in {@code iqkv.billing.plan-catalog.products.<key>.externalVariantId}.
     * {@link com.iqkv.foundation.billingservice.infrastructure.config.BillingSeedRunner} writes this
     * value to {@code plan_catalog.external_price_id} before calling
     * {@link com.iqkv.foundation.billingservice.gateway.port.PaymentGatewayPort#syncProduct}.
     * Stripe adapter ignores this field entirely.
     */
    String externalVariantId
) {
  /**
   * Returns the effective pricing model, defaulting to {@link PricingModel#FLAT} when absent.
   */
  public PricingModel effectivePricingModel() {
    return pricingModel != null ? pricingModel : PricingModel.FLAT;
  }
}
