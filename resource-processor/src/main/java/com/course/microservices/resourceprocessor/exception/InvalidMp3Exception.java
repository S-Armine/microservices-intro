package com.course.microservices.resourceprocessor.exception;

public class InvalidMp3Exception extends RuntimeException {
    public InvalidMp3Exception(String message) {
        super(message);
    }

    public InvalidMp3Exception(String message, Throwable cause) {
        super(message, cause);
    }
}
