package com.work.broker.controller;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.work.broker.model.EventMessage;
import com.work.broker.producer.FanoutExchangeProducer;

import lombok.RequiredArgsConstructor;

/**
 * REST Controller for testing Fanout Exchange
 *
 * Fanout Exchange broadcasts messages to ALL bound queues.
 * The routing key is completely IGNORED.
 *
 * All 3 notification services will receive every message:
 * - Email Notification Service (queue 1)
 * - SMS Notification Service (queue 2)
 * - Push Notification Service (queue 3)
 */
@RestController
@RequestMapping("/api/fanout")
@RequiredArgsConstructor
public class FanoutExchangeController {

    private final FanoutExchangeProducer fanoutProducer;

    /**
     * Broadcast a notification to ALL subscribers
     * All 3 queues will receive this message simultaneously
     */
    @PostMapping("/broadcast")
    public ResponseEntity<String> broadcastNotification(@RequestBody(required = false) String content) {
        EventMessage message = EventMessage.builder()
                .id(UUID.randomUUID().toString())
                .type("broadcast")
                .content(content != null ? content : "Important system announcement!")
                .timestamp(LocalDateTime.now())
                .source("broadcast-service")
                .build();

        fanoutProducer.broadcastMessage(message);

        return ResponseEntity.ok("""
                Broadcast sent to ALL subscribers!"
                "Receivers:"
                "- Email Notification Service (queue 1)"
                "- SMS Notification Service (queue 2)"
                "- Push Notification Service (queue 3)"
                """);
    }

    /**
     * Broadcast a system alert
     */
    @PostMapping("/alert")
    public ResponseEntity<String> broadcastAlert(@RequestBody(required = false) String content) {
        EventMessage message = EventMessage.builder()
                .id(UUID.randomUUID().toString())
                .type("system-alert")
                .content(content != null ? content : "ALERT: System maintenance scheduled")
                .timestamp(LocalDateTime.now())
                .source("alert-service")
                .build();

        fanoutProducer.broadcastMessage(message);

        return ResponseEntity.ok("System alert broadcasted to ALL notification services!");
    }

    /**
     * Broadcast a promotional message
     */
    @PostMapping("/promo")
    public ResponseEntity<String> broadcastPromo(@RequestBody(required = false) String content) {
        EventMessage message = EventMessage.builder()
                .id(UUID.randomUUID().toString())
                .type("promotional")
                .content(content != null ? content : "Special offer: 50% off today only!")
                .timestamp(LocalDateTime.now())
                .source("marketing-service")
                .build();

        fanoutProducer.broadcastMessage(message);

        return ResponseEntity.ok("Promotional message broadcasted via Email, SMS, and Push!");
    }
}
