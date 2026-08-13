package com.example.nexbank.account_service.account;

import com.example.nexbank.account_service.account.dto.AccountResponse;
import com.example.nexbank.account_service.account.dto.CreateAccountRequest;
import com.example.nexbank.account_service.account.exception.AccountNotFoundException;
import com.example.nexbank.account_service.account.exception.DuplicateAccountException;
import com.example.nexbank.account_service.transaction.TransactionHistory;
import com.example.nexbank.account_service.transaction.TransactionHistoryRepository;
import com.example.nexbank.account_service.transaction.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionHistoryRepository historyRepository;

    private AccountService accountService;

    @BeforeEach
    void setUp() {
        accountService = new AccountService(accountRepository, historyRepository);
    }

    private Account activeAccount(BigDecimal balance) {
        return Account.builder()
                .id(UUID.randomUUID())
                .accountNumber("1234567890")
                .ownerName("Somchai Jaidee")
                .email("somchai@example.com")
                .balance(balance)
                .currency("THB")
                .status(AccountStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void createAccount_savesNewAccount_whenEmailIsUnique() {
        CreateAccountRequest request = new CreateAccountRequest(
                "Somchai Jaidee", "somchai@example.com", "THB", BigDecimal.valueOf(1000));

        when(accountRepository.existsByEmail(request.email())).thenReturn(false);
        when(accountRepository.existsByAccountNumber(anyString())).thenReturn(false);
        when(accountRepository.save(any(Account.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AccountResponse response = accountService.createAccount(request);

        assertThat(response.email()).isEqualTo("somchai@example.com");
        assertThat(response.balance()).isEqualByComparingTo("1000");
        assertThat(response.status()).isEqualTo(AccountStatus.ACTIVE);
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void createAccount_throwsDuplicateAccountException_whenEmailAlreadyExists() {
        CreateAccountRequest request = new CreateAccountRequest(
                "Somchai Jaidee", "somchai@example.com", "THB", BigDecimal.ZERO);

        when(accountRepository.existsByEmail(request.email())).thenReturn(true);

        assertThatThrownBy(() -> accountService.createAccount(request))
                .isInstanceOf(DuplicateAccountException.class)
                .hasMessageContaining("somchai@example.com");

        verify(accountRepository, never()).save(any());
    }

    @Test
    void deposit_increasesBalance_andRecordsTransactionHistory() {
        Account account = activeAccount(BigDecimal.valueOf(500));
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AccountResponse response = accountService.deposit(account.getId(), BigDecimal.valueOf(250));

        assertThat(response.balance()).isEqualByComparingTo("750");

        ArgumentCaptor<TransactionHistory> captor = ArgumentCaptor.forClass(TransactionHistory.class);
        verify(historyRepository).save(captor.capture());
        TransactionHistory history = captor.getValue();
        assertThat(history.getType()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(history.getAmount()).isEqualByComparingTo("250");
        assertThat(history.getBalanceBefore()).isEqualByComparingTo("500");
        assertThat(history.getBalanceAfter()).isEqualByComparingTo("750");
    }

    @Test
    void deposit_throwsAccountNotFoundException_whenAccountMissing() {
        UUID missingId = UUID.randomUUID();
        when(accountRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.deposit(missingId, BigDecimal.TEN))
                .isInstanceOf(AccountNotFoundException.class);

        verifyNoInteractions(historyRepository);
    }

    @Test
    void withdraw_decreasesBalance_andRecordsTransactionHistory() {
        Account account = activeAccount(BigDecimal.valueOf(500));
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AccountResponse response = accountService.withdraw(account.getId(), BigDecimal.valueOf(200));

        assertThat(response.balance()).isEqualByComparingTo("300");

        ArgumentCaptor<TransactionHistory> captor = ArgumentCaptor.forClass(TransactionHistory.class);
        verify(historyRepository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(TransactionType.WITHDRAWAL);
    }

    @Test
    void withdraw_throwsIllegalStateException_whenInsufficientFunds() {
        Account account = activeAccount(BigDecimal.valueOf(100));
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountService.withdraw(account.getId(), BigDecimal.valueOf(500)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient funds");

        verify(accountRepository, never()).save(any());
        verifyNoInteractions(historyRepository);
    }

    @Test
    void withdraw_throwsIllegalStateException_whenAccountNotActive() {
        Account account = activeAccount(BigDecimal.valueOf(500));
        account.setStatus(AccountStatus.SUSPENDED);
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountService.withdraw(account.getId(), BigDecimal.TEN))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SUSPENDED");
    }

    @Test
    void closeAccount_throwsIllegalStateException_whenBalanceRemains() {
        Account account = activeAccount(BigDecimal.valueOf(50));
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountService.closeAccount(account.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("remaining balance");

        verify(accountRepository, never()).save(any());
    }

    @Test
    void closeAccount_succeeds_whenBalanceIsZero() {
        Account account = activeAccount(BigDecimal.ZERO);
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        accountService.closeAccount(account.getId());

        assertThat(account.getStatus()).isEqualTo(AccountStatus.CLOSED);
        verify(accountRepository).save(account);
    }
}
