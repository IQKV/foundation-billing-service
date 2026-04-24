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

  // Exchange names
  public static final String EVENTS_EXCHANGE = "iqkv.events";
  public static final String DLX_EXCHANGE = "iqkv.dlx";

  // Queue names
  public static final String DLQ = "iqkv.dlq";
  public static final String TENANT_EVENTS_QUEUE = "iqkv.billing.tenant.events";

  // Routing keys
  public static final String ROUTING_TENANT_CREATED = "tenant.created";
  public static final String ROUTING_SUBSCRIPTION_CANCELLED = "subscription.cancelled";

  private static final long TTL_24H_MS = 86_400_000L;

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
  public Binding tenantEventsBinding() {
    return BindingBuilder.bind(tenantEventsQueue()).to(eventsExchange()).with(ROUTING_TENANT_CREATED);
  }

  @Bean
  public Binding dlqBinding() {
    return BindingBuilder.bind(deadLetterQueue()).to(dlxExchange()).with("#");
  }
}
