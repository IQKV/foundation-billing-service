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

package com.iqkv.foundation.billingservice.infrastructure.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "iqkv.messaging.rabbitmq.enabled", havingValue = "true")
public class RabbitMQConfig {

  // -------------------------------------------------------------------------
  // Shared exchange (all services publish here)
  // -------------------------------------------------------------------------
  public static final String EVENTS_EXCHANGE = "iqkv.events";
  public static final String DLX_EXCHANGE    = "iqkv.dlx";
  public static final String DLQ             = "iqkv.dlq";

  // -------------------------------------------------------------------------
  // Billing queue names
  // -------------------------------------------------------------------------
  public static final String TENANT_EVENTS_QUEUE  = "iqkv.billing.tenant.events";
  public static final String USER_EVENTS_QUEUE    = "iqkv.billing.user.events";
  public static final String NOTIFICATIONS_QUEUE  = "iqkv.billing.notifications";

  // -------------------------------------------------------------------------
  // Routing keys — domain events
  // -------------------------------------------------------------------------
  public static final String ROUTING_TENANT_CREATED             = "tenant.created";
  public static final String ROUTING_TENANT_PROVISIONED         = "tenant.provisioned";
  public static final String ROUTING_TENANT_PROVISIONING_FAILED = "tenant.provisioning_failed";
  public static final String ROUTING_TENANT_UPDATED             = "tenant.updated";
  public static final String ROUTING_TENANT_DELETED             = "tenant.deleted";
  public static final String ROUTING_TENANT_SUSPENDED           = "tenant.suspended";
  public static final String ROUTING_USER_REMOVED               = "user.removed";
  public static final String ROUTING_USER_DELETED               = "user.deleted";
  public static final String ROUTING_SUBSCRIPTION_CANCELLED     = "subscription.cancelled";

  // -------------------------------------------------------------------------
  // Routing keys — billing notification emails (scoped to avoid conflicts)
  // billing.email.# wildcard: adding new email types needs no config change
  // -------------------------------------------------------------------------
  public static final String ROUTING_NOTIFICATION_BILLING_EMAIL          = "notification.billing.email";
  public static final String ROUTING_NOTIFICATION_BILLING_EMAIL_PATTERN  = "notification.billing.#";

  private static final long TTL_24H_MS = 86_400_000L;

  // -------------------------------------------------------------------------
  // Beans
  // -------------------------------------------------------------------------

  @Bean
  public TopicExchange eventsExchange() {
    return new TopicExchange(EVENTS_EXCHANGE, true, false);
  }

  @Bean
  public TopicExchange dlxExchange() {
    return new TopicExchange(DLX_EXCHANGE, true, false);
  }

  @Bean
  public Queue deadLetterQueue() {
    return QueueBuilder.durable(DLQ).build();
  }

  @Bean
  public Queue tenantEventsQueue() {
    return QueueBuilder.durable(TENANT_EVENTS_QUEUE)
        .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
        .withArgument("x-message-ttl", TTL_24H_MS)
        .build();
  }

  @Bean
  public Queue userEventsQueue() {
    return QueueBuilder.durable(USER_EVENTS_QUEUE)
        .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
        .withArgument("x-message-ttl", TTL_24H_MS)
        .build();
  }

  @Bean
  public Queue notificationsQueue() {
    return QueueBuilder.durable(NOTIFICATIONS_QUEUE)
        .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
        .withArgument("x-message-ttl", TTL_24H_MS)
        .build();
  }

  @Bean
  public Binding tenantEventsBinding() {
    // Wildcard — all tenant.* lifecycle events route here so billing has full visibility
    return BindingBuilder.bind(tenantEventsQueue()).to(eventsExchange()).with("tenant.#");
  }

  @Bean
  public Binding userRemovedBinding() {
    return BindingBuilder.bind(userEventsQueue()).to(eventsExchange()).with(ROUTING_USER_REMOVED);
  }

  @Bean
  public Binding userDeletedBinding() {
    return BindingBuilder.bind(userEventsQueue()).to(eventsExchange()).with(ROUTING_USER_DELETED);
  }

  @Bean
  public Binding notificationsBinding() {
    // Wildcard — all notification.billing.* keys route here
    return BindingBuilder.bind(notificationsQueue()).to(eventsExchange())
        .with(ROUTING_NOTIFICATION_BILLING_EMAIL_PATTERN);
  }

  @Bean
  public Binding dlqBinding() {
    return BindingBuilder.bind(deadLetterQueue()).to(dlxExchange()).with("#");
  }
}
