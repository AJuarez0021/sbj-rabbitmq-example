package com.work.broker.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.work.broker.entity.ProcessedMessage;

@Repository
public interface ProcessedMessageRepository extends JpaRepository<ProcessedMessage, String> {

    boolean existsByMessageId(String messageId);

    boolean existsByMessageIdAndQueueName(String messageId, String queueName);

    List<ProcessedMessage> findByQueueName(String queueName);

    @Modifying
    @Query("DELETE FROM ProcessedMessage p WHERE p.processedAt < :expirationDate")
    int deleteExpiredMessages(@Param("expirationDate") LocalDateTime expirationDate);

    long countByQueueName(String queueName);
}
