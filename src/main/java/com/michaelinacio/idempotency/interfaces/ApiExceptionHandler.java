package com.michaelinacio.idempotency.interfaces;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("code", safeCode(exception, "INVALID_REQUEST")));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> conflict(IllegalStateException exception) {
        String code = safeCode(exception, "TRANSFER_CONFLICT");
        HttpStatus status = "IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_REQUEST".equals(code)
                ? HttpStatus.CONFLICT : HttpStatus.UNPROCESSABLE_ENTITY;
        return ResponseEntity.status(status).body(Map.of("code", code));
    }

    @ExceptionHandler(EmptyResultDataAccessException.class)
    public ResponseEntity<Map<String, String>> customerNotFound(EmptyResultDataAccessException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("code", "CUSTOMER_IDENTITY_NOT_REGISTERED"));
    }

    private String safeCode(RuntimeException exception, String fallback) {
        String message = exception.getMessage();
        if (message == null || !message.matches("[A-Z0-9_]{1,100}")) {
            return fallback;
        }
        return message;
    }
}
