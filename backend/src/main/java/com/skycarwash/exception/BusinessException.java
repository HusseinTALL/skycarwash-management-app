package com.skycarwash.exception;

import org.springframework.http.HttpStatus;

/**
 * Domain-level business rule violation (insufficient balance, cancellation window expired, etc.).
 * Mapped to HTTP 422 Unprocessable Entity by GlobalExceptionHandler.
 */
public class BusinessException extends RuntimeException {

    private final HttpStatus status;

    public BusinessException(String message) {
        super(message);
        this.status = HttpStatus.UNPROCESSABLE_ENTITY;
    }

    public BusinessException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
