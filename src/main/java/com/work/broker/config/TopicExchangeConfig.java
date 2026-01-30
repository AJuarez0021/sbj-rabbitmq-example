package com.work.broker.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topic Exchange Configuration
 *
 * Topic Exchange routes messages based on routing key patterns:
 * - '*' matches exactly one word
 * - '#' matches zero or more words
 *
 * Example routing keys:
 * - order.created -> matches "order.*" and "order.#"
 * - order.payment.completed -> matches "order.#" but NOT "order.*"
 * - system.error -> matches "*.error" and "#.error"
 */
@Configuration
public class TopicExchangeConfig {

    @Value("${rabbitmq.topic.exchange}")
    private String topicExchange;

    @Value("${rabbitmq.topic.queue.orders}")
    private String ordersQueue;

    @Value("${rabbitmq.topic.queue.errors}")
    private String errorsQueue;

    @Value("${rabbitmq.topic.queue.all}")
    private String allQueue;

    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange(topicExchange);
    }

    @Bean
    public Queue ordersQueue() {
        return new Queue(ordersQueue, true);
    }

    @Bean
    public Queue errorsQueue() {
        return new Queue(errorsQueue, true);
    }

    @Bean
    public Queue allEventsQueue() {
        return new Queue(allQueue, true);
    }


    /**
     * Binding: ordersQueue <- "order.*"
     * Matches: order.created, order.updated, order.deleted
     * Does NOT match: order.payment.completed (more than one word after "order.")
     */
    @Bean
    public Binding ordersBinding(Queue ordersQueue, TopicExchange topicExchange) {
        return BindingBuilder.bind(ordersQueue).to(topicExchange).with("order.*");
    }

    /**
     * Binding: errorsQueue <- "*.error"
     * Matches: system.error, payment.error, order.error
     * Does NOT match: system.critical.error (more than one word before ".error")
     */
    @Bean
    public Binding errorsBinding(Queue errorsQueue, TopicExchange topicExchange) {
        return BindingBuilder.bind(errorsQueue).to(topicExchange).with("*.error");
    }

    /**
     * Binding: allEventsQueue <- "#"
     * Matches ALL routing keys (catch-all pattern)
     */
    @Bean
    public Binding allEventsBinding(Queue allEventsQueue, TopicExchange topicExchange) {
        return BindingBuilder.bind(allEventsQueue).to(topicExchange).with("#");
    }
}
