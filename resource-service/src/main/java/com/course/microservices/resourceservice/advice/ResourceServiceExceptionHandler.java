package com.course.microservices.resourceservice.advice;

import com.course.microservices.resourceservice.dto.ErrorResponse;
import com.course.microservices.resourceservice.exception.CsvTooLongException;
import com.course.microservices.resourceservice.exception.ResourceNotFoundException;
import com.course.microservices.resourceservice.exception.ResourceStorageException;
import com.course.microservices.resourceservice.exception.SongServiceInvocationException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class ResourceServiceExceptionHandler {

    @ExceptionHandler(CsvTooLongException.class)
    public ResponseEntity<ErrorResponse> handleCsvTooLong(CsvTooLongException e) {
        return error(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException e) {
        return error(HttpStatus.BAD_REQUEST, "Invalid file format: " + e.getContentType() + ". Only MP3 files are allowed");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException e) {
        Method method = e.getParameter().getMethod();
        String message = method != null && method.getName().equals("getResources")
                ? String.format("Invalid value '%s' for %s. Must be a positive integer", e.getValue(), e.getName().toUpperCase())
                : String.format("Invalid ID format: '%s'. Only positive integers are allowed", extractInvalidValue(e.getValue()));
        return error(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        Map<String, String> details = new HashMap<>();
        e.getBindingResult().getFieldErrors().forEach(fe -> details.put(fe.getField(), fe.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ErrorResponse("Validation error", "400", details));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .findFirst()
                .orElseThrow(() -> e)
                .getMessage();
        return error(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException e) {
        return error(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler({SongServiceInvocationException.class, ResourceStorageException.class})
    public ResponseEntity<ErrorResponse> handleServiceInvocation(RuntimeException e) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception e) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }

    private ResponseEntity<ErrorResponse> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(message, String.valueOf(status.value())));
    }

    private String extractInvalidValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String s) {
            for (String part : s.split(",")) {
                String trimmed = part.trim();
                try {
                    if (Long.parseLong(trimmed) < 1) return trimmed;
                } catch (NumberFormatException ex) {
                    return trimmed;
                }
            }
        }
        return value.toString();
    }
}