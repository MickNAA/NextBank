package com.example.nexbank.account_service.account.exception;

public class DuplicateAccountException extends RuntimeException {
    public DuplicateAccountException(String field, String value) {
        super("Account with " + field + " '" + value + "' already exists");
    }
}