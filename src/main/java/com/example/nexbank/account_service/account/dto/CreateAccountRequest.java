package com.example.nexbank.account_service.account.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreateAccountRequest(

        @NotBlank(message = "Owner name is required")
        @Size(min = 2, max = 255, message = "Owner name must be 2-255 characters")
        String ownerName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @NotBlank(message = "Currency is required")
        @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be 3 uppercase letters (ISO 4217)")
        String currency,

        @DecimalMin(value = "0.0", inclusive = true, message = "Initial deposit must be zero or positive")
        BigDecimal initialDeposit
) {
    public CreateAccountRequest {
        if (initialDeposit == null) {
            initialDeposit = BigDecimal.ZERO;
        }
    }
}