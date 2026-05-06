package com.example.nexbank.account_service.transfer.dto;

import com.example.nexbank.account_service.transfer.Transfer;
import com.example.nexbank.account_service.transfer.TransferStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransferResponse(
        UUID id,
        String referenceId,
        UUID sourceAccountId,
        UUID targetAccountId,
        BigDecimal amount,
        String currency,
        TransferStatus status,
        String description,
        Instant createdAt,
        Instant completedAt
) {
    public static TransferResponse from(Transfer entity) {
        return new TransferResponse(
                entity.getId(),
                entity.getReferenceId(),
                entity.getSourceAccountId(),
                entity.getTargetAccountId(),
                entity.getAmount(),
                entity.getCurrency(),
                entity.getStatus(),
                entity.getDescription(),
                entity.getCreatedAt(),
                entity.getCompletedAt()
        );
    }
}