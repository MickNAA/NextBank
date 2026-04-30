package com.example.nexbank.account_service.account;

import com.example.nexbank.account_service.account.dto.AccountResponse;
import com.example.nexbank.account_service.account.dto.CreateAccountRequest;
import com.example.nexbank.account_service.account.dto.UpdateAccountRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Slf4j
public class AccountController {

    private final AccountService service;

    // POST create account
    @PostMapping
    public ResponseEntity<AccountResponse> create(
            @Valid @RequestBody CreateAccountRequest request) {
        log.debug("POST /api/v1/accounts — {}", request.email());
        AccountResponse response = service.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // GET get by ID
    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getAccount(id));
    }

    // GET get by account number
    @GetMapping("/number/{accountNumber}")
    public ResponseEntity<AccountResponse> getByNumber(@PathVariable String accountNumber) {
        return ResponseEntity.ok(service.getAccountByNumber(accountNumber));
    }

    // GET list all with pagination
    @GetMapping
    public ResponseEntity<Page<AccountResponse>> list(
            @RequestParam(required = false) AccountStatus status,
            @PageableDefault(size = 20, sort = "createdAt",
                    direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(service.listAccounts(status, pageable));
    }

    // PATCH update account info
    @PatchMapping("/{id}")
    public ResponseEntity<AccountResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAccountRequest request) {
        return ResponseEntity.ok(service.updateAccount(id, request));
    }

    // DELETE close account (soft delete)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> close(@PathVariable UUID id) {
        service.closeAccount(id);
        return ResponseEntity.noContent().build();
    }

    // POST deposit — deposit money
    @PostMapping("/{id}/deposit")
    public ResponseEntity<AccountResponse> deposit(
            @PathVariable UUID id,
            @RequestBody @Valid AmountRequest request) {
        return ResponseEntity.ok(service.deposit(id, request.amount()));
    }

    // POST withdraw — withdraw money
    @PostMapping("/{id}/withdraw")
    public ResponseEntity<AccountResponse> withdraw(
            @PathVariable UUID id,
            @RequestBody @Valid AmountRequest request) {
        return ResponseEntity.ok(service.withdraw(id, request.amount()));
    }

    // Inner record for deposit/withdraw
    public record AmountRequest(
            @NotNull(message = "Amount is required")
            @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
            BigDecimal amount
    ) {}
}