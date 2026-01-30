package com.work.broker.service;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.work.broker.entity.ProcessedMessage;
import com.work.broker.repository.ProcessedMessageRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for message deduplication to ensure idempotent processing.
 *
 * Stores processed message IDs in database to detect duplicates.
 * Includes automatic cleanup of old records.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MessageDeduplicationService {

    private final ProcessedMessageRepository repository;

    private static final int RETENTION_DAYS = 7;

    /**
     * Check if message was already processed and mark it as processing.
     * Uses database-level locking to handle concurrent access.
     *
     * @param messageId Unique message identifier
     * @param queueName Queue where the message was received
     * @param messageType Type/category of the message
     * @return true if message is NEW and should be processed, false if DUPLICATE
     */
    @Transactional
    public boolean tryProcess(String messageId, String queueName, String messageType) {
        if (messageId == null || messageId.isBlank()) {
            log.warn("Message ID is null or empty - processing without deduplication");
            return true;
        }

        if (repository.existsByMessageIdAndQueueName(messageId, queueName)) {
            log.info("DUPLICATE detected - messageId: {}, queue: {}", messageId, queueName);
            return false;
        }

        ProcessedMessage processed = ProcessedMessage.builder()
                .messageId(messageId)
                .queueName(queueName)
                .processedAt(LocalDateTime.now())
                .status("PROCESSED")
                .messageType(messageType)
                .build();

        repository.save(processed);
        log.debug("Message marked as processed - messageId: {}, queue: {}", messageId, queueName);

        return true;
    }

    /**
     * Simple check if message was already processed (without marking).
     */
    @Transactional(readOnly = true)
    public boolean isDuplicate(String messageId, String queueName) {
        return repository.existsByMessageIdAndQueueName(messageId, queueName);
    }

    /**
     * Simple check if message was already processed in any queue.
     */
    @Transactional(readOnly = true)
    public boolean isDuplicate(String messageId) {
        return repository.existsByMessageId(messageId);
    }

    /**
     * Mark message as failed (for retry tracking).
     */
    @Transactional
    public void markAsFailed(String messageId, String queueName, String messageType) {
        ProcessedMessage processed = ProcessedMessage.builder()
                .messageId(messageId)
                .queueName(queueName)
                .processedAt(LocalDateTime.now())
                .status("FAILED")
                .messageType(messageType)
                .build();

        repository.save(processed);
    }

    /**
     * Remove a message from processed list (to allow reprocessing).
     */
    @Transactional
    public void allowReprocess(String messageId) {
        repository.deleteById(messageId);
        log.info("Message removed from deduplication - messageId: {}", messageId);
    }

    /**
     * Scheduled cleanup of old processed messages.
     * Runs daily at midnight.
     */
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void cleanupExpiredMessages() {
        LocalDateTime expirationDate = LocalDateTime.now().minusDays(RETENTION_DAYS);
        int deleted = repository.deleteExpiredMessages(expirationDate);
        log.info("Cleaned up {} expired message records (older than {} days)", deleted, RETENTION_DAYS);
    }

    /**
     * Manual cleanup (for testing or maintenance).
     */
    @Transactional
    public int cleanupOlderThan(int days) {
        LocalDateTime expirationDate = LocalDateTime.now().minusDays(days);
        int deleted = repository.deleteExpiredMessages(expirationDate);
        log.info("Manual cleanup: deleted {} records older than {} days", deleted, days);
        return deleted;
    }

    /**
     * Get count of processed messages for a specific queue.
     */
    @Transactional(readOnly = true)
    public long getProcessedCount(String queueName) {
        return repository.countByQueueName(queueName);
    }
}
