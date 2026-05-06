package com.example.nexbank.account_service.transfer.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferRequest(

        @NotBlank(message = "Reference ID is required")
        @Size(max = 64, message = "Reference ID must not exceed 64 characters")
        String referenceId,

        @NotNull(message = "Source account ID is required")
        UUID sourceAccountId,

        @NotNull(message = "Target account ID is required")
        UUID targetAccountId,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
        BigDecimal amount,

        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description
) {}