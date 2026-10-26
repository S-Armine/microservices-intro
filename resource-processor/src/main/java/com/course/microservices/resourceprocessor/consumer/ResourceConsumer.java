package com.course.microservices.resourceprocessor.consumer;

import com.course.microservices.resourceprocessor.service.ResourceProcessingService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ResourceConsumer {

    private final ResourceProcessingService resourceProcessingService;

    @RabbitListener(queues = "${resource.processing.queue}")
    public void receiveMessage(String resourceId) {
        resourceProcessingService.processResource(resourceId);
    }
}
