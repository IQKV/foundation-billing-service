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

package com.iqkv.foundation.billingservice.infrastructure.messaging;

import java.time.Instant;

import com.iqkv.foundation.audit.model.event.AuditActor;
import com.iqkv.foundation.audit.model.event.AuditableEvent;

/**
 * Represents refund events published by the Billing service.
 */
public class RefundEvent implements AuditableEvent {

  public enum EventType {
    REFUND_CREATED
  }

  private String tenantKey;
  private String externalRefundId;
  private String externalPaymentId;
  private String externalCustomerId;
  private EventType eventType;
  private Long amountRefunded;
  private String currency;
  private String status;
  private Instant occurredAt;
  private String subjectType;
  private String subjectKey;
  private AuditActor actor;

  public RefundEvent() {
  }

  @Override
  public AuditActor getActor() {
    return actor;
  }

  @Override
  public void setActor(final AuditActor actor) {
    this.actor = actor;
  }

  public RefundEvent(final String tenantKey, final String externalRefundId,
                     final String externalPaymentId, final String externalCustomerId,
                     final EventType eventType, final Long amountRefunded,
                     final String currency, final String status, final Instant occurredAt,
                     final String subjectType, final String subjectKey) {
    this.tenantKey = tenantKey;
    this.externalRefundId = externalRefundId;
    this.externalPaymentId = externalPaymentId;
    this.externalCustomerId = externalCustomerId;
    this.eventType = eventType;
    this.amountRefunded = amountRefunded;
    this.currency = currency;
    this.status = status;
    this.occurredAt = occurredAt;
    this.subjectType = subjectType;
    this.subjectKey = subjectKey;
  }

  public String getTenantKey() {
    return tenantKey;
  }

  public void setTenantKey(final String tenantKey) {
    this.tenantKey = tenantKey;
  }

  public String getExternalRefundId() {
    return externalRefundId;
  }

  public void setExternalRefundId(final String externalRefundId) {
    this.externalRefundId = externalRefundId;
  }

  public String getExternalPaymentId() {
    return externalPaymentId;
  }

  public void setExternalPaymentId(final String externalPaymentId) {
    this.externalPaymentId = externalPaymentId;
  }

  public String getExternalCustomerId() {
    return externalCustomerId;
  }

  public void setExternalCustomerId(final String externalCustomerId) {
    this.externalCustomerId = externalCustomerId;
  }

  public EventType getEventType() {
    return eventType;
  }

  public void setEventType(final EventType eventType) {
    this.eventType = eventType;
  }

  public Long getAmountRefunded() {
    return amountRefunded;
  }

  public void setAmountRefunded(final Long amountRefunded) {
    this.amountRefunded = amountRefunded;
  }

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(final String currency) {
    this.currency = currency;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(final String status) {
    this.status = status;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }

  public void setOccurredAt(final Instant occurredAt) {
    this.occurredAt = occurredAt;
  }

  public String getSubjectType() {
    return subjectType;
  }

  public void setSubjectType(final String subjectType) {
    this.subjectType = subjectType;
  }

  public String getSubjectKey() {
    return subjectKey;
  }

  public void setSubjectKey(final String subjectKey) {
    this.subjectKey = subjectKey;
  }
}
