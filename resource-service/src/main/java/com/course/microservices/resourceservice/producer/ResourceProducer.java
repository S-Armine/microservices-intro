package com.course.microservices.resourceservice.producer;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;

@Component
public class ResourceProducer {

    private final RabbitTemplate rabbitTemplate;
    private final String exchangeName;
    private final String routingKey;

    public ResourceProducer(RabbitTemplate rabbitTemplate,
                            @Value("${resource.processing.exchange}") String exchangeName,
                            @Value("${resource.processing.routing-key}") String routingKey) {
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
