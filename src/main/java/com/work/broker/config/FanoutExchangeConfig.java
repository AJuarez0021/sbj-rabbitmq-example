package com.work.broker.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Fanout Exchange Configuration
 *
 * Fanout Exchange broadcasts messages to ALL bound queues.
 * The routing key is IGNORED - all queues receive the message.
 *
 * Use cases:
 * - Broadcasting notifications to multiple services
 * - Sending updates to all subscribers
 * - Event distribution where all consumers need the same data
 */
@Configuration
public class FanoutExchangeConfig {

    @Value("${rabbitmq.fanout.exchange}")
    private String fanoutExchange;

    @Value("${rabbitmq.fanout.queue.notification1}")
    private String notificationQueue1;

    @Value("${rabbitmq.fanout.queue.notification2}")
    private String notificationQueue2;

    @Value("${rabbitmq.fanout.queue.notification3}")
    private String notificationQueue3;


    @Bean
    public FanoutExchange fanoutExchange() {
        return new FanoutExchange(fanoutExchange);
    }

    @Bean
    public Queue notificationQueue1() {
        return new Queue(notificationQueue1, true);
    }

    @Bean
    public Queue notificationQueue2() {
        return new Queue(notificationQueue2, true);
    }

    @Bean
    public Queue notificationQueue3() {
        return new Queue(notificationQueue3, true);
    }

    @Bean
    public Binding fanoutBinding1(Queue notificationQueue1, FanoutExchange fanoutExchange) {
        return BindingBuilder.bind(notificationQueue1).to(fanoutExchange);
    }

    @Bean
    public Binding fanoutBinding2(Queue notificationQueue2, FanoutExchange fanoutExchange) {
        return BindingBuilder.bind(notificationQueue2).to(fanoutExchange);
    }

    @Bean
    public Binding fanoutBinding3(Queue notificationQueue3, FanoutExchange fanoutExchange) {
        return BindingBuilder.bind(notificationQueue3).to(fanoutExchange);
    }
}
