package com.example.nexbank.account_service.transfer.dto;

import com.example.nexbank.account_service.transfer.Transfer;
import com.example.nexbank.account_service.transfer.TransferStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransferResponse(
        UUID id,
        UUID sourceAccountId,
        UUID destinationAccountId,
        BigDecimal amount,
        String currency,
        TransferStatus status,
        String description,
        Instant createdAt
) {
    public static TransferResponse from(Transfer t) {
        return new TransferResponse(
                t.getId(),
                t.getSourceAccountId(),
                t.getDestinationAccountId(),
                t.getAmount(),
                t.getCurrency(),
                t.getStatus(),
                t.getDescription(),
                t.getCreatedAt()
        );
    }
}
