package com.course.microservices.resourceprocessor.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${resource.processing.queue}")
    private String queueName;

    @Value("${resource.processed.queue}")
    private String processedQueueName;

    @Value("${resource.processed.exchange}")
    private String processedExchangeName;

    @Value("${resource.processed.routing-key}")
    private String processedRoutingKey;

    @Bean
    public Queue resourceProcessingQueue() {
        return new Queue(queueName, true);
    }

    @Bean
    public Queue resourceProcessedQueue() {
        return new Queue(processedQueueName, true);
    }

    @Bean
    public TopicExchange resourceProcessedExchange() {
        return new TopicExchange(processedExchangeName);
    }

    @Bean
    public Binding resourceProcessedBinding(Queue resourceProcessedQueue, TopicExchange resourceProcessedExchange) {
        return BindingBuilder.bind(resourceProcessedQueue).to(resourceProcessedExchange).with(processedRoutingKey);
    }
}
