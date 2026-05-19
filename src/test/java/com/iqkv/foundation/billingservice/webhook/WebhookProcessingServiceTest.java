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

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.iqkv.foundation.billingservice.gateway.event.GatewayInvoiceEvent;
import com.iqkv.foundation.billingservice.gateway.event.GatewayPaymentFailureEvent;
import com.iqkv.foundation.billingservice.gateway.event.GatewaySubscriptionEvent;
import com.iqkv.foundation.billingservice.gateway.event.GatewayWebhookEvent;
import com.iqkv.foundation.billingservice.infrastructure.config.NotificationConfigurationProperties;
import com.iqkv.foundation.billingservice.infrastructure.messaging.MessagingService;
import com.iqkv.foundation.billingservice.infrastructure.persistence.BillingSettingsMapper;
import com.iqkv.foundation.billingservice.infrastructure.persistence.SubscriptionMapper;
import com.iqkv.foundation.billingservice.infrastructure.persistence.UserBillingSettingsMapper;
import com.iqkv.foundation.billingservice.infrastructure.persistence.WebhookLogMapper;
import com.iqkv.foundation.billingservice.plan.PlanEligibilityPolicy;
import com.iqkv.foundation.billingservice.settings.BillingSettings;
import com.iqkv.foundation.billingservice.subscription.SubjectType;
import com.iqkv.foundation.billingservice.subscription.SubscriptionSubject;
import com.iqkv.foundation.billingservice.subscription.SubscriptionSubjectResolver;
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

  // -------------------------------------------------------------------------
  // Idempotency
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("Should return true when event is duplicate")
  void shouldReturnTrueWhenEventIsDuplicate() {
    // Arrange
    final GatewayWebhookEvent event = subscriptionCreatedEvent("evt_123", "tenant-abc");
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
    final String tenantKey = "tenant-abc";
    final GatewayWebhookEvent event = subscriptionCreatedEvent("evt_456", tenantKey);
    when(webhookLogMapper.existsByExternalEventId("evt_456")).thenReturn(false);
    when(subjectResolver.resolveSubject(eq(tenantKey), any()))
        .thenReturn(new SubscriptionSubject(SubjectType.TENANT, tenantKey));
    when(billingSettingsMapper.findByTenantKey(tenantKey)).thenReturn(Optional.empty());

    // Act
    final boolean result = webhookProcessingService.process(event);

    // Assert
    assertThat(result).isFalse();
    verify(webhookLogMapper).insert(argThat(log ->
        log.getExternalEventId().equals("evt_456")
            && log.getEventType().equals("customer.subscription.created")
            && log.getStatus().equals("RECEIVED")
    ));
  }

  // -------------------------------------------------------------------------
  // Subscription created
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("Should publish subscription.created event on subscription created webhook")
  void shouldPublishSubscriptionCreatedEvent() {
    // Arrange
    final String tenantKey = "tenant-xyz";
    final String externalSubscriptionId = "sub_abc123";
    final GatewayWebhookEvent event = subscriptionCreatedEvent("evt_sub_created", tenantKey, externalSubscriptionId);

    when(webhookLogMapper.existsByExternalEventId("evt_sub_created")).thenReturn(false);
    when(subjectResolver.resolveSubject(eq(tenantKey), any()))
        .thenReturn(new SubscriptionSubject(SubjectType.TENANT, tenantKey));
    when(billingSettingsMapper.findByTenantKey(tenantKey)).thenReturn(Optional.empty());

    // Act
    webhookProcessingService.process(event);

    // Assert
    verify(subscriptionMapper).upsert(argThat(sub ->
        sub.getExternalSubscriptionId().equals(externalSubscriptionId)
            && sub.getTenantKey().equals(tenantKey)
    ));
    verify(messagingService).publishSubscriptionCreated(
        eq(tenantKey), eq(externalSubscriptionId),
        eq(SubjectType.TENANT.name()), eq(tenantKey)
    );
    verify(webhookLogMapper).updateStatus(eq("evt_sub_created"), eq("PROCESSED"), eq(null), any());
  }

  // -------------------------------------------------------------------------
  // Invoice paid
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("Should publish invoice.paid event when billing settings found")
  void shouldPublishInvoicePaidEvent() {
    // Arrange
    final String tenantKey = "tenant-inv";
    final String customerId = "cus_inv123";
    final String invoiceId = "inv_abc";
    final GatewayInvoiceEvent event = new GatewayInvoiceEvent(
        "evt_inv_paid", "invoice.payment_succeeded", Instant.now(),
        invoiceId, customerId, "sub_inv123", 9900L, 9900L, "USD"
    );
    final BillingSettings settings = createBillingSettings(tenantKey, customerId);

    when(webhookLogMapper.existsByExternalEventId("evt_inv_paid")).thenReturn(false);
    when(billingSettingsMapper.findByExternalCustomerId(customerId)).thenReturn(Optional.of(settings));
    when(subscriptionMapper.findByExternalSubscriptionId("sub_inv123")).thenReturn(Optional.empty());
    when(notificationProps.defaultLocale()).thenReturn("en");

    // Act
    webhookProcessingService.process(event);

    // Assert
    verify(messagingService).publishInvoicePaid(
        eq(tenantKey), eq(invoiceId), eq(customerId), eq("sub_inv123"),
        eq(9900L), eq("USD"), eq(SubjectType.TENANT.name()), eq(tenantKey)
    );
    verify(webhookLogMapper).updateStatus(eq("evt_inv_paid"), eq("PROCESSED"), eq(null), any());
  }

  // -------------------------------------------------------------------------
  // Payment failed
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("Should publish payment.failed event when billing settings found")
  void shouldPublishPaymentFailedEvent() {
    // Arrange
    final String tenantKey = "tenant-fail";
    final String customerId = "cus_fail123";
    final String invoiceId = "inv_fail";
    final GatewayPaymentFailureEvent event = new GatewayPaymentFailureEvent(
        "evt_pay_failed", "invoice.payment_failed", Instant.now(),
        invoiceId, customerId, "sub_fail123", 4900L, "USD", "Card declined"
    );
    final BillingSettings settings = createBillingSettings(tenantKey, customerId);

    when(webhookLogMapper.existsByExternalEventId("evt_pay_failed")).thenReturn(false);
    when(billingSettingsMapper.findByExternalCustomerId(customerId)).thenReturn(Optional.of(settings));
    when(subscriptionMapper.findByExternalSubscriptionId("sub_fail123")).thenReturn(Optional.empty());
    when(notificationProps.defaultLocale()).thenReturn("en");

    // Act
    webhookProcessingService.process(event);

    // Assert
    verify(messagingService).publishPaymentFailed(
        eq(tenantKey), eq(invoiceId), eq(customerId), eq("sub_fail123"),
        eq(4900L), eq("USD"), eq("Card declined"),
        eq(SubjectType.TENANT.name()), eq(tenantKey)
    );
    verify(webhookLogMapper).updateStatus(eq("evt_pay_failed"), eq("PROCESSED"), eq(null), any());
  }

  // -------------------------------------------------------------------------
  // Error handling
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("Should mark webhook log as FAILED when processing throws")
  void shouldMarkWebhookLogAsFailedOnException() {
    // Arrange
    final String tenantKey = "tenant-err";
    final GatewayWebhookEvent event = subscriptionCreatedEvent("evt_err", tenantKey);

    when(webhookLogMapper.existsByExternalEventId("evt_err")).thenReturn(false);
    when(subjectResolver.resolveSubject(any(), any()))
        .thenThrow(new RuntimeException("Unexpected error"));

    // Act
    final boolean result = webhookProcessingService.process(event);

    // Assert
    assertThat(result).isFalse();
    verify(webhookLogMapper).updateStatus(
        eq("evt_err"), eq("FAILED"), argThat(msg -> msg.contains("Unexpected error")), eq(null)
    );
  }

  @Test
  @DisplayName("Should skip invoice.paid when no billing settings found for customer")
  void shouldSkipInvoicePaidWhenNoBillingSettingsFound() {
    // Arrange
    final GatewayInvoiceEvent event = new GatewayInvoiceEvent(
        "evt_no_settings", "invoice.payment_succeeded", Instant.now(),
        "inv_x", "cus_unknown", null, 1000L, 1000L, "USD"
    );

    when(webhookLogMapper.existsByExternalEventId("evt_no_settings")).thenReturn(false);
    when(billingSettingsMapper.findByExternalCustomerId("cus_unknown")).thenReturn(Optional.empty());

    // Act
    webhookProcessingService.process(event);

    // Assert — no messaging published, but log is still PROCESSED
    verify(messagingService, never()).publishInvoicePaid(any(), any(), any(), any(), any(), any(), any(), any());
    verify(webhookLogMapper).updateStatus(eq("evt_no_settings"), eq("PROCESSED"), eq(null), any());
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private GatewaySubscriptionEvent subscriptionCreatedEvent(final String eventId, final String tenantKey) {
    return subscriptionCreatedEvent(eventId, tenantKey, "sub_" + UUID.randomUUID());
  }

  private GatewaySubscriptionEvent subscriptionCreatedEvent(final String eventId,
                                                            final String tenantKey,
                                                            final String subscriptionId) {
    return new GatewaySubscriptionEvent(
        eventId,
        "customer.subscription.created",
        Instant.now(),
        subscriptionId,
        "cus_" + UUID.randomUUID(),
        "active",
        "price_basic",
        Instant.now(),
        Instant.now().plusSeconds(2592000),
        false,
        null,
        Map.of("tenantKey", tenantKey)
    );
  }

  private BillingSettings createBillingSettings(final String tenantKey, final String customerId) {
    final var settings = new BillingSettings();
    settings.setId(UUID.randomUUID());
    settings.setTenantKey(tenantKey);
    settings.setExternalCustomerId(customerId);
    settings.setBillingEmail("billing@example.com");
    settings.setCompanyName("Test Company");
    settings.setCurrency("USD");
    settings.setCreatedAt(LocalDateTime.now());
    settings.setUpdatedAt(LocalDateTime.now());
    return settings;
  }
}
