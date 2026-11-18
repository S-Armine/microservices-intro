package com.course.microservices.resourceservice.service;

import com.course.microservices.resourceservice.dto.StorageDto;
import com.course.microservices.resourceservice.exception.StorageServiceInvocationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Stub data emulating the storage-service response (same structure as {@link StorageDto}),
 * used while the storage-service is unavailable.
 */
@Component
public class StorageFallback {

    private final Map<String, StorageDto> stubs;

    public StorageFallback(@Value("${storage.fallback.staging.id}") Long stagingId,
                           @Value("${storage.fallback.staging.bucket}") String stagingBucket,
                           @Value("${storage.fallback.staging.path}") String stagingPath,
                           @Value("${storage.fallback.permanent.id}") Long permanentId,
                           @Value("${storage.fallback.permanent.bucket}") String permanentBucket,
                           @Value("${storage.fallback.permanent.path}") String permanentPath) {
        this.stubs = Map.of(
                "STAGING", new StorageDto(stagingId, "STAGING", stagingBucket, stagingPath),
                "PERMANENT", new StorageDto(permanentId, "PERMANENT", permanentBucket, permanentPath));
    }

    public StorageDto get(String storageType) {
        StorageDto stub = stubs.get(storageType);
        if (stub == null) {
            throw new StorageServiceInvocationException("No stub storage for type " + storageType);
        }
        return stub;
    }
}
