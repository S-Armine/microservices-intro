package com.course.microservices.resourceservice.exception;

public class StorageServiceInvocationException extends RuntimeException {
    public StorageServiceInvocationException(String message, Throwable cause) {
        super(message, cause);
    }

    public StorageServiceInvocationException(String message) {
        super(message);
    }
}
