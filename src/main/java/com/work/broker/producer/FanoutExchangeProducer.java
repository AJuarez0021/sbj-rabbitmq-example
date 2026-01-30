package com.work.broker.producer;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.work.broker.model.EventMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Producer for Fanout Exchange
 *
 * Broadcasts messages to ALL bound queues.
 * The routing key is ignored - every queue receives the message.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FanoutExchangeProducer {

    private final AmqpTemplate amqpTemplate;

    @Value("${rabbitmq.fanout.exchange}")
    private String fanoutExchange;

    /**
     * Broadcast message to all queues bound to the fanout exchange
     *
     * @param message The message to broadcast
     */
    public void broadcastMessage(EventMessage message) {
        log.info("Broadcasting to Fanout Exchange [{}]: {}", fanoutExchange, message);

        amqpTemplate.convertAndSend(fanoutExchange, "", message);

        log.info("Message broadcasted to all subscribers");
    }
}
