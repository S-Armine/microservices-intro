package com.course.microservices.storageservice.dto;

import com.course.microservices.storageservice.entity.StorageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StorageRequest(
        @NotNull(message = "Storage type is required. Allowed values: STAGING, PERMANENT")
        StorageType storageType,

        @NotBlank(message = "Bucket is required")
        String bucket,

        @NotBlank(message = "Path is required")
        @Size(max = 1024, message = "Path must be at most 1024 characters")
        String path) {
}
