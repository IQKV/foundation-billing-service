package com.iqkv.foundation.billingservice.infrastructure.messaging;

import java.time.Instant;

/**
 * Represents subscription lifecycle events published by the Billing service.
 */
public class SubscriptionEvent {

  public enum EventType {
    SUBSCRIPTION_CANCELLED
  }

  private String tenantKey;
  private String externalSubscriptionId;
  private EventType eventType;
  private Instant occurredAt;

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
   */
  public SubscriptionEvent(String tenantKey, String externalSubscriptionId,
                           EventType eventType, Instant occurredAt) {
    this.tenantKey = tenantKey;
    this.externalSubscriptionId = externalSubscriptionId;
    this.eventType = eventType;
    this.occurredAt = occurredAt;
  }

  public String getTenantKey() {
    return tenantKey;
  }

  public void setTenantKey(String tenantKey) {
    this.tenantKey = tenantKey;
  }

  public String getExternalSubscriptionId() {
    return externalSubscriptionId;
  }

  public void setExternalSubscriptionId(String externalSubscriptionId) {
    this.externalSubscriptionId = externalSubscriptionId;
  }

  public EventType getEventType() {
    return eventType;
  }

  public void setEventType(EventType eventType) {
    this.eventType = eventType;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }

  public void setOccurredAt(Instant occurredAt) {
    this.occurredAt = occurredAt;
  }
}
