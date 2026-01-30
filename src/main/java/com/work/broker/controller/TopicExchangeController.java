package com.work.broker.controller;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.work.broker.model.EventMessage;
import com.work.broker.producer.TopicExchangeProducer;

import lombok.RequiredArgsConstructor;

/**
 * REST Controller for testing Topic Exchange
 *
 * Topic Exchange routes messages based on routing key patterns:
 * - '*' matches exactly one word
 * - '#' matches zero or more words
 *
 * Test endpoints:
 * POST /api/topic/send/{routingKey}  - Send with custom routing key
 * POST /api/topic/order/created      - Matches "order.*" queue
 * POST /api/topic/system/error       - Matches "*.error" queue
 */
@RestController
@RequestMapping("/api/topic")
@RequiredArgsConstructor
public class TopicExchangeController {

    private final TopicExchangeProducer topicProducer;

    /**
     * Send message with custom routing key
     * Example: POST /api/topic/send/order.created
     */
    @PostMapping("/send/{routingKey}")
    public ResponseEntity<String> sendWithRoutingKey(
            @PathVariable String routingKey,
            @RequestBody(required = false) String content) {

        EventMessage message = EventMessage.builder()
                .id(UUID.randomUUID().toString())
                .type(routingKey)
                .content(content != null ? content : "Message for " + routingKey)
                .timestamp(LocalDateTime.now())
                .source("topic-controller")
                .build();

        topicProducer.sendMessage(routingKey, message);

        return ResponseEntity.ok("Message sent with routing key: " + routingKey +
                "Routing behavior:" +
                "- 'order.*' pattern -> ordersQueue" +
                "- '*.error' pattern -> errorsQueue" +
                "- '#' pattern -> allEventsQueue (catches all)");
    }

    /**
     * Send order.created event
     * Will be received by: ordersQueue (order.*) and allEventsQueue (#)
     */
    @PostMapping("/order/created")
    public ResponseEntity<String> sendOrderCreated(@RequestBody(required = false) String content) {
        EventMessage message = EventMessage.builder()
                .id(UUID.randomUUID().toString())
                .type("order.created")
                .content(content != null ? content : "New order has been created")
                .timestamp(LocalDateTime.now())
                .source("order-service")
                .build();

        topicProducer.sendOrderCreated(message);

        return ResponseEntity.ok("""
                Order created event sent!"
                "Routing key: order.created"
                "Received by: ordersQueue (order.*), allEventsQueue (#)
                """);
    }

    /**
     * Send order.updated event
     * Will be received by: ordersQueue (order.*) and allEventsQueue (#)
     */
    @PostMapping("/order/updated")
    public ResponseEntity<String> sendOrderUpdated(@RequestBody(required = false) String content) {
        EventMessage message = EventMessage.builder()
                .id(UUID.randomUUID().toString())
                .type("order.updated")
                .content(content != null ? content : "Order has been updated")
                .timestamp(LocalDateTime.now())
                .source("order-service")
                .build();

        topicProducer.sendOrderUpdated(message);

        return ResponseEntity.ok("""
                Order updated event sent!"
                "Routing key: order.updated"
                "Received by: ordersQueue (order.*), allEventsQueue (#)
                """);
    }

    /**
     * Send system.error event
     * Will be received by: errorsQueue (*.error) and allEventsQueue (#)
     */
    @PostMapping("/system/error")
    public ResponseEntity<String> sendSystemError(@RequestBody(required = false) String content) {
        EventMessage message = EventMessage.builder()
                .id(UUID.randomUUID().toString())
                .type("system.error")
                .content(content != null ? content : "System error occurred!")
                .timestamp(LocalDateTime.now())
                .source("monitoring-service")
                .build();

        topicProducer.sendSystemError(message);

        return ResponseEntity.ok("""
                System error event sent!"
                "Routing key: system.error"
                "Received by: errorsQueue (*.error), allEventsQueue (#)
                """);
    }

    /**
     * Send payment.error event
     * Will be received by: errorsQueue (*.error) and allEventsQueue (#)
     */
    @PostMapping("/payment/error")
    public ResponseEntity<String> sendPaymentError(@RequestBody(required = false) String content) {
        EventMessage message = EventMessage.builder()
                .id(UUID.randomUUID().toString())
                .type("payment.error")
                .content(content != null ? content : "Payment processing failed!")
                .timestamp(LocalDateTime.now())
                .source("payment-service")
                .build();

        topicProducer.sendPaymentError(message);

        return ResponseEntity.ok("""
                Payment error event sent!"
                "Routing key: payment.error"
                "Received by: errorsQueue (*.error), allEventsQueue (#)
                """);
    }

    /**
     * Send user.registered event
     * Will ONLY be received by: allEventsQueue (#)
     * Does NOT match order.* or *.error patterns
     */
    @PostMapping("/user/registered")
    public ResponseEntity<String> sendUserRegistered(@RequestBody(required = false) String content) {
        EventMessage message = EventMessage.builder()
                .id(UUID.randomUUID().toString())
                .type("user.registered")
                .content(content != null ? content : "New user registered")
                .timestamp(LocalDateTime.now())
                .source("user-service")
                .build();

        topicProducer.sendUserRegistered(message);

        return ResponseEntity.ok("""
                User registered event sent!"
                "Routing key: user.registered"
                "Received by: allEventsQueue (#) ONLY"
                "Does NOT match: ordersQueue (order.*), errorsQueue (*.error)
                """);
    }
}
