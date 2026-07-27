/*
 * Copyright 2026 iQKV Foundation Team.
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

import com.iqkv.foundation.billingservice.infrastructure.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumes billing notification events from {@code iqkv.billing.notifications} and
 * dispatches them to {@link EmailService} for delivery.
 *
 * <p>Exceptions are swallowed here — failed messages are routed to the DLQ automatically
 * via the {@code x-dead-letter-exchange} queue argument, so rethrowing would cause
 * infinite redelivery before the TTL expires.
 */
@Component
public class BillingEmailConsumer {

  private static final Logger log = LoggerFactory.getLogger(BillingEmailConsumer.class);

  private final EmailService emailService;

  public BillingEmailConsumer(final EmailService emailService) {
    this.emailService = emailService;
  }

  @RabbitListener(queues = RabbitMQConfig.NOTIFICATIONS_QUEUE)
  public void handleNotification(final NotificationEvent event) {
    try {
      emailService.send(event);
    } catch (final Exception e) {
      // Do NOT rethrow — failed messages route to DLQ via x-dead-letter-exchange
      log.error("Failed to process billing notification: type={} recipient={}",
          event.getType(), event.getRecipientEmail(), e);
    }
  }
}
