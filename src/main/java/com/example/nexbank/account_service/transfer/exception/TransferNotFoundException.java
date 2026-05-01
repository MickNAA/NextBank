package com.example.nexbank.account_service.transfer.exception;

public class TransferNotFoundException extends RuntimeException {
    public TransferNotFoundException(String identifier) {
        super("Transfer not found: " + identifier);
    }
}
