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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.iqkv.foundation.billingservice.gateway.command.UpdateSubscriptionCommand;
import com.iqkv.foundation.billingservice.gateway.port.PaymentGatewayPort;
import com.iqkv.foundation.billingservice.infrastructure.persistence.BillingSettingsMapper;
import com.iqkv.foundation.billingservice.infrastructure.persistence.RefundMapper;
import com.iqkv.foundation.billingservice.infrastructure.persistence.SubscriptionMapper;
import com.iqkv.foundation.billingservice.settings.BillingSettings;
import com.iqkv.foundation.billingservice.shared.exception.ResourceNotFoundException;
import com.iqkv.foundation.billingservice.shared.exception.TenantContextMismatchException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionService Unit Tests")
class SubscriptionServiceTest {

  @Mock
  private SubscriptionMapper subscriptionMapper;

  @Mock
  private RefundMapper refundMapper;

  @Mock
  private SubscriptionSubjectResolver subjectResolver;

  @Mock
  private PaymentGatewayPort paymentGatewayPort;

  @Mock
  private BillingSettingsMapper billingSettingsMapper;

  @InjectMocks
  private SubscriptionService subscriptionService;

  @Test
  @DisplayName("Should return active subscription by tenant key")
  void shouldReturnActiveSubscriptionByTenantKey() {
    // Arrange
    final String tenantKey = "tenant-123";
    final Subscription subscription = createSubscription(tenantKey, "active");
    when(subscriptionMapper.findActiveByTenantKey(tenantKey)).thenReturn(Optional.of(subscription));

    // Act
    final Subscription result = subscriptionService.getActiveByTenantKey(tenantKey);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getTenantKey()).isEqualTo(tenantKey);
    assertThat(result.getStatus()).isEqualTo("active");
    verify(subscriptionMapper).findActiveByTenantKey(tenantKey);
  }

  @Test
  @DisplayName("Should throw ResourceNotFoundException when no active subscription exists for tenant")
  void shouldThrowExceptionWhenNoActiveSubscriptionForTenant() {
    // Arrange
    final String tenantKey = "tenant-123";
    when(subscriptionMapper.findActiveByTenantKey(tenantKey)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> subscriptionService.getActiveByTenantKey(tenantKey))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("No active subscription found for tenantKey=" + tenantKey);
  }

  @Test
  @DisplayName("Should return all subscriptions by tenant key")
  void shouldReturnAllSubscriptionsByTenantKey() {
    // Arrange
    final String tenantKey = "tenant-123";
    final List<Subscription> subscriptions = List.of(
        createSubscription(tenantKey, "active"),
        createSubscription(tenantKey, "canceled")
    );
    when(subscriptionMapper.findAllByTenantKey(tenantKey)).thenReturn(subscriptions);

    // Act
    final List<Subscription> result = subscriptionService.getAllByTenantKey(tenantKey);

    // Assert
    assertThat(result).hasSize(2);
    assertThat(result).extracting(Subscription::getTenantKey).containsOnly(tenantKey);
    verify(subscriptionMapper).findAllByTenantKey(tenantKey);
  }

  @Test
  @DisplayName("Should return active subscription by subject")
  void shouldReturnActiveSubscriptionBySubject() {
    // Arrange
    final String tenantKey = "tenant-123";
    final UUID userId = UUID.randomUUID();
    final SubscriptionSubject subject = new SubscriptionSubject(SubjectType.TENANT, tenantKey);
    final Subscription subscription = createSubscription(tenantKey, "active");

    when(subjectResolver.resolveSubject(tenantKey, userId)).thenReturn(subject);
    when(subscriptionMapper.findActiveBySubject("TENANT", tenantKey)).thenReturn(Optional.of(subscription));

    // Act
    final Subscription result = subscriptionService.getActiveBySubject(tenantKey, userId);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getTenantKey()).isEqualTo(tenantKey);
    assertThat(result.getStatus()).isEqualTo("active");
    verify(subjectResolver).resolveSubject(tenantKey, userId);
    verify(subscriptionMapper).findActiveBySubject("TENANT", tenantKey);
  }

  @Test
  @DisplayName("Should throw ResourceNotFoundException when no active subscription exists for subject")
  void shouldThrowExceptionWhenNoActiveSubscriptionForSubject() {
    // Arrange
    final String tenantKey = "tenant-123";
    final UUID userId = UUID.randomUUID();
    final SubscriptionSubject subject = new SubscriptionSubject(SubjectType.USER, userId.toString());

    when(subjectResolver.resolveSubject(tenantKey, userId)).thenReturn(subject);
    when(subscriptionMapper.findActiveBySubject("USER", userId.toString())).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> subscriptionService.getActiveBySubject(tenantKey, userId))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("No active subscription found for subject type=USER");
  }

  @Test
  @DisplayName("Should return all subscriptions by subject")
  void shouldReturnAllSubscriptionsBySubject() {
    // Arrange
    final String tenantKey = "tenant-123";
    final UUID userId = UUID.randomUUID();
    final SubscriptionSubject subject = new SubscriptionSubject(SubjectType.TENANT, tenantKey);
    final List<Subscription> subscriptions = List.of(
        createSubscription(tenantKey, "active"),
        createSubscription(tenantKey, "trialing")
    );

    when(subjectResolver.resolveSubject(tenantKey, userId)).thenReturn(subject);
    when(subscriptionMapper.findBySubject("TENANT", tenantKey)).thenReturn(subscriptions);

    // Act
    final List<Subscription> result = subscriptionService.getAllBySubject(tenantKey, userId);

    // Assert
    assertThat(result).hasSize(2);
    assertThat(result).extracting(Subscription::getTenantKey).containsOnly(tenantKey);
    verify(subjectResolver).resolveSubject(tenantKey, userId);
    verify(subscriptionMapper).findBySubject("TENANT", tenantKey);
  }

  @Test
  @DisplayName("Should update subscription when tenant matches")
  void shouldUpdateSubscriptionWhenTenantMatches() {
    // Arrange
    final String tenantKey = "tenant-123";
    final String externalSubId = "sub_abc";
    final var subscription = createSubscription(tenantKey, "active");
    subscription.setExternalSubscriptionId(externalSubId);
    final var request = new SubscriptionDtos.UpdateSubscriptionRequest("price_456", 2L, "always_invoice");

    when(subscriptionMapper.findByExternalSubscriptionId(externalSubId)).thenReturn(Optional.of(subscription));

    // Act
    subscriptionService.updateSubscription(tenantKey, externalSubId, request);

    // Assert
    verify(subscriptionMapper).findByExternalSubscriptionId(externalSubId);
    verify(paymentGatewayPort).updateSubscription(any(UpdateSubscriptionCommand.class));
  }

  @Test
  @DisplayName("Should throw TenantContextMismatchException when updating subscription of another tenant")
  void shouldThrowExceptionWhenUpdatingSubscriptionOfAnotherTenant() {
    // Arrange
    final String tenantKey = "tenant-123";
    final String externalSubId = "sub_abc";
    final var subscription = createSubscription("other-tenant", "active");
    subscription.setExternalSubscriptionId(externalSubId);
    final var request = new SubscriptionDtos.UpdateSubscriptionRequest("price_456", 2L, "always_invoice");

    when(subscriptionMapper.findByExternalSubscriptionId(externalSubId)).thenReturn(Optional.of(subscription));

    // Act & Assert
    assertThatThrownBy(() -> subscriptionService.updateSubscription(tenantKey, externalSubId, request))
        .isInstanceOf(TenantContextMismatchException.class)
        .hasMessageContaining("does not belong to tenant");
  }

  @Test
  @DisplayName("Should create refund when tenant matches")
  void shouldCreateRefundWhenTenantMatches() {
    // Arrange
    final String tenantKey = "tenant-123";
    final String externalCustomerId = "cus_abc";
    final var settings = new BillingSettings();
    settings.setTenantKey(tenantKey);
    settings.setExternalCustomerId(externalCustomerId);
    final var request = new SubscriptionDtos.CreateRefundRequest("ch_123", 1000L, "requested_by_customer");

    when(billingSettingsMapper.findByTenantKey(tenantKey)).thenReturn(Optional.of(settings));
    when(paymentGatewayPort.createRefund(any())).thenReturn("ref_123");

    // Act
    final var result = subscriptionService.createRefund(tenantKey, request);

    // Assert
    assertThat(result.refundId()).isEqualTo("ref_123");
    verify(billingSettingsMapper).findByTenantKey(tenantKey);
    verify(paymentGatewayPort).createRefund(any());
  }

  private Subscription createSubscription(final String tenantKey, final String status) {
    final var subscription = new Subscription();
    subscription.setId(UUID.randomUUID());
    subscription.setExternalSubscriptionId("sub_" + UUID.randomUUID());
    subscription.setExternalCustomerId("cus_" + UUID.randomUUID());
    subscription.setTenantKey(tenantKey);
    subscription.setStatus(status);
    subscription.setPlanId("price_123");
    subscription.setCurrentPeriodStart(Instant.now());
    subscription.setCurrentPeriodEnd(Instant.now().plusSeconds(2592000)); // 30 days
    return subscription;
  }
}
