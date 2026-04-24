package com.iqkv.foundation.billingservice.infrastructure.config;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.param.CustomerCreateParams;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PaymentGatewayClient {

  @Value("${iqkv.stripe.secret-key}")
  private String secretKey;

  @PostConstruct
  public void init() {
    Stripe.apiKey = secretKey;
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
