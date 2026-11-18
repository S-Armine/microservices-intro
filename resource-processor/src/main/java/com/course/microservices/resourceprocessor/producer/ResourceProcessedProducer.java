package com.course.microservices.resourceprocessor.producer;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;

@Component
public class ResourceProcessedProducer {

    private final RabbitTemplate rabbitTemplate;
    private final String exchangeName;
    private final String routingKey;

    public ResourceProcessedProducer(RabbitTemplate rabbitTemplate,
                                     @Value("${resource.processed.exchange}") String exchangeName,
                                     @Value("${resource.processed.routing-key}") String routingKey) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchangeName = exchangeName;
        this.routingKey = routingKey;
    }

    @Retryable(
            includes = AmqpException.class,
            maxRetries = 2,
            delay = 1000,
            multiplier = 2
    )
    public void sendMessage(String resourceId) {
        rabbitTemplate.convertAndSend(exchangeName, routingKey, resourceId);
    }
}
