package com.course.microservices.songservice.advice;

import com.course.microservices.songservice.controller.SongController;
import com.course.microservices.songservice.dto.ErrorResponse;
import com.course.microservices.songservice.exception.CsvTooLongException;
import com.course.microservices.songservice.exception.SongAlreadyExistsException;
import com.course.microservices.songservice.exception.SongNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SongServiceExceptionHandlerTest {

    private final SongServiceExceptionHandler underTest = new SongServiceExceptionHandler();

    @Test
    void notFoundMapsTo404() {
        ResponseEntity<ErrorResponse> response = underTest.handleSongNotFound(new SongNotFoundException("missing"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getErrorMessage()).isEqualTo("missing");
        assertThat(response.getBody().getErrorCode()).isEqualTo("404");
    }

    @Test
    void alreadyExistsMapsTo409() {
        ResponseEntity<ErrorResponse> response = underTest.handleSongAlreadyExists(new SongAlreadyExistsException("dup"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void csvTooLongMapsTo400() {
        ResponseEntity<ErrorResponse> response = underTest.handleCsvTooLong(new CsvTooLongException(250));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getErrorMessage()).contains("250");
    }

    @Test
    void genericExceptionMapsTo500() {
        ResponseEntity<ErrorResponse> response = underTest.handleGeneric(new RuntimeException("boom"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getErrorMessage()).isEqualTo("boom");
    }

    @Test
    void validationErrorsAreReturnedPerField() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("songDto", "name", "Song name is required")));

        ResponseEntity<ErrorResponse> response = underTest.handleMethodArgumentNotValid(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getErrorMessage()).isEqualTo("Validation error");
        assertThat(response.getBody().getDetails()).containsEntry("name", "Song name is required");
    }

    @Test
    void constraintViolationReturnsFirstMessage() {
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        when(violation.getMessage()).thenReturn("must be positive");

        ResponseEntity<ErrorResponse> response =
                underTest.handleConstraintViolation(new ConstraintViolationException(Set.of(violation)));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getErrorMessage()).isEqualTo("must be positive");
    }

    @Test
    void constraintViolationWithoutViolationsIsRethrown() {
        ConstraintViolationException ex = new ConstraintViolationException(Set.of());

        assertThatThrownBy(() -> underTest.handleConstraintViolation(ex)).isSameAs(ex);
    }

    @Test
    void typeMismatchOnGetByIdUsesTheShortMessage() throws Exception {
        ResponseEntity<ErrorResponse> response = underTest.handleMethodArgumentTypeMismatch(mismatch("getSongById", "abc"));

        assertThat(response.getBody().getErrorMessage()).isEqualTo("Invalid value 'abc' for ID. Must be a positive integer");
    }

    @Test
    void typeMismatchOnDeleteReportsTheOffendingValue() throws Exception {
        ResponseEntity<ErrorResponse> response = underTest.handleMethodArgumentTypeMismatch(mismatch("deleteSongs", "1,x,3"));

        assertThat(response.getBody().getErrorMessage()).isEqualTo("Invalid ID format: 'x'. Only positive integers are allowed");
    }

    @Test
    void typeMismatchReportsNonPositiveNumbers() throws Exception {
        ResponseEntity<ErrorResponse> response = underTest.handleMethodArgumentTypeMismatch(mismatch("deleteSongs", "4,0"));

        assertThat(response.getBody().getErrorMessage()).contains("'0'");
    }

    @Test
    void typeMismatchFallsBackToTheWholeValueWhenAllPartsAreValid() throws Exception {
        ResponseEntity<ErrorResponse> response = underTest.handleMethodArgumentTypeMismatch(mismatch("deleteSongs", "1,2"));

        assertThat(response.getBody().getErrorMessage()).contains("'1,2'");
    }

    @Test
    void typeMismatchWithNullValue() throws Exception {
        ResponseEntity<ErrorResponse> response = underTest.handleMethodArgumentTypeMismatch(mismatch("deleteSongs", null));

        assertThat(response.getBody().getErrorMessage()).contains("'null'");
    }

    @Test
    void typeMismatchWithNonStringValue() throws Exception {
        ResponseEntity<ErrorResponse> response = underTest.handleMethodArgumentTypeMismatch(mismatch("deleteSongs", 42));

        assertThat(response.getBody().getErrorMessage()).contains("'42'");
    }

    private MethodArgumentTypeMismatchException mismatch(String controllerMethod, Object value) throws Exception {
        Method method = controllerMethod.equals("getSongById")
                ? SongController.class.getMethod(controllerMethod, Long.class)
                : SongController.class.getMethod(controllerMethod, HttpServletRequest.class, List.class);
        return new MethodArgumentTypeMismatchException(value, Long.class, "id", new MethodParameter(method, 0), null);
    }
}
