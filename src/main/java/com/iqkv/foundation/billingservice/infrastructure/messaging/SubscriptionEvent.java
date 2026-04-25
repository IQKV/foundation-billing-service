package com.iqkv.foundation.billingservice.infrastructure.messaging;

import java.time.Instant;

/**
 * Represents subscription lifecycle events published by the Billing service.
 *
 * <p>{@code subjectType} and {@code subjectKey} identify the subscription owner:
 * {@code TENANT}/{@code tenantKey} in multi-tenant mode, or {@code USER}/{@code userId}
 * in single-tenant mode. Downstream consumers should use these fields to evaluate
 * entitlements consistently regardless of rollout mode.
 */
public class SubscriptionEvent {

  public enum EventType {
    SUBSCRIPTION_CANCELLED
  }

  private String tenantKey;
  private String externalSubscriptionId;
  private EventType eventType;
  private Instant occurredAt;
  private String subjectType;   // TENANT | USER
  private String subjectKey;    // tenantKey or userId depending on subjectType

  /**
   * No-args constructor for deserialization.
   */
  public SubscriptionEvent() {
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
   */
  public SubscriptionEvent(final String tenantKey, final String externalSubscriptionId,
                           final EventType eventType, final Instant occurredAt,
                           final String subjectType, final String subjectKey) {
    this.tenantKey = tenantKey;
    this.externalSubscriptionId = externalSubscriptionId;
    this.eventType = eventType;
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
}
