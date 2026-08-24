package com.securefilesharing.exception;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex) {

        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error ->
                        error.getField() + ": " +
                        error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        return build(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(
            NoResourceFoundException ex) {

        return build(
                HttpStatus.NOT_FOUND,
                "Endpoint not found");
    }
    
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(
            UnauthorizedException ex) {

        return build(
                HttpStatus.UNAUTHORIZED,
                ex.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(
            ForbiddenException ex) {

        return build(
                HttpStatus.FORBIDDEN,
                ex.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntime(
            RuntimeException ex) {

        return build(
                HttpStatus.BAD_REQUEST,
                ex.getMessage() == null
                        ? "Request failed"
                        : ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleOther(
            Exception ex) {

        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error");
    }

    private ResponseEntity<ErrorResponse> build(
            HttpStatus status,
            String message) {

        return ResponseEntity
                .status(status)
                .body(new ErrorResponse(
                        status.value(),
                        message,
                        LocalDateTime.now()));
    }
}
