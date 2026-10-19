package com.course.microservices.resourceprocessor.config;

import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${resource.processing.queue}")
    private String queueName;

    @Bean
    public Queue resourceProcessingQueue() {
        return new Queue(queueName, true);
    }
}
