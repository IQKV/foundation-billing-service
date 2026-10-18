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

package com.iqkv.foundation.billingservice.subscription;

import java.time.Instant;
import java.util.Map;

import com.iqkv.foundation.billingservice.infrastructure.config.NotificationConfigurationProperties;
import com.iqkv.foundation.billingservice.infrastructure.messaging.MessagingService;
import com.iqkv.foundation.billingservice.infrastructure.messaging.NotificationEvent;
import com.iqkv.foundation.billingservice.infrastructure.messaging.NotificationEventType;
import com.iqkv.foundation.billingservice.infrastructure.persistence.BillingSettingsMapper;
import com.iqkv.foundation.billingservice.infrastructure.persistence.SubscriptionMapper;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Suspends subscriptions whose payment grace period has expired.
 *
 * <p>Runs daily to find past_due subscriptions whose {@code grace_period_end}
 * deadline has passed, marks them suspended ({@code suspended_at}), publishes a
 * {@code subscription.grace_period_ended} domain event so downstream services cut
 * entitlements, and sends an account-suspended notification email.
 *
 * <p>Uses ShedLock to ensure only one instance runs in a distributed environment.
 */
@Service
@ConditionalOnProperty(name = "iqkv.messaging.rabbitmq.enabled", havingValue = "true")
public class GracePeriodService {

  private static final Logger log = LoggerFactory.getLogger(GracePeriodService.class);

  private final SubscriptionMapper subscriptionMapper;
  private final BillingSettingsMapper billingSettingsMapper;
  private final MessagingService messagingService;
  private final NotificationConfigurationProperties notificationProps;

  public GracePeriodService(final SubscriptionMapper subscriptionMapper,
                            final BillingSettingsMapper billingSettingsMapper,
                            final MessagingService messagingService,
                            final NotificationConfigurationProperties notificationProps) {
    this.subscriptionMapper = subscriptionMapper;
    this.billingSettingsMapper = billingSettingsMapper;
    this.messagingService = messagingService;
    this.notificationProps = notificationProps;
  }

  /**
   * Suspends subscriptions whose grace period has expired.
   * Runs daily at 2 AM UTC.
   */
  @Scheduled(cron = "0 0 2 * * *") // Daily at 2 AM UTC
  @SchedulerLock(name = "gracePeriodExpiry", lockAtMostFor = "PT10M", lockAtLeastFor = "PT1M")
  public void expireGracePeriods() {
    log.info("Starting grace period expiry job");

    final var expired = subscriptionMapper.findExpiredGracePeriods();
    if (expired.isEmpty()) {
      log.info("Grace period expiry job completed. No expired grace periods found");
      return;
    }

    int suspended = 0;
    final Instant now = Instant.now();
    for (final var subscription : expired) {
      try {
        subscriptionMapper.suspend(subscription.getId(), now);
        messagingService.publishSubscriptionGracePeriodEnded(
            subscription.getTenantKey(),
            subscription.getExternalSubscriptionId(),
            subscription.getSubjectType(),
            subscription.getSubjectKey());
        sendSuspendedNotification(subscription);
        suspended++;
      } catch (final Exception e) {
        log.error("Failed to suspend subscription {}: {}",
            subscription.getExternalSubscriptionId(), e.getMessage(), e);
      }
    }

    log.info("Grace period expiry job completed. Suspended {} subscriptions", suspended);
  }

  private void sendSuspendedNotification(final Subscription subscription) {
    final String tenantKey = subscription.getTenantKey();
    if (tenantKey == null || tenantKey.isBlank()) {
      log.warn("Cannot send suspension notification - tenantKey is null for subscription {}",
          subscription.getExternalSubscriptionId());
      return;
    }

    billingSettingsMapper.findByTenantKey(tenantKey).ifPresent(settings -> {
      final String email = settings.getBillingEmail();
      if (email != null && !email.isBlank()) {
        messagingService.publishNotification(new NotificationEvent(
            email,
            notificationProps.defaultLocale(),
            NotificationEventType.ACCOUNT_SUSPENDED,
            Map.of(
                "companyName", settings.getCompanyName() != null ? settings.getCompanyName() : "",
                "externalSubscriptionId", subscription.getExternalSubscriptionId()
            ),
            Instant.now()));

        log.info("Sent account suspended notification for tenant {}, subscription {}",
            tenantKey, subscription.getExternalSubscriptionId());
      }
    });
  }
}
