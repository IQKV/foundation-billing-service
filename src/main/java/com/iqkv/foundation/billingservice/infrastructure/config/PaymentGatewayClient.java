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

import java.util.Map;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.param.CustomerCreateParams;
import org.springframework.stereotype.Component;

/**
 * Wrapper for Stripe SDK client initialization and global settings.
 */
@Component
public class PaymentGatewayClient {

  public PaymentGatewayClient(final StripeConfigurationProperties stripeProps) {
    Stripe.apiKey = stripeProps.secretKey();
  }

  /**
   * Creates a new customer in Stripe.
   *
   * @param name  the customer name (tenant name)
   * @param email the customer email (owner email); may be {@code null} or blank in single-tenant mode
   * @return the Stripe customer ID
   * @throws StripeException if the creation fails
   */
  public String createCustomer(final String name, final String email) throws StripeException {
    final CustomerCreateParams.Builder builder = CustomerCreateParams.builder()
        .setName(name)
        .putMetadata("managed_by", "foundation-billing-service");

    if (email != null && !email.isBlank()) {
      builder.setEmail(email);
    }

    final Customer customer = Customer.create(builder.build());
    return customer.getId();
  }
}
