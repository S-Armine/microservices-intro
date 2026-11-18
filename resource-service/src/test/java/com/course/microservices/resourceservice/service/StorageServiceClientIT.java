package com.course.microservices.resourceservice.service;

import com.course.microservices.resourceservice.dto.StorageDto;
import com.course.microservices.resourceservice.helper.StorageServiceMock;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import software.amazon.awssdk.services.s3.S3Client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class StorageServiceClientIT {

    private static final String BODY = """
            [{"id": 7, "storageType": "STAGING", "bucket": "real-staging", "path": "in"},
             {"id": 8, "storageType": "PERMANENT", "bucket": "real-permanent", "path": "out"}]""";

    @Autowired
    private StorageServiceClient client;
    @Autowired
    private CircuitBreakerRegistry registry;

    @MockitoBean
    private S3Client s3Client;
    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @DynamicPropertySource
    static void storageService(DynamicPropertyRegistry registry) {
        StorageServiceMock.registerProperties(registry);
    }

    @BeforeEach
    void reset() {
        StorageServiceMock.SERVER.resetAll();
        registry.getAllCircuitBreakers().forEach(cb -> cb.reset());
    }

    @Test
    void returnsTheRealStorageWhenTheServiceIsUp() {
        StorageServiceMock.SERVER.stubFor(get(urlEqualTo("/storages"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(BODY)));

        assertThat(client.getStorageByType("PERMANENT"))
                .isEqualTo(new StorageDto(8L, "PERMANENT", "real-permanent", "out"));
    }

    @Test
    void returnsStubDataWhenTheServiceFails() {
        StorageServiceMock.SERVER.stubFor(get(urlEqualTo("/storages")).willReturn(aResponse().withStatus(500)));

        assertThat(client.getStorageByType("STAGING"))
                .isEqualTo(new StorageDto(1L, "STAGING", "staging-bucket", "resources"));
        assertThat(client.getStorageByType("PERMANENT"))
                .isEqualTo(new StorageDto(2L, "PERMANENT", "permanent-bucket", "resources"));
    }

    @Test
    void opensTheCircuitAndStopsCallingTheServiceAfterRepeatedFailures() {
        StorageServiceMock.SERVER.stubFor(get(urlEqualTo("/storages")).willReturn(aResponse().withStatus(500)));

        for (int i = 0; i < 10; i++) {
            client.getStorageByType("STAGING");
        }

        assertThat(registry.circuitBreaker("storage-service").getState().name()).isEqualTo("OPEN");
        int calls = StorageServiceMock.SERVER.getAllServeEvents().size();
        client.getStorageByType("STAGING");
        assertThat(StorageServiceMock.SERVER.getAllServeEvents()).hasSize(calls);
    }
}
