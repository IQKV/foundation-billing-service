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

package com.iqkv.foundation.billingservice.infrastructure.messaging;

import com.iqkv.foundation.billingservice.infrastructure.config.RabbitMQConfig;
import com.iqkv.foundation.billingservice.infrastructure.persistence.BillingSettingsMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Consumes IAM user lifecycle events relevant to billing from {@code iqkv.billing.user.events}.
 *
 * <p>Billing subscribes to:
 * <ul>
 *   <li>{@code user.removed} — user removed from a tenant; clear {@code profileOwnerId}
 *       on that tenant's billing settings if it matches</li>
 *   <li>{@code user.deleted} — user account deleted entirely; clear {@code profileOwnerId}
 *       across all tenants where this user was the billing profile owner</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(name = "iqkv.messaging.rabbitmq.enabled", havingValue = "true")
public class UserEventConsumer {

  private static final Logger log = LoggerFactory.getLogger(UserEventConsumer.class);

  private final BillingSettingsMapper billingSettingsMapper;

  public UserEventConsumer(final BillingSettingsMapper billingSettingsMapper) {
    this.billingSettingsMapper = billingSettingsMapper;
  }

  @RabbitListener(queues = RabbitMQConfig.USER_EVENTS_QUEUE)
  public void handleUserEvent(final UserEvent event) {
    if (event.getEventType() == null || event.getUserId() == null) {
      log.warn("Received user event with null eventType or userId — discarding");
      return;
    }
    switch (event.getEventType()) {
      case USER_REMOVED -> handleUserRemoved(event);
      case USER_DELETED -> handleUserDeleted(event);
      default -> log.debug("Unhandled user event type: {}", event.getEventType());
    }
  }

  /**
   * User was removed from a specific tenant.
   * If they were the {@code profileOwnerId} on that tenant's billing settings, clear the reference.
   */
  private void handleUserRemoved(final UserEvent event) {
    final String tenantKey = event.getTenantId();
    if (tenantKey == null || tenantKey.isBlank()) {
      log.warn("user.removed event missing tenantId for userId={}", event.getUserId());
      return;
    }
    billingSettingsMapper.findByTenantKey(tenantKey).ifPresent(settings -> {
      if (event.getUserId().equals(settings.getProfileOwnerId())) {
        settings.setProfileOwnerId(null);
        billingSettingsMapper.update(settings);
        log.info("Cleared profileOwnerId on billing settings: tenantKey={}, userId={}",
            tenantKey, event.getUserId());
      }
    });
  }

  /**
   * User account deleted entirely.
   * Clear {@code profileOwnerId} on all billing settings where this user was the owner.
   */
  private void handleUserDeleted(final UserEvent event) {
    final int updated = billingSettingsMapper.clearProfileOwnerById(event.getUserId());
    if (updated > 0) {
      log.info("Cleared profileOwnerId for deleted user={} across {} billing settings record(s)",
          event.getUserId(), updated);
    }
  }
}
