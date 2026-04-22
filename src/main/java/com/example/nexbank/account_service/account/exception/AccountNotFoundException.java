package com.example.nexbank.account_service.account.exception;

public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(String identifier) {
        super("Account not found: " + identifier);
    }
}