package com.work.broker.producer;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.work.broker.model.EventMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Producer for Topic Exchange
 *
 * Sends messages with specific routing keys that determine
 * which queues receive the message based on pattern matching.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TopicExchangeProducer {

    private final AmqpTemplate amqpTemplate;

    @Value("${rabbitmq.topic.exchange}")
    private String topicExchange;

    /**
     * Send message with a specific routing key
     *
     * @param routingKey The routing key (e.g., "order.created", "system.error")
     * @param message    The message to send
     */
    public void sendMessage(String routingKey, EventMessage message) {
        log.info("Sending to Topic Exchange [{}] with routing key [{}]: {}",
                topicExchange, routingKey, message);

        amqpTemplate.convertAndSend(topicExchange, routingKey, message);

        log.info("Message sent successfully with routing key: {}", routingKey);
    }

    public void sendOrderCreated(EventMessage message) {
        sendMessage("order.created", message);
    }

    public void sendOrderUpdated(EventMessage message) {
        sendMessage("order.updated", message);
    }

    public void sendSystemError(EventMessage message) {
        sendMessage("system.error", message);
    }

    public void sendPaymentError(EventMessage message) {
        sendMessage("payment.error", message);
    }

    public void sendUserRegistered(EventMessage message) {
        sendMessage("user.registered", message);
    }
}
