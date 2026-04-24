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

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.param.CustomerCreateParams;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class PaymentGatewayClient {

  private final ApplicationProperties applicationProperties;

  public PaymentGatewayClient(ApplicationProperties applicationProperties) {
    this.applicationProperties = applicationProperties;
  }

  @PostConstruct
  public void init() {
    Stripe.apiKey = applicationProperties.stripe().secretKey();
  }

  public String createCustomer(final String tenantName, final String ownerEmail) throws StripeException {
    final CustomerCreateParams params = CustomerCreateParams.builder()
        .setName(tenantName)
        .setEmail(ownerEmail)
        .build();
    final Customer customer = Customer.create(params);
    return customer.getId();
  }
}
