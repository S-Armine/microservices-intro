package com.course.microservices.songservice.exception;

public class CsvTooLongException extends RuntimeException {
    public CsvTooLongException(int length) {
        super(String.format("CSV string is too long: received %d characters, maximum allowed is 200", length));
    }
}
