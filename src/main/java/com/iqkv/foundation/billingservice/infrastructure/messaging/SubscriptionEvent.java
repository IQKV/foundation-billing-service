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
 * Represents subscription lifecycle events published by the Billing service.
 *
 * <p>{@code subjectType} and {@code subjectKey} identify the subscription owner:
 * {@code TENANT}/{@code tenantKey} in multi-tenant mode, or {@code USER}/{@code userId}
 * in single-tenant mode. Downstream consumers should use these fields to evaluate
 * entitlements consistently regardless of rollout mode.
 *
 * <p>{@code planCode} carries the human-readable plan code (e.g. {@code "pro-monthly"})
 * so that IAM can cache it on the tenant and stamp it into JWT tokens without needing
 * knowledge of the billing plan catalog.
 */
public class SubscriptionEvent implements AuditableEvent {

  public enum EventType {
    SUBSCRIPTION_CREATED,
    SUBSCRIPTION_UPDATED,
    SUBSCRIPTION_CANCELLED
  }

  private String tenantKey;
  private String externalSubscriptionId;
  private EventType eventType;
  private Instant occurredAt;
  private String subjectType;   // TENANT | USER
  private String subjectKey;    // tenantKey or userId depending on subjectType
  private String planCode;      // human-readable plan code (e.g. "pro-monthly"); nullable
  private Long seatCount;       // purchased seat count for PER_SEAT plans; null for FLAT plans
  private AuditActor actor;

  /**
   * No-args constructor for deserialization.
   */
  public SubscriptionEvent() {
  }

  @Override
  public AuditActor getActor() {
    return actor;
  }

  @Override
  public void setActor(final AuditActor actor) {
    this.actor = actor;
  }

  /**
   * All-args constructor.
   *
   * @param tenantKey              the tenant key
   * @param externalSubscriptionId the external subscription ID (payment-gateway-agnostic)
   * @param eventType              the event type
   * @param occurredAt             the timestamp when the event occurred
   * @param subjectType            the subject type (TENANT or USER)
   * @param subjectKey             the subject key (tenantKey or userId)
   * @param planCode               the human-readable plan code; may be null
   * @param seatCount              purchased seat count for PER_SEAT plans; null for FLAT plans
   */
  public SubscriptionEvent(final String tenantKey, final String externalSubscriptionId,
                           final EventType eventType, final Instant occurredAt,
                           final String subjectType, final String subjectKey,
                           final String planCode, final Long seatCount) {
    this.tenantKey = tenantKey;
    this.externalSubscriptionId = externalSubscriptionId;
    this.eventType = eventType;
    this.occurredAt = occurredAt;
    this.subjectType = subjectType;
    this.subjectKey = subjectKey;
    this.planCode = planCode;
    this.seatCount = seatCount;
  }

  public String getTenantKey() {
    return tenantKey;
  }

  public void setTenantKey(final String tenantKey) {
    this.tenantKey = tenantKey;
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

  public String getPlanCode() {
    return planCode;
  }

  public void setPlanCode(final String planCode) {
    this.planCode = planCode;
  }

  public Long getSeatCount() {
    return seatCount;
  }

  public void setSeatCount(final Long seatCount) {
    this.seatCount = seatCount;
  }
}
