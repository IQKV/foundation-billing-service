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

package com.iqkv.foundation.billingservice.infrastructure.messaging;

import java.time.LocalDateTime;
import java.util.UUID;

import com.iqkv.foundation.billingservice.infrastructure.config.PaymentGatewayClient;
import com.iqkv.foundation.billingservice.infrastructure.config.RabbitMQConfig;
import com.iqkv.foundation.billingservice.infrastructure.persistence.BillingSettingsMapper;
import com.iqkv.foundation.billingservice.settings.BillingSettings;
import com.stripe.exception.StripeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "iqkv.messaging.rabbitmq.enabled", havingValue = "true")
public class TenantEventConsumer {

  private static final Logger log = LoggerFactory.getLogger(TenantEventConsumer.class);

  private final BillingSettingsMapper billingSettingsMapper;
  private final PaymentGatewayClient paymentGatewayClient;

  public TenantEventConsumer(BillingSettingsMapper billingSettingsMapper,
                              PaymentGatewayClient paymentGatewayClient) {
    this.billingSettingsMapper = billingSettingsMapper;
    this.paymentGatewayClient = paymentGatewayClient;
  }

  @RabbitListener(queues = RabbitMQConfig.TENANT_EVENTS_QUEUE)
  public void handleTenantCreated(TenantEvent event) {
    final String tenantKey = event.getTenantKey();
    final String tenantName = event.getTenantName();
    final String ownerEmail = event.getOwnerEmail();

    // 1. Validate ownerEmail non-null — throw to route message to DLQ
    if (ownerEmail == null) {
      throw new IllegalArgumentException(
          "tenant.created event for tenantKey=" + tenantKey + " is missing ownerEmail");
    }

    // 2. Idempotency check — skip if billing settings already exist
    if (billingSettingsMapper.existsByTenantKey(tenantKey)) {
      log.warn("BillingSettings already exist for tenantKey={}, skipping duplicate tenant.created event", tenantKey);
      return;
    }

    // 3. Create payment gateway customer
    final String externalCustomerId;
    try {
      externalCustomerId = paymentGatewayClient.createCustomer(tenantName, ownerEmail);
    } catch (StripeException e) {
      throw new RuntimeException(
          "Failed to create payment gateway customer for tenantKey=" + tenantKey, e);
    }

    // 4. Persist billing settings
    final LocalDateTime now = LocalDateTime.now();
    final BillingSettings settings = new BillingSettings(
        UUID.randomUUID(),
        tenantKey,
        externalCustomerId,
        ownerEmail,
        tenantName,
        null,
        null,
        null,
        "USD",
        null,
        now,
        now
    );
    billingSettingsMapper.insert(settings);
    log.info("BillingSettings created for tenantKey={}, externalCustomerId={}", tenantKey, externalCustomerId);
  }
}
