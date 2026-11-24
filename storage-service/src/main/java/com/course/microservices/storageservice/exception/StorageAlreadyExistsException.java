package com.course.microservices.storageservice.exception;

public class StorageAlreadyExistsException extends RuntimeException {

    public StorageAlreadyExistsException(String bucket, String path) {
        super("Storage with bucket='" + bucket + "' and path='" + path + "' already exists");
    }
}
