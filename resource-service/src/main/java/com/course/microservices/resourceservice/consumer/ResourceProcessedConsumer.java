package com.course.microservices.resourceservice.consumer;

import com.course.microservices.resourceservice.service.ResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ResourceProcessedConsumer {

    private final ResourceService resourceService;

    @RabbitListener(queues = "${resource.processed.queue}")
    public void receiveMessage(String resourceId) {
        resourceService.moveToPermanent(Long.valueOf(resourceId));
    }
}
