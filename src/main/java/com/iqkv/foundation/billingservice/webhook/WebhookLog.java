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

import java.time.Instant;
import java.util.UUID;

/**
 * Audit log entry for an inbound payment gateway webhook event.
 *
 * <p>Provides idempotency guard and audit trail for webhook delivery.
 * {@code externalEventId} is unique — duplicate deliveries are detected via
 * {@code WebhookLogMapper#existsByExternalEventId} before any business logic runs.
 *
 * <p>Status lifecycle: {@code RECEIVED} → {@code PROCESSED} on success,
 * or {@code RECEIVED} → {@code FAILED} on error.
 */
public class WebhookLog {

  private UUID id;
  private String externalEventId;  // payment-gateway-agnostic (e.g. Stripe evt_xxx)
  private String eventType;
  private String tenantKey;        // tenant key for organization context
  private String status;           // RECEIVED | PROCESSED | FAILED
  private String errorMessage;     // null unless FAILED
  private Instant receivedAt;
  private Instant processedAt;     // null until PROCESSED

  public WebhookLog() {
  }

  public WebhookLog(final UUID id,
                    final String externalEventId,
                    final String eventType,
                    final String status,
                    final String errorMessage,
                    final Instant receivedAt,
                    final Instant processedAt) {
    this(id, externalEventId, eventType, null, status, errorMessage, receivedAt, processedAt);
  }

  public WebhookLog(final UUID id,
                    final String externalEventId,
                    final String eventType,
                    final String tenantKey,
                    final String status,
                    final String errorMessage,
                    final Instant receivedAt,
                    final Instant processedAt) {
    this.id = id;
    this.externalEventId = externalEventId;
    this.eventType = eventType;
    this.tenantKey = tenantKey;
    this.status = status;
    this.errorMessage = errorMessage;
    this.receivedAt = receivedAt;
    this.processedAt = processedAt;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getExternalEventId() {
    return externalEventId;
  }

  public void setExternalEventId(String externalEventId) {
    this.externalEventId = externalEventId;
  }

  public String getEventType() {
    return eventType;
  }

  public void setEventType(String eventType) {
    this.eventType = eventType;
  }

  public String getTenantKey() {
    return tenantKey;
  }

  public void setTenantKey(String tenantKey) {
    this.tenantKey = tenantKey;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public Instant getReceivedAt() {
    return receivedAt;
  }

  public void setReceivedAt(Instant receivedAt) {
    this.receivedAt = receivedAt;
  }

  public Instant getProcessedAt() {
    return processedAt;
  }

  public void setProcessedAt(Instant processedAt) {
    this.processedAt = processedAt;
  }
}
