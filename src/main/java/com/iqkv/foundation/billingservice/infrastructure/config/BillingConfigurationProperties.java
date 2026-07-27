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

package com.iqkv.foundation.billingservice.infrastructure.config;

import jakarta.validation.constraints.Email;
import java.util.Collections;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for billing-specific settings.
 *
 * <p>The plan catalog is now bound from {@code iqkv.billing.plan-catalog.products}
 * (gateway-neutral path, renamed from the former {@code iqkv.billing.stripe.schema.products}).
 *
 * <p>Provides fallback values used in single-tenant mode where tenant owner fields may be absent.
 */
@Validated
@ConfigurationProperties(prefix = "iqkv.billing")
public record BillingConfigurationProperties(
    /*
     * Fallback billing contact email used in single-tenant mode when the tenant.created event
     * does not carry an ownerEmail.
     * Required when gateway type is LEMON_SQUEEZY in SINGLE_TENANT mode (LS requires an email).
     * Optional for Stripe — Stripe allows customer creation without an email address.
     */
    @Email(message = "defaultContactEmail must be a valid email address") String defaultContactEmail,

    PlanCatalogProperties planCatalog
) {
  public BillingConfigurationProperties {
    if (planCatalog == null) {
      planCatalog = new PlanCatalogProperties(Collections.emptyMap());
    }
  }

  /**
   * Gateway-neutral plan catalog properties.
   * Bound from {@code iqkv.billing.plan-catalog}.
   */
  public record PlanCatalogProperties(Map<String, ProductSchema> products) {
    public PlanCatalogProperties {
      if (products == null) {
        products = Collections.emptyMap();
      }
    }
  }
}
