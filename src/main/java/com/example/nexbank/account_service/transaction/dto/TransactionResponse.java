package com.example.nexbank.account_service.transaction.dto;

import com.example.nexbank.account_service.transaction.Transaction;
import com.example.nexbank.account_service.transaction.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        UUID accountId,
        TransactionType type,
        BigDecimal amount,
        BigDecimal balanceBefore,
        BigDecimal balanceAfter,
        String currency,
        UUID referenceId,
        String description,
        Instant createdAt
) {
    public static TransactionResponse from(Transaction t) {
        return new TransactionResponse(
                t.getId(),
                t.getAccountId(),
                t.getType(),
                t.getAmount(),
                t.getBalanceBefore(),
                t.getBalanceAfter(),
                t.getCurrency(),
                t.getReferenceId(),
                t.getDescription(),
                t.getCreatedAt()
        );
    }
}
