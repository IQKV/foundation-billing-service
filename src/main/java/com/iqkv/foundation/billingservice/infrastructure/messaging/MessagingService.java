package com.iqkv.foundation.billingservice.infrastructure.messaging;

import java.time.Instant;

import com.iqkv.foundation.billingservice.infrastructure.config.RabbitMQConfig;
import com.iqkv.foundation.billingservice.shared.exception.MessagingException;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class MessagingService {

  private final RabbitTemplate rabbitTemplate;

  public MessagingService(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }

  public void publishSubscriptionCancelled(String tenantKey, String externalSubscriptionId) {
    final var event = new SubscriptionEvent(
        tenantKey,
        externalSubscriptionId,
        SubscriptionEvent.EventType.SUBSCRIPTION_CANCELLED,
        Instant.now()
    );
    try {
      rabbitTemplate.convertAndSend(
          RabbitMQConfig.EVENTS_EXCHANGE,
          RabbitMQConfig.ROUTING_SUBSCRIPTION_CANCELLED,
          event
      );
    } catch (AmqpException e) {
      throw new MessagingException(
          "Failed to publish subscription.cancelled event for tenantKey=" + tenantKey, e);
    }
  }
}
