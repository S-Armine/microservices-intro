package com.course.microservices.storageservice.dto;

import com.course.microservices.storageservice.entity.Storage;
import com.course.microservices.storageservice.entity.StorageType;

public record StorageDto(Long id, StorageType storageType, String bucket, String path) {

    public static StorageDto from(Storage storage) {
        return new StorageDto(storage.getId(), storage.getStorageType(), storage.getBucket(), storage.getPath());
    }
}
