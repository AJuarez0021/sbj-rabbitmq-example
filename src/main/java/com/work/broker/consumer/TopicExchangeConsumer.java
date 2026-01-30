package com.work.broker.consumer;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import com.work.broker.model.EventMessage;
import com.work.broker.service.MessageDeduplicationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Consumer for Topic Exchange with Idempotent Processing.
 *
 * Uses MessageDeduplicationService to prevent duplicate message processing.
 * Each queue tracks its own processed messages independently.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TopicExchangeConsumer {

    private final MessageDeduplicationService deduplicationService;

    private static final String ORDERS_QUEUE = "topic.queue.orders";
    private static final String ERRORS_QUEUE = "topic.queue.errors";
    private static final String ALL_QUEUE = "topic.queue.all";

    /**
     * Listens to orders queue - receives "order.*" messages
     * Examples: order.created, order.updated, order.deleted
     */
    @RabbitListener(queues = "${rabbitmq.topic.queue.orders}")
    public void handleOrderEvents(EventMessage message) {
        log.info("=== ORDERS QUEUE ===");

        if (!deduplicationService.tryProcess(message.getId(), ORDERS_QUEUE, message.getType())) {
            log.warn("DUPLICATE order event ignored: {}", message.getId());
            return;
        }

        try {
            log.info("Processing order event: {}", message);
            log.info("Order details: type={}, content={}",
                    message.getType(), message.getContent());

            processOrder(message);

        } catch (Exception e) {
            log.error("Error processing order event: {}", e.getMessage());
            deduplicationService.allowReprocess(message.getId());
            throw e;
        }
    }

    /**
     * Listens to errors queue - receives "*.error" messages
     * Examples: system.error, payment.error, order.error
     */
    @RabbitListener(queues = "${rabbitmq.topic.queue.errors}")
    public void handleErrorEvents(EventMessage message) {
        log.info("=== ERRORS QUEUE ===");

        if (!deduplicationService.tryProcess(message.getId(), ERRORS_QUEUE, message.getType())) {
            log.warn("DUPLICATE error event ignored: {}", message.getId());
            return;
        }

        try {
            log.info("Processing error event: {}", message);
            log.warn("ERROR ALERT: type={}, content={}, source={}",
                    message.getType(), message.getContent(), message.getSource());


            handleError(message);

        } catch (Exception e) {
            log.error("Error handling error event: {}", e.getMessage());
            deduplicationService.allowReprocess(message.getId());
            throw e;
        }
    }

    /**
     * Listens to all events queue - receives ALL messages ("#" catch-all)
     */
    @RabbitListener(queues = "${rabbitmq.topic.queue.all}")
    public void handleAllEvents(EventMessage message) {
        log.info("=== ALL EVENTS QUEUE ===");


        if (!deduplicationService.tryProcess(message.getId(), ALL_QUEUE, message.getType())) {
            log.warn("DUPLICATE event ignored in all-events queue: {}", message.getId());
            return;
        }

        try {
            log.info("Received event (catch-all): {}", message);


            auditEvent(message);

        } catch (Exception e) {
            log.error("Error in all-events handler: {}", e.getMessage());
            deduplicationService.allowReprocess(message.getId());
            throw e;
        }
    }


    private void processOrder(EventMessage message) {
        log.info("Processing order business logic for: {}", message.getType());
    }

    private void handleError(EventMessage message) {
        log.info("Handling error alert for: {}", message.getSource());
    }

    private void auditEvent(EventMessage message) {
        log.info("Auditing event: {} from {}", message.getType(), message.getSource());
    }
}
