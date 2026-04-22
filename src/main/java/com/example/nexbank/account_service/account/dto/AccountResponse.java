package com.example.nexbank.account_service.account.dto;

import com.example.nexbank.account_service.account.Account;
import com.example.nexbank.account_service.account.AccountStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        String accountNumber,
        String ownerName,
        String email,
        BigDecimal balance,
        String currency,
        AccountStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static AccountResponse from(Account entity) {
        return new AccountResponse(
                entity.getId(),
                entity.getAccountNumber(),
                entity.getOwnerName(),
                entity.getEmail(),
                entity.getBalance(),
                entity.getCurrency(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}