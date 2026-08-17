package com.course.microservices.resourceservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidMp3Exception extends RuntimeException {
    public InvalidMp3Exception(String message) {
        super(message);
    }

    public InvalidMp3Exception(String message, Throwable cause) {
        super(message, cause);
    }
}
