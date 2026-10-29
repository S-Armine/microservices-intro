package com.course.microservices.resourceprocessor.consumer;

import com.course.microservices.resourceprocessor.service.ResourceProcessingService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ResourceConsumerTest {

    @Test
    void handsTheMessageToTheProcessingService() {
        ResourceProcessingService processing = mock(ResourceProcessingService.class);

        new ResourceConsumer(processing).receiveMessage("8");

        verify(processing).processResource("8");
    }
}
