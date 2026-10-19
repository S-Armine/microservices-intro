package com.course.microservices.resourceservice.config;

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

    @Value("${resource.processing.exchange}")
    private String exchangeName;

    @Value("${resource.processing.routing-key}")
    private String routingKey;

    @Bean
    public Queue resourceProcessingQueue() {
        return new Queue(queueName, true);
    }

    @Bean
    public TopicExchange resourceProcessingExchange() {
        return new TopicExchange(exchangeName);
    }

    @Bean
    public Binding resourceProcessingBinding(Queue resourceProcessingQueue, TopicExchange resourceProcessingExchange) {
        return BindingBuilder.bind(resourceProcessingQueue).to(resourceProcessingExchange).with(routingKey);
    }

}
