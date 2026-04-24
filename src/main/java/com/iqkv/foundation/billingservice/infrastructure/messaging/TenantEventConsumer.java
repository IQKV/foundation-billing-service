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

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import com.iqkv.foundation.billingservice.infrastructure.config.NotificationConfigurationProperties;
import com.iqkv.foundation.billingservice.infrastructure.config.PaymentGatewayClient;
import com.iqkv.foundation.billingservice.infrastructure.config.RabbitMQConfig;
import com.iqkv.foundation.billingservice.infrastructure.persistence.BillingSettingsMapper;
import com.iqkv.foundation.billingservice.settings.BillingSettings;
import com.iqkv.foundation.billingservice.shared.exception.PaymentGatewayException;
import com.stripe.exception.StripeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Consumes all {@code tenant.#} lifecycle events from {@code iqkv.billing.tenant.events}.
 *
 * <p>Billing needs full visibility into the tenant provisioning lifecycle because:
 * <ul>
 *   <li>{@code TENANT_CREATED} — bootstraps the Stripe customer and billing settings</li>
 *   <li>{@code TENANT_PROVISIONED} — tenant is now ACTIVE; billing can activate services</li>
 *   <li>{@code TENANT_PROVISIONING_FAILED} — provisioning failed; billing should not charge</li>
 *   <li>{@code TENANT_SUSPENDED} — tenant suspended; billing may pause invoicing</li>
 *   <li>{@code TENANT_DELETED} — tenant deleted; billing should archive/cancel</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(name = "iqkv.messaging.rabbitmq.enabled", havingValue = "true")
public class TenantEventConsumer {

  private static final Logger log = LoggerFactory.getLogger(TenantEventConsumer.class);

  private final BillingSettingsMapper billingSettingsMapper;
  private final PaymentGatewayClient paymentGatewayClient;
  private final MessagingService messagingService;
  private final NotificationConfigurationProperties notificationProps;

  public TenantEventConsumer(final BillingSettingsMapper billingSettingsMapper,
                              final PaymentGatewayClient paymentGatewayClient,
                              final MessagingService messagingService,
                              final NotificationConfigurationProperties notificationProps) {
    this.billingSettingsMapper = billingSettingsMapper;
    this.paymentGatewayClient = paymentGatewayClient;
    this.messagingService = messagingService;
    this.notificationProps = notificationProps;
  }

  @RabbitListener(queues = RabbitMQConfig.TENANT_EVENTS_QUEUE)
  public void handleTenantEvent(final TenantEvent event) {
    if (event.getEventType() == null) {
      log.warn("Received tenant event with null eventType for tenantKey={}", event.getTenantKey());
      return;
    }
    switch (event.getEventType()) {
      case TENANT_CREATED            -> handleTenantCreated(event);
      case TENANT_PROVISIONED        -> handleTenantProvisioned(event);
      case TENANT_PROVISIONING_FAILED -> handleTenantProvisioningFailed(event);
      case TENANT_SUSPENDED          -> handleTenantSuspended(event);
      case TENANT_DELETED            -> handleTenantDeleted(event);
      default -> log.debug("Unhandled tenant event type: {}", event.getEventType());
    }
  }

  // ---------------------------------------------------------------------------
  // Handlers
  // ---------------------------------------------------------------------------

  private void handleTenantCreated(final TenantEvent event) {
    final String tenantKey = event.getTenantKey();
    final String tenantName = event.getTenantName();
    final String ownerEmail = event.getOwnerEmail();

    if (ownerEmail == null) {
      throw new IllegalArgumentException(
          "tenant.created event for tenantKey=" + tenantKey + " is missing ownerEmail");
    }

    if (billingSettingsMapper.existsByTenantKey(tenantKey)) {
      log.warn("BillingSettings already exist for tenantKey={}, skipping duplicate tenant.created", tenantKey);
      return;
    }

    final String externalCustomerId;
    try {
      externalCustomerId = paymentGatewayClient.createCustomer(tenantName, ownerEmail);
    } catch (final StripeException e) {
      throw new PaymentGatewayException(
          "Failed to create payment gateway customer for tenantKey=" + tenantKey, e);
    }

    final LocalDateTime now = LocalDateTime.now();
    billingSettingsMapper.insert(new BillingSettings(
        UUID.randomUUID(), tenantKey, externalCustomerId, ownerEmail, tenantName,
        null, null, null, "USD", null, now, now));
    log.info("BillingSettings created: tenantKey={}, externalCustomerId={}", tenantKey, externalCustomerId);
  }

  private void handleTenantProvisioned(final TenantEvent event) {
    final String tenantKey = event.getTenantKey();
    log.info("Tenant provisioning succeeded: tenantKey={}", tenantKey);

    // Notify billing contact that their account is ready
    billingSettingsMapper.findByTenantKey(tenantKey).ifPresent(settings -> {
      final String email = settings.getBillingEmail();
      if (email != null && !email.isBlank()) {
        publishNotification(new NotificationEvent(
            email,
            notificationProps.defaultLocale(),
            NotificationEventType.SUBSCRIPTION_ACTIVATED,
            Map.of(
                "companyName", settings.getCompanyName() != null ? settings.getCompanyName() : "",
                "firstName", ""
            ),
            Instant.now()));
      }
    });
  }

  private void handleTenantProvisioningFailed(final TenantEvent event) {
    final String tenantKey = event.getTenantKey();
    log.warn("Tenant provisioning failed: tenantKey={} — billing will not activate", tenantKey);
    // No billing action needed; Stripe customer already created but no subscription activated.
    // Billing settings remain so retry-provisioning can succeed later.
  }

  private void handleTenantSuspended(final TenantEvent event) {
    final String tenantKey = event.getTenantKey();
    log.info("Tenant suspended: tenantKey={}", tenantKey);
    // Future: pause invoicing, flag billing settings, etc.
  }

  private void handleTenantDeleted(final TenantEvent event) {
    final String tenantKey = event.getTenantKey();
    log.info("Tenant deleted: tenantKey={}", tenantKey);
    // Future: cancel Stripe customer, archive billing settings, etc.
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  private void publishNotification(final NotificationEvent event) {
    try {
      messagingService.publishNotification(event);
    } catch (final Exception e) {
      log.warn("Failed to publish billing notification: type={} recipient={}",
          event.getType(), event.getRecipientEmail(), e);
    }
  }
}
