package com.course.microservices.resourceservice.contract;

import com.course.microservices.resourceservice.advice.ResourceServiceExceptionHandler;
import com.course.microservices.resourceservice.controller.ResourceController;
import com.course.microservices.resourceservice.exception.ResourceNotFoundException;
import com.course.microservices.resourceservice.service.ResourceService;
import org.junit.jupiter.api.BeforeEach;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.standaloneSetup;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class ContractVerifierBase {

    @BeforeEach
    void setUp() {
        ResourceService resourceService = mock(ResourceService.class);
        when(resourceService.getResourceById(1L))
                .thenReturn("ID3-fake-mp3-bytes".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        when(resourceService.getResourceById(404L))
                .thenThrow(new ResourceNotFoundException("Resource with ID=404 not found"));

        standaloneSetup(new ResourceController(resourceService), new ResourceServiceExceptionHandler());
    }
}
