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

package com.iqkv.foundation.billingservice.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.UUID;

import com.iqkv.foundation.billingservice.infrastructure.config.NotificationConfigurationProperties;
import com.iqkv.foundation.billingservice.infrastructure.messaging.MessagingService;
import com.iqkv.foundation.billingservice.infrastructure.persistence.BillingSettingsMapper;
import com.iqkv.foundation.billingservice.infrastructure.persistence.SubscriptionMapper;
import com.iqkv.foundation.billingservice.infrastructure.persistence.UserBillingSettingsMapper;
import com.iqkv.foundation.billingservice.infrastructure.persistence.WebhookLogMapper;
import com.iqkv.foundation.billingservice.plan.PlanEligibilityPolicy;
import com.iqkv.foundation.billingservice.settings.BillingSettings;
import com.iqkv.foundation.billingservice.subscription.SubscriptionSubjectResolver;
import com.stripe.model.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebhookProcessingService Unit Tests")
class WebhookProcessingServiceTest {

  @Mock
  private WebhookLogMapper webhookLogMapper;

  @Mock
  private SubscriptionMapper subscriptionMapper;

  @Mock
  private BillingSettingsMapper billingSettingsMapper;

  @Mock
  private UserBillingSettingsMapper userBillingSettingsMapper;

  @Mock
  private MessagingService messagingService;

  @Mock
  private NotificationConfigurationProperties notificationProps;

  @Mock
  private SubscriptionSubjectResolver subjectResolver;

  @Mock
  private PlanEligibilityPolicy planEligibilityPolicy;

  private WebhookProcessingService webhookProcessingService;

  @BeforeEach
  void setUp() {
    webhookProcessingService = new WebhookProcessingService(
        webhookLogMapper,
        subscriptionMapper,
        billingSettingsMapper,
        userBillingSettingsMapper,
        messagingService,
        notificationProps,
        subjectResolver,
        planEligibilityPolicy
    );
  }

  @Test
  @DisplayName("Should return true when event is duplicate")
  void shouldReturnTrueWhenEventIsDuplicate() {
    // Arrange
    final Event event = createMockEvent("evt_123", "customer.subscription.created");
    when(webhookLogMapper.existsByExternalEventId("evt_123")).thenReturn(true);

    // Act
    final boolean result = webhookProcessingService.process(event);

    // Assert
    assertThat(result).isTrue();
    verify(webhookLogMapper).existsByExternalEventId("evt_123");
    verify(webhookLogMapper, never()).insert(any());
  }

  @Test
  @DisplayName("Should insert webhook log for new event")
  void shouldInsertWebhookLogForNewEvent() {
    // Arrange
    final Event event = createMockEvent("evt_456", "customer.updated");
    when(webhookLogMapper.existsByExternalEventId("evt_456")).thenReturn(false);

    // Act
    final boolean result = webhookProcessingService.process(event);

    // Assert
    assertThat(result).isFalse();
    verify(webhookLogMapper).existsByExternalEventId("evt_456");
    verify(webhookLogMapper).insert(argThat(log ->
        log.getExternalEventId().equals("evt_456")
            && log.getEventType().equals("customer.updated")
            && log.getStatus().equals("RECEIVED")
    ));
  }

  @Test
  @DisplayName("Should handle unhandled event types gracefully")
  void shouldHandleUnhandledEventTypesGracefully() {
    // Arrange
    final Event event = createMockEvent("evt_unknown", "customer.updated");
    when(webhookLogMapper.existsByExternalEventId("evt_unknown")).thenReturn(false);

    // Act
    final boolean result = webhookProcessingService.process(event);

    // Assert
    assertThat(result).isFalse();
    verify(webhookLogMapper).insert(any());
    verify(webhookLogMapper).updateStatus(eq("evt_unknown"), eq("PROCESSED"), eq(null), any());
  }

  private Event createMockEvent(final String eventId, final String eventType) {
    final Event event = new Event();
    event.setId(eventId);
    event.setType(eventType);
    return event;
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
