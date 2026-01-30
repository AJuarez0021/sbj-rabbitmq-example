package com.work.broker.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.work.broker.repository.ProcessedMessageRepository;
import com.work.broker.service.MessageDeduplicationService;

import lombok.RequiredArgsConstructor;

/**
 * REST Controller for monitoring and managing message deduplication.
 */
@RestController
@RequestMapping("/api/deduplication")
@RequiredArgsConstructor
public class DeduplicationController {

    private final MessageDeduplicationService deduplicationService;
    private final ProcessedMessageRepository repository;

    /**
     * Get statistics about processed messages
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalProcessedMessages", repository.count());
        stats.put("topicOrdersQueue", deduplicationService.getProcessedCount("topic.queue.orders"));
        stats.put("topicErrorsQueue", deduplicationService.getProcessedCount("topic.queue.errors"));
        stats.put("topicAllQueue", deduplicationService.getProcessedCount("topic.queue.all"));
        stats.put("fanoutNotification1", deduplicationService.getProcessedCount("fanout.queue.notification1"));
        stats.put("fanoutNotification2", deduplicationService.getProcessedCount("fanout.queue.notification2"));
        stats.put("fanoutNotification3", deduplicationService.getProcessedCount("fanout.queue.notification3"));

        return ResponseEntity.ok(stats);
    }

    /**
     * Get all processed messages
     */
    @GetMapping("/messages")
    public ResponseEntity<?> getAllProcessedMessages() {
        return ResponseEntity.ok(repository.findAll());
    }

    /**
     * Get processed messages for a specific queue
     */
    @GetMapping("/messages/{queueName}")
    public ResponseEntity<?> getMessagesByQueue(@PathVariable String queueName) {
        return ResponseEntity.ok(repository.findByQueueName(queueName));
    }

    /**
     * Check if a specific message ID was processed
     */
    @GetMapping("/check/{messageId}")
    public ResponseEntity<Map<String, Object>> checkMessage(@PathVariable String messageId) {
        Map<String, Object> result = new HashMap<>();
        result.put("messageId", messageId);
        result.put("isDuplicate", deduplicationService.isDuplicate(messageId));
        result.put("details", repository.findById(messageId).orElse(null));
        return ResponseEntity.ok(result);
    }

    /**
     * Allow reprocessing of a specific message
     */
    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<String> allowReprocess(@PathVariable String messageId) {
        deduplicationService.allowReprocess(messageId);
        return ResponseEntity.ok("Message " + messageId + " can now be reprocessed");
    }

    /**
     * Manual cleanup of old messages
     */
    @DeleteMapping("/cleanup")
    public ResponseEntity<Map<String, Object>> cleanup(@RequestParam(defaultValue = "7") int days) {
        int deleted = deduplicationService.cleanupOlderThan(days);
        Map<String, Object> result = new HashMap<>();
        result.put("deletedRecords", deleted);
        result.put("olderThanDays", days);
        return ResponseEntity.ok(result);
    }
}
