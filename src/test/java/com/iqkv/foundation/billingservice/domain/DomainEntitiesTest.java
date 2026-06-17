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

package com.iqkv.foundation.billingservice.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import com.iqkv.foundation.billingservice.plan.Plan;
import com.iqkv.foundation.billingservice.settings.BillingSettings;
import com.iqkv.foundation.billingservice.subscription.Refund;
import com.iqkv.foundation.billingservice.subscription.Subscription;
import com.iqkv.foundation.billingservice.userbilling.UserBillingSettings;
import com.iqkv.foundation.billingservice.webhook.WebhookLog;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Domain Entities Unit Tests")
class DomainEntitiesTest {

  // ─── BillingSettings ──────────────────────────────────────────────────────

  @Test
  @DisplayName("Should create BillingSettings via all-args constructor")
  void shouldCreateBillingSettingsViaAllArgsConstructor() {
    // Arrange
    final var id = UUID.randomUUID();
    final var ownerId = UUID.randomUUID();
    final var now = LocalDateTime.now();

    // Act
    final var settings = new BillingSettings(
        id, "tenant-123", "cus_ext123", "billing@example.com",
        "Acme Corp", "{\"street\":\"123 Main\"}", "TAX999", "VAT",
        "USD", ownerId, now, now
    );

    // Assert
    assertThat(settings.getId()).isEqualTo(id);
    assertThat(settings.getTenantKey()).isEqualTo("tenant-123");
    assertThat(settings.getExternalCustomerId()).isEqualTo("cus_ext123");
    assertThat(settings.getBillingEmail()).isEqualTo("billing@example.com");
    assertThat(settings.getCompanyName()).isEqualTo("Acme Corp");
    assertThat(settings.getBillingAddress()).isEqualTo("{\"street\":\"123 Main\"}");
    assertThat(settings.getTaxId()).isEqualTo("TAX999");
    assertThat(settings.getTaxIdType()).isEqualTo("VAT");
    assertThat(settings.getCurrency()).isEqualTo("USD");
    assertThat(settings.getProfileOwnerId()).isEqualTo(ownerId);
    assertThat(settings.getCreatedAt()).isEqualTo(now);
    assertThat(settings.getUpdatedAt()).isEqualTo(now);
  }

  @Test
  @DisplayName("Should create BillingSettings via no-args constructor and setters")
  void shouldCreateBillingSettingsViaSetters() {
    // Arrange
    final var id = UUID.randomUUID();
    final var settings = new BillingSettings();

    // Act
    settings.setId(id);
    settings.setTenantKey("tenant-s");
    settings.setExternalCustomerId("cus_s");
    settings.setBillingEmail("s@example.com");
    settings.setCompanyName("Setters Inc");
    settings.setBillingAddress(null);
    settings.setTaxId(null);
    settings.setTaxIdType(null);
    settings.setCurrency("EUR");
    settings.setProfileOwnerId(null);
    settings.setCreatedAt(LocalDateTime.now());
    settings.setUpdatedAt(LocalDateTime.now());

    // Assert
    assertThat(settings.getId()).isEqualTo(id);
    assertThat(settings.getCurrency()).isEqualTo("EUR");
    assertThat(settings.getBillingAddress()).isNull();
    assertThat(settings.getProfileOwnerId()).isNull();
  }

  // ─── Plan ─────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("Should create Plan via all-args constructor")
  void shouldCreatePlanViaAllArgsConstructor() {
    // Arrange
    final var id = UUID.randomUUID();
    final var now = LocalDateTime.now();

    // Act
    final var plan = new Plan(
        id, "pro-monthly", "Professional Monthly",
        "Professional plan description",
        "MONTHLY", 2999, "USD",
        "{\"feature\":true}", "TENANT",
        "prod_ext123", "price_ext456", true, now, now
    );

    // Assert
    assertThat(plan.getId()).isEqualTo(id);
    assertThat(plan.getPlanCode()).isEqualTo("pro-monthly");
    assertThat(plan.getDisplayName()).isEqualTo("Professional Monthly");
    assertThat(plan.getDescription()).isEqualTo("Professional plan description");
    assertThat(plan.getBillingPeriod()).isEqualTo("MONTHLY");
    assertThat(plan.getPriceMinor()).isEqualTo(2999);
    assertThat(plan.getCurrency()).isEqualTo("USD");
    assertThat(plan.getFeatureSet()).isEqualTo("{\"feature\":true}");
    assertThat(plan.getScope()).isEqualTo("TENANT");
    assertThat(plan.getExternalProductId()).isEqualTo("prod_ext123");
    assertThat(plan.getExternalPriceId()).isEqualTo("price_ext456");
    assertThat(plan.getActive()).isTrue();
  }

  @Test
  @DisplayName("Should create Plan via no-args constructor and setters")
  void shouldCreatePlanViaSetters() {
    // Arrange
    final var id = UUID.randomUUID();
    final var plan = new Plan();

    // Act
    plan.setId(id);
    plan.setPlanCode("basic-annual");
    plan.setDisplayName("Basic Annual");
    plan.setBillingPeriod("ANNUAL");
    plan.setPriceMinor(9999);
    plan.setCurrency("GBP");
    plan.setFeatureSet(null);
    plan.setScope("USER");
    plan.setExternalProductId(null);
    plan.setExternalPriceId(null);
    plan.setActive(false);
    plan.setCreatedAt(LocalDateTime.now());
    plan.setUpdatedAt(LocalDateTime.now());

    // Assert
    assertThat(plan.getPlanCode()).isEqualTo("basic-annual");
    assertThat(plan.getBillingPeriod()).isEqualTo("ANNUAL");
    assertThat(plan.getActive()).isFalse();
    assertThat(plan.getFeatureSet()).isNull();
  }

  // ─── Subscription ─────────────────────────────────────────────────────────

  @Test
  @DisplayName("Should create Subscription via all-args constructor")
  void shouldCreateSubscriptionViaAllArgsConstructor() {
    // Arrange
    final var id = UUID.randomUUID();
    final var now = Instant.now();
    final var created = LocalDateTime.now();

    // Act
    final var sub = new Subscription(
        id, "tenant-123", "sub_ext123", "cus_ext456",
        "active", "price_789", 3L,
        now, now.plusSeconds(604800),
        now, now.plusSeconds(2592000),
        false, null,
        "TENANT", "tenant-123", created, created
    );

    // Assert
    assertThat(sub.getId()).isEqualTo(id);
    assertThat(sub.getTenantKey()).isEqualTo("tenant-123");
    assertThat(sub.getExternalSubscriptionId()).isEqualTo("sub_ext123");
    assertThat(sub.getExternalCustomerId()).isEqualTo("cus_ext456");
    assertThat(sub.getStatus()).isEqualTo("active");
    assertThat(sub.getPlanId()).isEqualTo("price_789");
    assertThat(sub.getQuantity()).isEqualTo(3L);
    assertThat(sub.getTrialStart()).isEqualTo(now);
    assertThat(sub.isCancelAtPeriodEnd()).isFalse();
    assertThat(sub.getCanceledAt()).isNull();
    assertThat(sub.getSubjectType()).isEqualTo("TENANT");
    assertThat(sub.getSubjectKey()).isEqualTo("tenant-123");
  }

  @Test
  @DisplayName("Should create Subscription via no-args constructor and setters")
  void shouldCreateSubscriptionViaSetters() {
    // Arrange
    final var id = UUID.randomUUID();
    final var now = Instant.now();
    final var sub = new Subscription();

    // Act
    sub.setId(id);
    sub.setTenantKey("tenant-s");
    sub.setExternalSubscriptionId("sub_s");
    sub.setExternalCustomerId("cus_s");
    sub.setStatus("trialing");
    sub.setPlanId("price_s");
    sub.setQuantity(null);
    sub.setTrialStart(now);
    sub.setTrialEnd(now.plusSeconds(604800));
    sub.setCurrentPeriodStart(now);
    sub.setCurrentPeriodEnd(now.plusSeconds(2592000));
    sub.setCancelAtPeriodEnd(true);
    sub.setCanceledAt(null);
    sub.setSubjectType("USER");
    sub.setSubjectKey("user-s");
    sub.setCreatedAt(LocalDateTime.now());
    sub.setUpdatedAt(LocalDateTime.now());

    // Assert
    assertThat(sub.getStatus()).isEqualTo("trialing");
    assertThat(sub.isCancelAtPeriodEnd()).isTrue();
    assertThat(sub.getQuantity()).isNull();
    assertThat(sub.getSubjectType()).isEqualTo("USER");
  }

  // ─── Refund ───────────────────────────────────────────────────────────────

  @Test
  @DisplayName("Should create Refund via all-args constructor")
  void shouldCreateRefundViaAllArgsConstructor() {
    // Arrange
    final var id = UUID.randomUUID();
    final var now = Instant.now();
    final var created = LocalDateTime.now();

    // Act
    final var refund = new Refund(
        id, "tenant-123", "re_ext123", "ch_ext456", "cus_ext789",
        1500L, "usd", "succeeded", now, created, created
    );

    // Assert
    assertThat(refund.getId()).isEqualTo(id);
    assertThat(refund.getTenantKey()).isEqualTo("tenant-123");
    assertThat(refund.getExternalRefundId()).isEqualTo("re_ext123");
    assertThat(refund.getExternalPaymentId()).isEqualTo("ch_ext456");
    assertThat(refund.getExternalCustomerId()).isEqualTo("cus_ext789");
    assertThat(refund.getAmount()).isEqualTo(1500L);
    assertThat(refund.getCurrency()).isEqualTo("usd");
    assertThat(refund.getStatus()).isEqualTo("succeeded");
    assertThat(refund.getOccurredAt()).isEqualTo(now);
  }

  @Test
  @DisplayName("Should create Refund via no-args constructor and setters")
  void shouldCreateRefundViaSetters() {
    // Arrange
    final var id = UUID.randomUUID();
    final var refund = new Refund();

    // Act
    refund.setId(id);
    refund.setTenantKey("tenant-r");
    refund.setExternalRefundId("re_r");
    refund.setExternalPaymentId("ch_r");
    refund.setExternalCustomerId(null);
    refund.setAmount(2000L);
    refund.setCurrency("eur");
    refund.setStatus("pending");
    refund.setOccurredAt(Instant.now());
    refund.setCreatedAt(LocalDateTime.now());
    refund.setUpdatedAt(LocalDateTime.now());

    // Assert
    assertThat(refund.getAmount()).isEqualTo(2000L);
    assertThat(refund.getExternalCustomerId()).isNull();
    assertThat(refund.getCurrency()).isEqualTo("eur");
  }

  // ─── WebhookLog ───────────────────────────────────────────────────────────

  @Test
  @DisplayName("Should create WebhookLog via all-args constructor")
  void shouldCreateWebhookLogViaAllArgsConstructor() {
    // Arrange
    final var id = UUID.randomUUID();
    final var receivedAt = Instant.now();
    final var processedAt = Instant.now().plusSeconds(1);

    // Act
    final var log = new WebhookLog(
        id, "evt_ext123", "customer.subscription.created",
        "PROCESSED", null, receivedAt, processedAt
    );

    // Assert
    assertThat(log.getId()).isEqualTo(id);
    assertThat(log.getExternalEventId()).isEqualTo("evt_ext123");
    assertThat(log.getEventType()).isEqualTo("customer.subscription.created");
    assertThat(log.getStatus()).isEqualTo("PROCESSED");
    assertThat(log.getErrorMessage()).isNull();
    assertThat(log.getReceivedAt()).isEqualTo(receivedAt);
    assertThat(log.getProcessedAt()).isEqualTo(processedAt);
  }

  @Test
  @DisplayName("Should create WebhookLog for failed status via setters")
  void shouldCreateWebhookLogForFailedStatus() {
    // Arrange
    final var log = new WebhookLog();

    // Act
    log.setId(UUID.randomUUID());
    log.setExternalEventId("evt_fail");
    log.setEventType("invoice.payment_failed");
    log.setStatus("FAILED");
    log.setErrorMessage("Unexpected gateway error");
    log.setReceivedAt(Instant.now());
    log.setProcessedAt(null);

    // Assert
    assertThat(log.getStatus()).isEqualTo("FAILED");
    assertThat(log.getErrorMessage()).isEqualTo("Unexpected gateway error");
    assertThat(log.getProcessedAt()).isNull();
  }

  // ─── UserBillingSettings ──────────────────────────────────────────────────

  @Test
  @DisplayName("Should create UserBillingSettings via all-args constructor")
  void shouldCreateUserBillingSettingsViaAllArgsConstructor() {
    // Arrange
    final var id = UUID.randomUUID();
    final var userId = UUID.randomUUID();
    final var now = LocalDateTime.now();

    // Act
    final var settings = new UserBillingSettings(
        id, userId, "cus_ext123", "user@example.com",
        "User Corp", "{\"address\":\"456 User St\"}",
        "USR999", "SSN", "USD", now, now
    );

    // Assert
    assertThat(settings.getId()).isEqualTo(id);
    assertThat(settings.getUserId()).isEqualTo(userId);
    assertThat(settings.getExternalCustomerId()).isEqualTo("cus_ext123");
    assertThat(settings.getBillingEmail()).isEqualTo("user@example.com");
    assertThat(settings.getCompanyName()).isEqualTo("User Corp");
    assertThat(settings.getBillingAddress()).isEqualTo("{\"address\":\"456 User St\"}");
    assertThat(settings.getTaxId()).isEqualTo("USR999");
    assertThat(settings.getTaxIdType()).isEqualTo("SSN");
    assertThat(settings.getCurrency()).isEqualTo("USD");
    assertThat(settings.getCreatedAt()).isEqualTo(now);
  }

  @Test
  @DisplayName("Should create UserBillingSettings via no-args constructor and setters")
  void shouldCreateUserBillingSettingsViaSetters() {
    // Arrange
    final var userId = UUID.randomUUID();
    final var settings = new UserBillingSettings();

    // Act
    settings.setId(UUID.randomUUID());
    settings.setUserId(userId);
    settings.setExternalCustomerId(null);
    settings.setBillingEmail(null);
    settings.setCompanyName(null);
    settings.setBillingAddress(null);
    settings.setTaxId(null);
    settings.setTaxIdType(null);
    settings.setCurrency("JPY");
    settings.setCreatedAt(LocalDateTime.now());
    settings.setUpdatedAt(LocalDateTime.now());

    // Assert
    assertThat(settings.getUserId()).isEqualTo(userId);
    assertThat(settings.getCurrency()).isEqualTo("JPY");
    assertThat(settings.getExternalCustomerId()).isNull();
  }
}
