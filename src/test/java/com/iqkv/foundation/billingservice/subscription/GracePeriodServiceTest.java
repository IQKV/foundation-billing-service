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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.iqkv.foundation.billingservice.infrastructure.config.NotificationConfigurationProperties;
import com.iqkv.foundation.billingservice.infrastructure.messaging.MessagingService;
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
@DisplayName("GracePeriodService Unit Tests")
class GracePeriodServiceTest {

  @Mock
  private SubscriptionMapper subscriptionMapper;

  @Mock
  private BillingSettingsMapper billingSettingsMapper;

  @Mock
  private MessagingService messagingService;

  @Mock
  private NotificationConfigurationProperties notificationProps;

  @InjectMocks
  private GracePeriodService gracePeriodService;

  @Test
  @DisplayName("Should suspend expired grace period subscriptions and publish events")
  void shouldSuspendExpiredGracePeriodSubscriptions() {
    // Arrange
    final String tenantKey = "tenant-123";
    final String subscriptionId = "sub_expired";
    final UUID subUuid = UUID.randomUUID();
    final Subscription subscription = createSubscription(subUuid, tenantKey, subscriptionId, "past_due");

    when(subscriptionMapper.findExpiredGracePeriods()).thenReturn(List.of(subscription));

    // Act
    gracePeriodService.expireGracePeriods();

    // Assert
    verify(subscriptionMapper).suspend(eq(subUuid), any(Instant.class));
    verify(messagingService).publishSubscriptionGracePeriodEnded(
        eq(tenantKey), eq(subscriptionId), eq("TENANT"), eq(tenantKey));
  }

  @Test
  @DisplayName("Should do nothing when no expired grace periods exist")
  void shouldDoNothingWhenNoExpiredGracePeriods() {
    // Arrange
    when(subscriptionMapper.findExpiredGracePeriods()).thenReturn(List.of());

    // Act
    gracePeriodService.expireGracePeriods();

    // Assert
    verify(subscriptionMapper, never()).suspend(any(), any());
    verify(messagingService, never()).publishSubscriptionGracePeriodEnded(any(), any(), any(), any());
  }

  @Test
  @DisplayName("Should send account suspended notification email")
  void shouldSendAccountSuspendedNotification() {
    // Arrange
    final String tenantKey = "tenant-456";
    final String subscriptionId = "sub_notify";
    final UUID subUuid = UUID.randomUUID();
    final Subscription subscription = createSubscription(subUuid, tenantKey, subscriptionId, "past_due");
    final BillingSettings settings = createBillingSettings(tenantKey);

    when(subscriptionMapper.findExpiredGracePeriods()).thenReturn(List.of(subscription));
    when(billingSettingsMapper.findByTenantKey(tenantKey)).thenReturn(Optional.of(settings));
    when(notificationProps.defaultLocale()).thenReturn("en");

    // Act
    gracePeriodService.expireGracePeriods();

    // Assert
    verify(messagingService).publishNotification(argThat(event ->
        event.getRecipientEmail().equals("billing@example.com")
        && event.getType() == NotificationEventType.ACCOUNT_SUSPENDED
    ));
  }

  @Test
  @DisplayName("Should continue processing when one subscription fails")
  void shouldContinueWhenOneSubscriptionFails() {
    // Arrange
    final Subscription failing = createSubscription(UUID.randomUUID(), "tenant-fail", "sub_fail", "past_due");
    final Subscription ok = createSubscription(UUID.randomUUID(), "tenant-ok", "sub_ok", "past_due");

    when(subscriptionMapper.findExpiredGracePeriods()).thenReturn(List.of(failing, ok));
    doThrow(new RuntimeException("DB error"))
        .when(subscriptionMapper).suspend(eq(failing.getId()), any(Instant.class));

    // Act
    gracePeriodService.expireGracePeriods();

    // Assert — the second subscription is still suspended
    verify(subscriptionMapper).suspend(eq(ok.getId()), any(Instant.class));
    verify(messagingService).publishSubscriptionGracePeriodEnded(
        eq("tenant-ok"), eq("sub_ok"), eq("TENANT"), eq("tenant-ok"));
  }

  private Subscription createSubscription(final UUID id, final String tenantKey,
                                          final String externalSubscriptionId, final String status) {
    final var subscription = new Subscription();
    subscription.setId(id);
    subscription.setTenantKey(tenantKey);
    subscription.setExternalSubscriptionId(externalSubscriptionId);
    subscription.setExternalCustomerId("cus_" + UUID.randomUUID());
    subscription.setStatus(status);
    subscription.setPlanId("price_123");
    subscription.setSubjectType("TENANT");
    subscription.setSubjectKey(tenantKey);
    subscription.setGracePeriodEnd(Instant.now().minusSeconds(86400));
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
