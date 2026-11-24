package com.course.microservices.storageservice.advice;

import com.course.microservices.storageservice.dto.ErrorResponse;
import com.course.microservices.storageservice.exception.BucketProvisioningException;
import com.course.microservices.storageservice.exception.CsvTooLongException;
import com.course.microservices.storageservice.exception.StorageAlreadyExistsException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class StorageServiceExceptionHandler {

    @ExceptionHandler(CsvTooLongException.class)
    public ResponseEntity<ErrorResponse> handleCsvTooLong(CsvTooLongException e) {
        return error(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException e) {
        return error(HttpStatus.BAD_REQUEST,
                String.format("Invalid ID format: '%s'. Only positive integers are allowed", e.getValue()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException e) {
        return error(HttpStatus.BAD_REQUEST, "Malformed request body. Allowed storageType values: STAGING, PERMANENT");
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

    @ExceptionHandler(StorageAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleAlreadyExists(StorageAlreadyExistsException e) {
        return error(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(BucketProvisioningException.class)
    public ResponseEntity<ErrorResponse> handleBucketProvisioning(BucketProvisioningException e) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception e) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }

    private ResponseEntity<ErrorResponse> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(message, String.valueOf(status.value())));
    }
}
