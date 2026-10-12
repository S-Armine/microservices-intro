package com.course.microservices.resourceservice.exception;

public class SongServiceInvocationException extends RuntimeException {
    public SongServiceInvocationException(String message, Throwable cause) {
        super(message, cause);
    }
}
