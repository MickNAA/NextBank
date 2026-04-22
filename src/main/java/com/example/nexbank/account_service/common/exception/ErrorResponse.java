package com.example.nexbank.account_service.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String details,
        String path
) {
    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(Instant.now(), status, error, message, null, path);
    }

    public static ErrorResponse of(int status, String error, String message, String details, String path) {
        return new ErrorResponse(Instant.now(), status, error, message, details, path);
    }
}