package com.course.microservices.songservice.exception;

public class CsvTooLongException extends RuntimeException {
    public static final int MAX_CSV_LENGTH = 200;

    public CsvTooLongException(int length) {
        super(String.format("CSV string is too long: received %d characters, maximum allowed is %d", length, MAX_CSV_LENGTH));
    }
}
