package com.example.nexbank.account_service.transaction.dto;

import com.example.nexbank.account_service.transaction.TransactionHistory;
import com.example.nexbank.account_service.transaction.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        UUID accountId,
        UUID transferId,
        TransactionType type,
        BigDecimal amount,
        BigDecimal balanceBefore,
        BigDecimal balanceAfter,
        String description,
        Instant createdAt
) {
    public static TransactionResponse from(TransactionHistory entity) {
        return new TransactionResponse(
                entity.getId(),
                entity.getAccountId(),
                entity.getTransferId(),
                entity.getType(),
                entity.getAmount(),
                entity.getBalanceBefore(),
                entity.getBalanceAfter(),
                entity.getDescription(),
                entity.getCreatedAt()
        );
    }
}