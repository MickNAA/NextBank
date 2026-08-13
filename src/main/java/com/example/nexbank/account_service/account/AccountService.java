package com.example.nexbank.account_service.account;

import com.example.nexbank.account_service.account.dto.AccountResponse;
import com.example.nexbank.account_service.account.dto.CreateAccountRequest;
import com.example.nexbank.account_service.account.dto.UpdateAccountRequest;
import com.example.nexbank.account_service.account.exception.AccountNotFoundException;
import com.example.nexbank.account_service.account.exception.DuplicateAccountException;
import com.example.nexbank.account_service.transaction.TransactionHistory;
import com.example.nexbank.account_service.transaction.TransactionHistoryRepository;
import com.example.nexbank.account_service.transaction.TransactionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final AccountRepository repository;
    private final TransactionHistoryRepository historyRepository;

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        log.info("Creating account for: {}", request.email());

        if (repository.existsByEmail(request.email())) {
            throw new DuplicateAccountException("email", request.email());
        }

        String accountNumber = generateAccountNumber();

        Account account = Account.builder()
                .accountNumber(accountNumber)
                .ownerName(request.ownerName())
                .email(request.email())
                .currency(request.currency())
                .balance(request.initialDeposit())
                .status(AccountStatus.ACTIVE)
                .build();

        Account saved = repository.save(account);
        log.info("Account created: {} ({})", saved.getAccountNumber(), saved.getId());

        return AccountResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccount(UUID id) {
        return repository.findById(id)
                .map(AccountResponse::from)
                .orElseThrow(() -> new AccountNotFoundException(id.toString()));
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccountByNumber(String accountNumber) {
        return repository.findByAccountNumber(accountNumber)
                .map(AccountResponse::from)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
    }

    @Transactional(readOnly = true)
    public Page<AccountResponse> listAccounts(AccountStatus status, Pageable pageable) {
        Page<Account> accounts = (status != null)
                ? repository.findByStatus(status, pageable)
                : repository.findAll(pageable);

        return accounts.map(AccountResponse::from);
    }

    @Transactional
    public AccountResponse updateAccount(UUID id, UpdateAccountRequest request) {
        Account account = repository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id.toString()));

        if (request.ownerName() != null) {
            account.setOwnerName(request.ownerName());
        }

        if (request.email() != null && !request.email().equals(account.getEmail())) {
            if (repository.existsByEmail(request.email())) {
                throw new DuplicateAccountException("email", request.email());
            }
            account.setEmail(request.email());
        }

        Account saved = repository.save(account);
        log.info("Account updated: {}", saved.getId());

        return AccountResponse.from(saved);
    }

    @Transactional
    public void closeAccount(UUID id) {
        Account account = repository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id.toString()));

        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new IllegalStateException("Account is already closed");
        }

        if (account.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalStateException(
                    "Cannot close account with remaining balance: " + account.getBalance());
        }

        account.setStatus(AccountStatus.CLOSED);
        repository.save(account);
        log.info("Account closed: {}", id);
    }

    @Transactional
    public AccountResponse deposit(UUID id, BigDecimal amount) {
        Account account = repository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id.toString()));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Cannot deposit to " + account.getStatus() + " account");
        }

        BigDecimal balanceBefore = account.getBalance();
        account.deposit(amount);
        Account saved = repository.save(account);

        historyRepository.save(TransactionHistory.builder()
                .accountId(saved.getId())
                .type(TransactionType.DEPOSIT)
                .amount(amount)
                .balanceBefore(balanceBefore)
                .balanceAfter(saved.getBalance())
                .build());

        log.info("Deposit {} {} to account {}", amount, account.getCurrency(), id);
        return AccountResponse.from(saved);
    }

    @Transactional
    public AccountResponse withdraw(UUID id, BigDecimal amount) {
        Account account = repository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id.toString()));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Cannot withdraw from " + account.getStatus() + " account");
        }

        BigDecimal balanceBefore = account.getBalance();
        account.withdraw(amount);
        Account saved = repository.save(account);

        historyRepository.save(TransactionHistory.builder()
                .accountId(saved.getId())
                .type(TransactionType.WITHDRAWAL)
                .amount(amount)
                .balanceBefore(balanceBefore)
                .balanceAfter(saved.getBalance())
                .build());

        log.info("Withdrawal {} {} from account {}", amount, account.getCurrency(), id);
        return AccountResponse.from(saved);
    }

    private String generateAccountNumber() {
        String accountNumber;
        do {
            long number = ThreadLocalRandom.current().nextLong(1_000_000_000L, 9_999_999_999L);
            accountNumber = String.valueOf(number);
        } while (repository.existsByAccountNumber(accountNumber));

        return accountNumber;
    }
}