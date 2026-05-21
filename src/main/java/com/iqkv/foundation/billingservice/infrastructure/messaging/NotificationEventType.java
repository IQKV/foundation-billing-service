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

/**
 * Billing-domain notification event types.
 * Each value maps to an email template and a subject message key.
 */
public enum NotificationEventType {
  /**
   * Sent to the billing contact when a new subscription becomes active.
   */
  SUBSCRIPTION_ACTIVATED,
  /**
   * Sent to the billing contact when a subscription is updated (plan change, quantity, etc.).
   */
  SUBSCRIPTION_UPDATED,
  /**
   * Sent to the billing contact when a subscription is cancelled.
   */
  SUBSCRIPTION_CANCELLED,
  /**
   * Sent to the billing contact when a trial is about to expire.
   */
  TRIAL_ENDING,
  /**
   * Sent to the billing contact when a payment is overdue.
   */
  PAYMENT_OVERDUE,
  /**
   * Sent to the billing contact when a payment fails.
   */
  PAYMENT_FAILED,
  /**
   * Sent to the billing contact when a payment succeeds / invoice is paid.
   */
  INVOICE_PAID,
  /**
   * Sent to the billing contact when billing settings are updated.
   */
  BILLING_UPDATED,
  /**
   * Sent to the billing contact when account is suspended.
   */
  ACCOUNT_SUSPENDED,
  /**
   * Sent to the billing contact when a refund is processed.
   */
  REFUND_CREATED
}
