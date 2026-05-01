package com.example.nexbank.account_service.transaction.exception;

public class TransactionNotFoundException extends RuntimeException {
    public TransactionNotFoundException(String identifier) {
        super("Transaction not found: " + identifier);
    }
}
