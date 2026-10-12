package com.course.microservices.songservice.exception;

public class SongAlreadyExistsException extends RuntimeException {
    public SongAlreadyExistsException(String s) {
        super(s);
    }
}
