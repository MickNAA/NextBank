package com.example.nexbank.account_service.account.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateAccountRequest(

        @Size(min = 2, max = 255, message = "Owner name must be 2-255 characters")
        String ownerName,

        @Email(message = "Email must be valid")
        String email
) {}