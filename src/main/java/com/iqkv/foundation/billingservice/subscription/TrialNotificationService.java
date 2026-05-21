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

package com.iqkv.foundation.billingservice.subscription;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
 * Service for sending trial ending notifications.
 *
 * <p>Runs daily to check for trials ending in 3 days and sends notification emails.
 * Uses ShedLock to ensure only one instance runs in a distributed environment.
 */
@Service
@ConditionalOnProperty(name = "iqkv.messaging.rabbitmq.enabled", havingValue = "true")
public class TrialNotificationService {

  private static final Logger log = LoggerFactory.getLogger(TrialNotificationService.class);

  private final SubscriptionMapper subscriptionMapper;
  private final BillingSettingsMapper billingSettingsMapper;
  private final MessagingService messagingService;
  private final NotificationConfigurationProperties notificationProps;

  public TrialNotificationService(final SubscriptionMapper subscriptionMapper,
                                  final BillingSettingsMapper billingSettingsMapper,
                                  final MessagingService messagingService,
                                  final NotificationConfigurationProperties notificationProps) {
    this.subscriptionMapper = subscriptionMapper;
    this.billingSettingsMapper = billingSettingsMapper;
    this.messagingService = messagingService;
    this.notificationProps = notificationProps;
  }

  /**
   * Checks for trials ending in 3 days and sends notification emails.
   * Runs daily at 9 AM UTC.
   */
  @Scheduled(cron = "0 0 9 * * *") // Daily at 9 AM UTC
  @SchedulerLock(name = "trialEndingNotifications", lockAtMostFor = "PT10M", lockAtLeastFor = "PT1M")
  public void sendTrialEndingNotifications() {
    log.info("Starting trial ending notification job");

    final Instant threeDaysFromNow = Instant.now().plus(3, ChronoUnit.DAYS);
    final Instant twoDaysFromNow = Instant.now().plus(2, ChronoUnit.DAYS);

    // Find trials ending between 2-3 days from now (24-hour window)
    final var trialSubscriptions = subscriptionMapper.findTrialsEndingBetween(twoDaysFromNow, threeDaysFromNow);

    int notificationsSent = 0;
    for (final var subscription : trialSubscriptions) {
      try {
        sendTrialEndingNotification(subscription);
        notificationsSent++;
      } catch (final Exception e) {
        log.error("Failed to send trial ending notification for subscription {}: {}",
            subscription.getExternalSubscriptionId(), e.getMessage(), e);
      }
    }

    log.info("Trial ending notification job completed. Sent {} notifications", notificationsSent);
  }

  /**
   * Sends trial ending notification for overdue payments.
   * Runs daily at 10 AM UTC.
   */
  @Scheduled(cron = "0 0 10 * * *") // Daily at 10 AM UTC  
  @SchedulerLock(name = "paymentOverdueNotifications", lockAtMostFor = "PT10M", lockAtLeastFor = "PT1M")
  public void sendPaymentOverdueNotifications() {
    log.info("Starting payment overdue notification job");

    // Find subscriptions that are past_due
    final var overdueSubscriptions = subscriptionMapper.findByStatus("past_due");

    int notificationsSent = 0;
    for (final var subscription : overdueSubscriptions) {
      try {
        sendPaymentOverdueNotification(subscription);
        notificationsSent++;
      } catch (final Exception e) {
        log.error("Failed to send payment overdue notification for subscription {}: {}",
            subscription.getExternalSubscriptionId(), e.getMessage(), e);
      }
    }

    log.info("Payment overdue notification job completed. Sent {} notifications", notificationsSent);
  }

  private void sendTrialEndingNotification(final Subscription subscription) {
    final String tenantKey = subscription.getTenantKey();
    if (tenantKey == null || tenantKey.isBlank()) {
      log.warn("Cannot send trial ending notification - tenantKey is null for subscription {}",
          subscription.getExternalSubscriptionId());
      return;
    }

    billingSettingsMapper.findByTenantKey(tenantKey).ifPresent(settings -> {
      final String email = resolveEmail(settings);
      if (email != null) {
        messagingService.publishNotification(new NotificationEvent(
            email,
            notificationProps.defaultLocale(),
            NotificationEventType.TRIAL_ENDING,
            Map.of(
                "companyName", settings.getCompanyName() != null ? settings.getCompanyName() : "",
                "planId", subscription.getPlanId() != null ? subscription.getPlanId() : "",
                "externalSubscriptionId", subscription.getExternalSubscriptionId(),
                "trialEndDate", subscription.getCurrentPeriodEnd() != null
                    ? subscription.getCurrentPeriodEnd().toString() : ""
            ),
            Instant.now()));

        log.info("Sent trial ending notification for tenant {}, subscription {}",
            tenantKey, subscription.getExternalSubscriptionId());
      }
    });
  }

  private void sendPaymentOverdueNotification(final Subscription subscription) {
    final String tenantKey = subscription.getTenantKey();
    if (tenantKey == null || tenantKey.isBlank()) {
      log.warn("Cannot send payment overdue notification - tenantKey is null for subscription {}",
          subscription.getExternalSubscriptionId());
      return;
    }

    billingSettingsMapper.findByTenantKey(tenantKey).ifPresent(settings -> {
      final String email = resolveEmail(settings);
      if (email != null) {
        messagingService.publishNotification(new NotificationEvent(
            email,
            notificationProps.defaultLocale(),
            NotificationEventType.PAYMENT_OVERDUE,
            Map.of(
                "companyName", settings.getCompanyName() != null ? settings.getCompanyName() : "",
                "planId", subscription.getPlanId() != null ? subscription.getPlanId() : "",
                "externalSubscriptionId", subscription.getExternalSubscriptionId(),
                "overdueDate", subscription.getCurrentPeriodEnd() != null
                    ? subscription.getCurrentPeriodEnd().toString() : ""
            ),
            Instant.now()));

        log.info("Sent payment overdue notification for tenant {}, subscription {}",
            tenantKey, subscription.getExternalSubscriptionId());
      }
    });
  }

  private String resolveEmail(final com.iqkv.foundation.billingservice.settings.BillingSettings settings) {
    if (settings.getBillingEmail() != null && !settings.getBillingEmail().isBlank()) {
      return settings.getBillingEmail();
    }
    log.warn("No billing email configured for tenantKey={}", settings.getTenantKey());
    return null;
  }
}
