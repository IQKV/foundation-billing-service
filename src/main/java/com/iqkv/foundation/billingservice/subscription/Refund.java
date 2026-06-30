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
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain class representing a cached payment gateway refund record.
 */
public class Refund {

  private UUID id;
  private String tenantKey;
  private String externalRefundId;
  private String externalPaymentId;
  private String externalCustomerId;
  private Long amount;
  private String currency;
  private String status;
  private Instant occurredAt;
  private String gatewayType;             // STRIPE | LEMON_SQUEEZY
  private String subjectType;             // TENANT | USER
  private String subjectKey;              // tenantKey or userId depending on subjectType
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public Refund() {
  }

  public Refund(final UUID id,
                final String tenantKey,
                final String externalRefundId,
                final String externalPaymentId,
                final String externalCustomerId,
                final Long amount,
                final String currency,
                final String status,
                final Instant occurredAt,
                final String gatewayType,
                final String subjectType,
                final String subjectKey,
                final LocalDateTime createdAt,
                final LocalDateTime updatedAt) {
    this.id = id;
    this.tenantKey = tenantKey;
    this.externalRefundId = externalRefundId;
    this.externalPaymentId = externalPaymentId;
    this.externalCustomerId = externalCustomerId;
    this.amount = amount;
    this.currency = currency;
    this.status = status;
    this.occurredAt = occurredAt;
    this.gatewayType = gatewayType;
    this.subjectType = subjectType;
    this.subjectKey = subjectKey;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getTenantKey() {
    return tenantKey;
  }

  public void setTenantKey(String tenantKey) {
    this.tenantKey = tenantKey;
  }

  public String getExternalRefundId() {
    return externalRefundId;
  }

  public void setExternalRefundId(String externalRefundId) {
    this.externalRefundId = externalRefundId;
  }

  public String getExternalPaymentId() {
    return externalPaymentId;
  }

  public void setExternalPaymentId(String externalPaymentId) {
    this.externalPaymentId = externalPaymentId;
  }

  public String getExternalCustomerId() {
    return externalCustomerId;
  }

  public void setExternalCustomerId(String externalCustomerId) {
    this.externalCustomerId = externalCustomerId;
  }

  public Long getAmount() {
    return amount;
  }

  public void setAmount(Long amount) {
    this.amount = amount;
  }

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }

  public void setOccurredAt(Instant occurredAt) {
    this.occurredAt = occurredAt;
  }

  public String getGatewayType() {
    return gatewayType;
  }

  public void setGatewayType(String gatewayType) {
    this.gatewayType = gatewayType;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  public String getSubjectType() {
    return subjectType;
  }

  public void setSubjectType(String subjectType) {
    this.subjectType = subjectType;
  }

  public String getSubjectKey() {
    return subjectKey;
  }

  public void setSubjectKey(String subjectKey) {
    this.subjectKey = subjectKey;
  }
}
