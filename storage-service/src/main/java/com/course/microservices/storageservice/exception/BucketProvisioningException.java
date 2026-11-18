package com.course.microservices.storageservice.exception;

public class BucketProvisioningException extends RuntimeException {

    public BucketProvisioningException(String message, Throwable cause) {
        super(message, cause);
    }
}
