package com.course.microservices.resourceservice.service;

import com.course.microservices.resourceservice.dto.StorageDto;
import com.course.microservices.resourceservice.exception.StorageServiceInvocationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Slf4j
@Service
public class StorageServiceClient {

    private final RestClient storageServiceRestClient;
    private final CircuitBreaker circuitBreaker;
    private final StorageFallback storageFallback;

    public StorageServiceClient(RestClient storageServiceRestClient,
                                CircuitBreakerFactory<?, ?> circuitBreakerFactory,
                                StorageFallback storageFallback) {
        this.storageServiceRestClient = storageServiceRestClient;
        this.circuitBreaker = circuitBreakerFactory.create("storage-service");
        this.storageFallback = storageFallback;
    }

    public StorageDto getStorageByType(String storageType) {
        return circuitBreaker.run(() -> fetch(storageType), ex -> {
            log.warn("Storage service unavailable ({}), using stub {} storage", ex.toString(), storageType);
            return storageFallback.get(storageType);
        });
    }

    private StorageDto fetch(String storageType) {
        List<StorageDto> storages = storageServiceRestClient.get()
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        return storages == null ? null : storages.stream()
                .filter(s -> storageType.equals(s.storageType()))
                .findFirst()
                .orElseThrow(() -> new StorageServiceInvocationException("No " + storageType + " storage is configured"));
    }
}
