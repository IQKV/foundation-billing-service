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
 * Represents invoice lifecycle events published by the Billing service.
 *
 * <p>{@code subjectType} and {@code subjectKey} identify the invoice owner:
 * {@code TENANT}/{@code tenantKey} in multi-tenant mode, or {@code USER}/{@code userId}
 * in single-tenant mode.
 */
public class InvoiceEvent implements AuditableEvent {

  public enum EventType {
    INVOICE_PAID,
    INVOICE_CREATED,
    INVOICE_FINALIZED,
    INVOICE_UPDATED
  }

  private String tenantKey;
  private String externalInvoiceId;
  private String externalCustomerId;
  private String externalSubscriptionId;
  private EventType eventType;
  private Long amountPaid;        // in minor currency units (cents)
  private String currency;
  private Instant occurredAt;
  private String subjectType;     // TENANT | USER
  private String subjectKey;      // tenantKey or userId depending on subjectType
  private AuditActor actor;

  /**
   * No-args constructor for deserialization.
   */
  public InvoiceEvent() {
  }

  /**
   * All-args constructor.
   */
  public InvoiceEvent(final String tenantKey, final String externalInvoiceId,
                      final String externalCustomerId, final String externalSubscriptionId,
                      final EventType eventType, final Long amountPaid, final String currency,
                      final Instant occurredAt, final String subjectType, final String subjectKey) {
    this.tenantKey = tenantKey;
    this.externalInvoiceId = externalInvoiceId;
    this.externalCustomerId = externalCustomerId;
    this.externalSubscriptionId = externalSubscriptionId;
    this.eventType = eventType;
    this.amountPaid = amountPaid;
    this.currency = currency;
    this.occurredAt = occurredAt;
    this.subjectType = subjectType;
    this.subjectKey = subjectKey;
  }

  @Override
  public AuditActor getActor() {
    return actor;
  }

  @Override
  public void setActor(final AuditActor actor) {
    this.actor = actor;
  }

  public String getTenantKey() {
    return tenantKey;
  }

  public void setTenantKey(final String tenantKey) {
    this.tenantKey = tenantKey;
  }

  public String getExternalInvoiceId() {
    return externalInvoiceId;
  }

  public void setExternalInvoiceId(final String externalInvoiceId) {
    this.externalInvoiceId = externalInvoiceId;
  }

  public String getExternalCustomerId() {
    return externalCustomerId;
  }

  public void setExternalCustomerId(final String externalCustomerId) {
    this.externalCustomerId = externalCustomerId;
  }

  public String getExternalSubscriptionId() {
    return externalSubscriptionId;
  }

  public void setExternalSubscriptionId(final String externalSubscriptionId) {
    this.externalSubscriptionId = externalSubscriptionId;
  }

  public EventType getEventType() {
    return eventType;
  }

  public void setEventType(final EventType eventType) {
    this.eventType = eventType;
  }

  public Long getAmountPaid() {
    return amountPaid;
  }

  public void setAmountPaid(final Long amountPaid) {
    this.amountPaid = amountPaid;
  }

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(final String currency) {
    this.currency = currency;
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