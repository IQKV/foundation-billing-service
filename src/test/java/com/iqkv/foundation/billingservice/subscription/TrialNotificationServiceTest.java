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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.iqkv.foundation.billingservice.infrastructure.config.NotificationConfigurationProperties;
import com.iqkv.foundation.billingservice.infrastructure.messaging.MessagingService;
import com.iqkv.foundation.billingservice.infrastructure.messaging.NotificationEvent;
import com.iqkv.foundation.billingservice.infrastructure.messaging.NotificationEventType;
import com.iqkv.foundation.billingservice.infrastructure.persistence.BillingSettingsMapper;
import com.iqkv.foundation.billingservice.infrastructure.persistence.SubscriptionMapper;
import com.iqkv.foundation.billingservice.settings.BillingSettings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("TrialNotificationService Unit Tests")
class TrialNotificationServiceTest {

  @Mock
  private SubscriptionMapper subscriptionMapper;

  @Mock
  private BillingSettingsMapper billingSettingsMapper;

  @Mock
  private MessagingService messagingService;

  @Mock
  private NotificationConfigurationProperties notificationProps;

  @InjectMocks
  private TrialNotificationService trialNotificationService;

  @Test
  @DisplayName("Should send trial ending notifications for subscriptions ending in 3 days")
  void shouldSendTrialEndingNotifications() {
    // Arrange
    final String tenantKey = "tenant-123";
    final Subscription subscription = createSubscription(tenantKey, "trialing");
    final BillingSettings settings = createBillingSettings(tenantKey);

    when(subscriptionMapper.findTrialsEndingBetween(any(Instant.class), any(Instant.class)))
        .thenReturn(List.of(subscription));
    when(billingSettingsMapper.findByTenantKey(tenantKey)).thenReturn(Optional.of(settings));
    when(notificationProps.defaultLocale()).thenReturn("en");

    // Act
    trialNotificationService.sendTrialEndingNotifications();

    // Assert
    verify(subscriptionMapper).findTrialsEndingBetween(any(Instant.class), any(Instant.class));
    verify(billingSettingsMapper).findByTenantKey(tenantKey);
    verify(messagingService).publishNotification(argThat(event ->
        event.getRecipientEmail().equals("billing@example.com")
        && event.getType() == NotificationEventType.TRIAL_ENDING
    ));
  }

  @Test
  @DisplayName("Should send payment overdue notifications for past_due subscriptions")
  void shouldSendPaymentOverdueNotifications() {
    // Arrange
    final String tenantKey = "tenant-456";
    final Subscription subscription = createSubscription(tenantKey, "past_due");
    final BillingSettings settings = createBillingSettings(tenantKey);

    when(subscriptionMapper.findByStatus("past_due")).thenReturn(List.of(subscription));
    when(billingSettingsMapper.findByTenantKey(tenantKey)).thenReturn(Optional.of(settings));
    when(notificationProps.defaultLocale()).thenReturn("en");

    // Act
    trialNotificationService.sendPaymentOverdueNotifications();

    // Assert
    verify(subscriptionMapper).findByStatus("past_due");
    verify(billingSettingsMapper).findByTenantKey(tenantKey);
    verify(messagingService).publishNotification(argThat(event ->
        event.getRecipientEmail().equals("billing@example.com")
        && event.getType() == NotificationEventType.PAYMENT_OVERDUE
    ));
  }

  @Test
  @DisplayName("Should not send notification when billing email is not configured")
  void shouldNotSendNotificationWhenBillingEmailNotConfigured() {
    // Arrange
    final String tenantKey = "tenant-789";
    final Subscription subscription = createSubscription(tenantKey, "trialing");
    final BillingSettings settings = createBillingSettings(tenantKey);
    settings.setBillingEmail(null);

    when(subscriptionMapper.findTrialsEndingBetween(any(Instant.class), any(Instant.class)))
        .thenReturn(List.of(subscription));
    when(billingSettingsMapper.findByTenantKey(tenantKey)).thenReturn(Optional.of(settings));

    // Act
    trialNotificationService.sendTrialEndingNotifications();

    // Assert
    verify(messagingService, never()).publishNotification(any(NotificationEvent.class));
  }

  @Test
  @DisplayName("Should not send notification when tenant key is null")
  void shouldNotSendNotificationWhenTenantKeyIsNull() {
    // Arrange
    final Subscription subscription = createSubscription(null, "trialing");

    when(subscriptionMapper.findTrialsEndingBetween(any(Instant.class), any(Instant.class)))
        .thenReturn(List.of(subscription));

    // Act
    trialNotificationService.sendTrialEndingNotifications();

    // Assert
    verify(billingSettingsMapper, never()).findByTenantKey(any());
    verify(messagingService, never()).publishNotification(any(NotificationEvent.class));
  }

  @Test
  @DisplayName("Should not send notification when billing settings not found")
  void shouldNotSendNotificationWhenBillingSettingsNotFound() {
    // Arrange
    final String tenantKey = "tenant-999";
    final Subscription subscription = createSubscription(tenantKey, "trialing");

    when(subscriptionMapper.findTrialsEndingBetween(any(Instant.class), any(Instant.class)))
        .thenReturn(List.of(subscription));
    when(billingSettingsMapper.findByTenantKey(tenantKey)).thenReturn(Optional.empty());

    // Act
    trialNotificationService.sendTrialEndingNotifications();

    // Assert
    verify(messagingService, never()).publishNotification(any(NotificationEvent.class));
  }

  @Test
  @DisplayName("Should send notifications for multiple subscriptions")
  void shouldSendNotificationsForMultipleSubscriptions() {
    // Arrange
    final String tenantKey1 = "tenant-111";
    final String tenantKey2 = "tenant-222";
    final Subscription subscription1 = createSubscription(tenantKey1, "trialing");
    final Subscription subscription2 = createSubscription(tenantKey2, "trialing");
    final BillingSettings settings1 = createBillingSettings(tenantKey1);
    final BillingSettings settings2 = createBillingSettings(tenantKey2);

    when(subscriptionMapper.findTrialsEndingBetween(any(Instant.class), any(Instant.class)))
        .thenReturn(List.of(subscription1, subscription2));
    when(billingSettingsMapper.findByTenantKey(tenantKey1)).thenReturn(Optional.of(settings1));
    when(billingSettingsMapper.findByTenantKey(tenantKey2)).thenReturn(Optional.of(settings2));
    when(notificationProps.defaultLocale()).thenReturn("en");

    // Act
    trialNotificationService.sendTrialEndingNotifications();

    // Assert
    verify(messagingService, times(2)).publishNotification(any(NotificationEvent.class));
  }

  private Subscription createSubscription(final String tenantKey, final String status) {
    final var subscription = new Subscription();
    subscription.setId(UUID.randomUUID());
    subscription.setExternalSubscriptionId("sub_" + UUID.randomUUID());
    subscription.setExternalCustomerId("cus_" + UUID.randomUUID());
    subscription.setTenantKey(tenantKey);
    subscription.setStatus(status);
    subscription.setPlanId("price_123");
    subscription.setCurrentPeriodEnd(Instant.now().plusSeconds(259200)); // 3 days
    return subscription;
  }

  private BillingSettings createBillingSettings(final String tenantKey) {
    final var settings = new BillingSettings();
    settings.setId(UUID.randomUUID());
    settings.setTenantKey(tenantKey);
    settings.setExternalCustomerId("cus_" + UUID.randomUUID());
    settings.setBillingEmail("billing@example.com");
    settings.setCompanyName("Test Company");
    settings.setCurrency("USD");
    settings.setCreatedAt(LocalDateTime.now());
    settings.setUpdatedAt(LocalDateTime.now());
    return settings;
  }
}
