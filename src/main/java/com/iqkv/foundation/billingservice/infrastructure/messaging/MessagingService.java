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

import com.iqkv.foundation.audit.spi.context.AuditEventEnricher;
import com.iqkv.foundation.billingservice.infrastructure.config.RabbitMQConfig;
import com.iqkv.foundation.billingservice.shared.exception.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class MessagingService {

  private static final Logger log = LoggerFactory.getLogger(MessagingService.class);

  private final RabbitTemplate rabbitTemplate;

  public MessagingService(final RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }

  public void publishSubscriptionCreated(final String tenantKey, final String externalSubscriptionId) {
    final var event = new SubscriptionEvent(
        tenantKey,
        externalSubscriptionId,
        SubscriptionEvent.EventType.SUBSCRIPTION_CREATED,
        Instant.now(),
        "TENANT",
        tenantKey,
        null,
        null
    );
    publish(RabbitMQConfig.EVENTS_EXCHANGE, RabbitMQConfig.ROUTING_SUBSCRIPTION_CREATED, event);
  }

  /**
   * Publishes a subscription.created event with explicit subject scope, plan code, and seat count.
   * Prefer this overload when the subject type and key are known (e.g. from the Subscription entity).
   *
   * @param seatCount purchased seat count for PER_SEAT plans; {@code null} for FLAT plans
   */
  public void publishSubscriptionCreated(final String tenantKey, final String externalSubscriptionId,
                                         final String subjectType, final String subjectKey,
                                         final String planCode, final Long seatCount) {
    final var event = new SubscriptionEvent(
        tenantKey,
        externalSubscriptionId,
        SubscriptionEvent.EventType.SUBSCRIPTION_CREATED,
        Instant.now(),
        subjectType,
        subjectKey,
        planCode,
        seatCount
    );
    publish(RabbitMQConfig.EVENTS_EXCHANGE, RabbitMQConfig.ROUTING_SUBSCRIPTION_CREATED, event);
  }

  /**
   * @deprecated Use {@link #publishSubscriptionCreated(String, String, String, String, String, Long)} instead.
   */
  @Deprecated
  public void publishSubscriptionCreated(final String tenantKey, final String externalSubscriptionId,
                                         final String subjectType, final String subjectKey,
                                         final String planCode) {
    publishSubscriptionCreated(tenantKey, externalSubscriptionId, subjectType, subjectKey, planCode, null);
  }

  /**
   * Publishes a subscription.updated event with explicit subject scope, plan code, and seat count.
   * Published when a subscription's plan, status, or seat count changes.
   *
   * @param seatCount purchased seat count for PER_SEAT plans; {@code null} for FLAT plans
   */
  public void publishSubscriptionUpdated(final String tenantKey, final String externalSubscriptionId,
                                         final String subjectType, final String subjectKey,
                                         final String planCode, final Long seatCount) {
    final var event = new SubscriptionEvent(
        tenantKey,
        externalSubscriptionId,
        SubscriptionEvent.EventType.SUBSCRIPTION_UPDATED,
        Instant.now(),
        subjectType,
        subjectKey,
        planCode,
        seatCount
    );
    publish(RabbitMQConfig.EVENTS_EXCHANGE, RabbitMQConfig.ROUTING_SUBSCRIPTION_UPDATED, event);
  }

  /**
   * @deprecated Use {@link #publishSubscriptionUpdated(String, String, String, String, String, Long)} instead.
   */
  @Deprecated
  public void publishSubscriptionUpdated(final String tenantKey, final String externalSubscriptionId,
                                         final String subjectType, final String subjectKey,
                                         final String planCode) {
    publishSubscriptionUpdated(tenantKey, externalSubscriptionId, subjectType, subjectKey, planCode, null);
  }

  public void publishSubscriptionCancelled(final String tenantKey, final String externalSubscriptionId) {
    final var event = new SubscriptionEvent(
        tenantKey,
        externalSubscriptionId,
        SubscriptionEvent.EventType.SUBSCRIPTION_CANCELLED,
        Instant.now(),
        "TENANT",
        tenantKey,
        null,
        null
    );
    publish(RabbitMQConfig.EVENTS_EXCHANGE, RabbitMQConfig.ROUTING_SUBSCRIPTION_CANCELLED, event);
  }

  /**
   * Publishes a subscription.cancelled event with explicit subject scope.
   * Prefer this overload when the subject type and key are known (e.g. from the Subscription entity).
   */
  public void publishSubscriptionCancelled(final String tenantKey, final String externalSubscriptionId,
                                           final String subjectType, final String subjectKey) {
    final var event = new SubscriptionEvent(
        tenantKey,
        externalSubscriptionId,
        SubscriptionEvent.EventType.SUBSCRIPTION_CANCELLED,
        Instant.now(),
        subjectType,
        subjectKey,
        null,
        null
    );
    publish(RabbitMQConfig.EVENTS_EXCHANGE, RabbitMQConfig.ROUTING_SUBSCRIPTION_CANCELLED, event);
  }

  public void publishInvoicePaid(final String tenantKey, final String externalInvoiceId,
                                 final String externalCustomerId, final String externalSubscriptionId,
                                 final Long amountPaid, final String currency) {
    final var event = new InvoiceEvent(
        tenantKey,
        externalInvoiceId,
        externalCustomerId,
        externalSubscriptionId,
        InvoiceEvent.EventType.INVOICE_PAID,
        amountPaid,
        currency,
        Instant.now(),
        "TENANT",   // default: callers without subject context use tenant scope
        tenantKey
    );
    publish(RabbitMQConfig.EVENTS_EXCHANGE, RabbitMQConfig.ROUTING_INVOICE_PAID, event);
  }

  /**
   * Publishes an invoice.paid event with explicit subject scope.
   */
  public void publishInvoicePaid(final String tenantKey, final String externalInvoiceId,
                                 final String externalCustomerId, final String externalSubscriptionId,
                                 final Long amountPaid, final String currency,
                                 final String subjectType, final String subjectKey) {
    final var event = new InvoiceEvent(
        tenantKey,
        externalInvoiceId,
        externalCustomerId,
        externalSubscriptionId,
        InvoiceEvent.EventType.INVOICE_PAID,
        amountPaid,
        currency,
        Instant.now(),
        subjectType,
        subjectKey
    );
    publish(RabbitMQConfig.EVENTS_EXCHANGE, RabbitMQConfig.ROUTING_INVOICE_PAID, event);
  }

  public void publishInvoiceCreated(final String tenantKey, final String externalInvoiceId,
                                    final String externalCustomerId, final String externalSubscriptionId,
                                    final Long amountDue, final String currency,
                                    final String subjectType, final String subjectKey) {
    final var event = new InvoiceEvent(
        tenantKey,
        externalInvoiceId,
        externalCustomerId,
        externalSubscriptionId,
        InvoiceEvent.EventType.INVOICE_CREATED,
        0L, // amountPaid is 0 for created invoices
        currency,
        Instant.now(),
        subjectType,
        subjectKey
    );
    publish(RabbitMQConfig.EVENTS_EXCHANGE, RabbitMQConfig.ROUTING_INVOICE_CREATED, event);
  }

  public void publishInvoiceFinalized(final String tenantKey, final String externalInvoiceId,
                                      final String externalCustomerId, final String externalSubscriptionId,
                                      final Long amountDue, final String currency,
                                      final String subjectType, final String subjectKey) {
    final var event = new InvoiceEvent(
        tenantKey,
        externalInvoiceId,
        externalCustomerId,
        externalSubscriptionId,
        InvoiceEvent.EventType.INVOICE_FINALIZED,
        0L,
        currency,
        Instant.now(),
        subjectType,
        subjectKey
    );
    publish(RabbitMQConfig.EVENTS_EXCHANGE, RabbitMQConfig.ROUTING_INVOICE_FINALIZED, event);
  }

  public void publishInvoiceUpdated(final String tenantKey, final String externalInvoiceId,
                                    final String externalCustomerId, final String externalSubscriptionId,
                                    final Long amountDue, final String currency,
                                    final String subjectType, final String subjectKey) {
    final var event = new InvoiceEvent(
        tenantKey,
        externalInvoiceId,
        externalCustomerId,
        externalSubscriptionId,
        InvoiceEvent.EventType.INVOICE_UPDATED,
        0L,
        currency,
        Instant.now(),
        subjectType,
        subjectKey
    );
    publish(RabbitMQConfig.EVENTS_EXCHANGE, RabbitMQConfig.ROUTING_INVOICE_UPDATED, event);
  }

  public void publishPaymentFailed(final String tenantKey, final String externalInvoiceId,
                                   final String externalCustomerId, final String externalSubscriptionId,
                                   final Long amountDue, final String currency, final String failureReason) {
    final var event = new PaymentEvent(
        tenantKey,
        externalInvoiceId,
        externalCustomerId,
        externalSubscriptionId,
        PaymentEvent.EventType.PAYMENT_FAILED,
        amountDue,
        currency,
        failureReason,
        Instant.now(),
        "TENANT",   // default: callers without subject context use tenant scope
        tenantKey
    );
    publish(RabbitMQConfig.EVENTS_EXCHANGE, RabbitMQConfig.ROUTING_PAYMENT_FAILED, event);
  }

  /**
   * Publishes a payment.failed event with explicit subject scope.
   */
  public void publishPaymentFailed(final String tenantKey, final String externalInvoiceId,
                                   final String externalCustomerId, final String externalSubscriptionId,
                                   final Long amountDue, final String currency, final String failureReason,
                                   final String subjectType, final String subjectKey) {
    final var event = new PaymentEvent(
        tenantKey,
        externalInvoiceId,
        externalCustomerId,
        externalSubscriptionId,
        PaymentEvent.EventType.PAYMENT_FAILED,
        amountDue,
        currency,
        failureReason,
        Instant.now(),
        subjectType,
        subjectKey
    );
    publish(RabbitMQConfig.EVENTS_EXCHANGE, RabbitMQConfig.ROUTING_PAYMENT_FAILED, event);
  }

  public void publishRefundCreated(final String tenantKey, final String externalRefundId,
                                   final String externalPaymentId, final String externalCustomerId,
                                   final Long amountRefunded, final String currency,
                                   final String status, final String subjectType, final String subjectKey) {
    final var event = new RefundEvent(
        tenantKey,
        externalRefundId,
        externalPaymentId,
        externalCustomerId,
        RefundEvent.EventType.REFUND_CREATED,
        amountRefunded,
        currency,
        status,
        Instant.now(),
        subjectType,
        subjectKey
    );
    publish(RabbitMQConfig.EVENTS_EXCHANGE, RabbitMQConfig.ROUTING_REFUND_CREATED, event);
  }

  public void publishNotification(final NotificationEvent event) {
    publish(RabbitMQConfig.EVENTS_EXCHANGE, RabbitMQConfig.ROUTING_NOTIFICATION_BILLING_EMAIL, event);
  }

  private void publish(final String exchange, final String routingKey, final Object payload) {
    try {
      AuditEventEnricher.enrich(payload);
      rabbitTemplate.convertAndSend(exchange, routingKey, payload);
      log.debug("Published event to exchange={} routingKey={}", exchange, routingKey);
    } catch (final AmqpException e) {
      throw new MessagingException(
          "Failed to publish message to exchange=" + exchange + " routingKey=" + routingKey, e);
    }
  }
}
