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

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;

import java.util.Collections;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for billing-specific settings.
 * Provides fallback values used in single-tenant mode where tenant owner fields may be absent.
 */
@Validated
@ConfigurationProperties(prefix = "iqkv.billing")
public record BillingConfigurationProperties(
    /*
     * Fallback billing contact email used in single-tenant mode when the tenant.created event
     * does not carry an ownerEmail. Optional — if null, Stripe customer creation proceeds
     * without an email address.
     */
    @Email(message = "defaultContactEmail must be a valid email address") String defaultContactEmail,

    @Valid StripeProperties stripe
) {
  public BillingConfigurationProperties {
    if (stripe == null) {
      stripe = new StripeProperties(new SchemaProperties(Collections.emptyList()));
    }
  }

  public record StripeProperties(@Valid SchemaProperties schema) {
    public StripeProperties {
      if (schema == null) {
        schema = new SchemaProperties(Collections.emptyList());
      }
    }
  }

  public record SchemaProperties(@Valid List<StripeProductSchema> products) {
    public SchemaProperties {
      if (products == null) {
        products = Collections.emptyList();
      }
    }
  }
}
