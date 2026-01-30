package com.work.broker.consumer;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import com.work.broker.model.EventMessage;
import com.work.broker.service.MessageDeduplicationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Consumer for Fanout Exchange with Idempotent Processing.
 *
 * Each subscriber (queue) independently tracks processed messages.
 * The SAME message ID will be processed once per queue.
 *
 * This is correct behavior for fanout:
 * - Message "ABC" arrives at all 3 queues
 * - Each queue processes "ABC" exactly ONCE
 * - Retries/duplicates within each queue are prevented
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FanoutExchangeConsumer {

    private final MessageDeduplicationService deduplicationService;

    private static final String NOTIFICATION_QUEUE_1 = "fanout.queue.notification1";
    private static final String NOTIFICATION_QUEUE_2 = "fanout.queue.notification2";
    private static final String NOTIFICATION_QUEUE_3 = "fanout.queue.notification3";

    /**
     * Subscriber 1 - Email Notification Service
     */
    @RabbitListener(queues = "${rabbitmq.fanout.queue.notification1}")
    public void emailNotificationHandler(EventMessage message) {
        log.info("=== EMAIL NOTIFICATION SERVICE ===");

        if (!deduplicationService.tryProcess(message.getId(), NOTIFICATION_QUEUE_1, message.getType())) {
            log.warn("DUPLICATE email notification ignored: {}", message.getId());
            return;
        }

        try {
            log.info("Sending email notification for: {}", message);
            sendEmail(message);
            log.info("Email sent successfully for message: {}", message.getId());

        } catch (Exception e) {
            log.error("Failed to send email: {}", e.getMessage());
            deduplicationService.allowReprocess(message.getId());
            throw e;
        }
    }

    /**
     * Subscriber 2 - SMS Notification Service
     */
    @RabbitListener(queues = "${rabbitmq.fanout.queue.notification2}")
    public void smsNotificationHandler(EventMessage message) {
        log.info("=== SMS NOTIFICATION SERVICE ===");

        if (!deduplicationService.tryProcess(message.getId(), NOTIFICATION_QUEUE_2, message.getType())) {
            log.warn("DUPLICATE SMS notification ignored: {}", message.getId());
            return;
        }

        try {
            log.info("Sending SMS notification for: {}", message);
            sendSms(message);
            log.info("SMS sent successfully for message: {}", message.getId());

        } catch (Exception e) {
            log.error("Failed to send SMS: {}", e.getMessage());
            deduplicationService.allowReprocess(message.getId());
            throw e;
        }
    }

    /**
     * Subscriber 3 - Push Notification Service
     */
    @RabbitListener(queues = "${rabbitmq.fanout.queue.notification3}")
    public void pushNotificationHandler(EventMessage message) {
        log.info("=== PUSH NOTIFICATION SERVICE ===");


        if (!deduplicationService.tryProcess(message.getId(), NOTIFICATION_QUEUE_3, message.getType())) {
            log.warn("DUPLICATE push notification ignored: {}", message.getId());
            return;
        }

        try {
            log.info("Sending push notification for: {}", message);
            sendPushNotification(message);
            log.info("Push notification sent successfully for message: {}", message.getId());

        } catch (Exception e) {
            log.error("Failed to send push notification: {}", e.getMessage());
            deduplicationService.allowReprocess(message.getId());
            throw e;
        }
    }


    private void sendEmail(EventMessage message) {
        log.info("EMAIL -> To: users@example.com | Subject: {} | Body: {}",
                message.getType(), message.getContent());
    }

    private void sendSms(EventMessage message) {
        log.info("SMS -> To: +1234567890 | Message: {}", message.getContent());
    }

    private void sendPushNotification(EventMessage message) {
        log.info("PUSH -> Title: {} | Body: {}", message.getType(), message.getContent());
    }
}
