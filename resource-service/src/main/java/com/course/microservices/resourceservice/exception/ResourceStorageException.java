package com.course.microservices.resourceservice.exception;

public class ResourceStorageException extends RuntimeException {
    public ResourceStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
