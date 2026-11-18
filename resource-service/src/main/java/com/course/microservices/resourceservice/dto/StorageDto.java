package com.course.microservices.resourceservice.dto;

public record StorageDto(Long id, String storageType, String bucket, String path) {
}
