package com.course.microservices.resourceservice.producer;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ResourceProducerTest {

    @Test
    void publishesTheIdToTheConfiguredExchangeAndRoutingKey() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        ResourceProducer producer = new ResourceProducer(rabbitTemplate, "the-exchange", "the-key");

        producer.sendMessage("42");

        verify(rabbitTemplate).convertAndSend("the-exchange", "the-key", "42");
    }
}
