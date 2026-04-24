package com.iqkv.foundation.billingservice.infrastructure.messaging;

import java.time.Instant;

/**
 * Mirror of IAM's enriched TenantEvent for Billing service.
 * This is an independent class that represents tenant lifecycle events.
 */
public class TenantEvent {

  public enum EventType {
    TENANT_CREATED,
    TENANT_UPDATED,
    TENANT_DELETED,
    TENANT_SUSPENDED
  }

  private String tenantKey;
  private String tenantName;
  private String ownerEmail;
  private String ownerFirstName;
  private EventType eventType;
  private Instant occurredAt;

  /**
   * No-args constructor for deserialization.
   */
  public TenantEvent() {
  }

  /**
   * All-args constructor.
   *
   * @param tenantKey      the tenant key
   * @param tenantName     the tenant name
   * @param ownerEmail     the owner email (non-null for TENANT_CREATED, null otherwise)
   * @param ownerFirstName the owner first name (non-null for TENANT_CREATED, null otherwise)
   * @param eventType      the event type
   * @param occurredAt     the timestamp when the event occurred
   */
  public TenantEvent(String tenantKey, String tenantName, String ownerEmail,
                     String ownerFirstName, EventType eventType, Instant occurredAt) {
    this.tenantKey = tenantKey;
    this.tenantName = tenantName;
    this.ownerEmail = ownerEmail;
    this.ownerFirstName = ownerFirstName;
    this.eventType = eventType;
    this.occurredAt = occurredAt;
  }

  public String getTenantKey() {
    return tenantKey;
  }

  public void setTenantKey(String tenantKey) {
    this.tenantKey = tenantKey;
  }

  public String getTenantName() {
    return tenantName;
  }

  public void setTenantName(String tenantName) {
    this.tenantName = tenantName;
  }

  public String getOwnerEmail() {
    return ownerEmail;
  }

  public void setOwnerEmail(String ownerEmail) {
    this.ownerEmail = ownerEmail;
  }

  public String getOwnerFirstName() {
    return ownerFirstName;
  }

  public void setOwnerFirstName(String ownerFirstName) {
    this.ownerFirstName = ownerFirstName;
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
