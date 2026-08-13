package com.example.nexbank.account_service.transfer;

import com.example.nexbank.account_service.account.Account;
import com.example.nexbank.account_service.account.AccountRepository;
import com.example.nexbank.account_service.account.AccountStatus;
import com.example.nexbank.account_service.account.exception.AccountNotFoundException;
import com.example.nexbank.account_service.transaction.TransactionHistory;
import com.example.nexbank.account_service.transaction.TransactionHistoryRepository;
import com.example.nexbank.account_service.transaction.TransactionType;
import com.example.nexbank.account_service.transfer.dto.TransferRequest;
import com.example.nexbank.account_service.transfer.dto.TransferResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionHistoryRepository historyRepository;

    private TransferService transferService;

    @BeforeEach
    void setUp() {
        transferService = new TransferService(transferRepository, accountRepository, historyRepository);
    }

    private Account activeAccount(BigDecimal balance) {
        return Account.builder()
                .id(UUID.randomUUID())
                .accountNumber(String.valueOf((long) (Math.random() * 1_000_000_000L) + 1_000_000_000L))
                .ownerName("Test Owner")
                .email(UUID.randomUUID() + "@example.com")
                .balance(balance)
                .currency("THB")
                .status(AccountStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void transfer_movesMoneyBetweenAccounts_andWritesDoubleEntryHistory() {
        Account source = activeAccount(BigDecimal.valueOf(1000));
        Account target = activeAccount(BigDecimal.valueOf(200));

        TransferRequest request = new TransferRequest(
                "ref-001", source.getId(), target.getId(), BigDecimal.valueOf(300), "rent");

        when(transferRepository.existsByReferenceId("ref-001")).thenReturn(false);
        when(accountRepository.findByIdWithLock(source.getId())).thenReturn(Optional.of(source));
        when(accountRepository.findByIdWithLock(target.getId())).thenReturn(Optional.of(target));
        when(transferRepository.save(any(Transfer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TransferResponse response = transferService.transfer(request);

        assertThat(response.status()).isEqualTo(TransferStatus.COMPLETED);
        assertThat(source.getBalance()).isEqualByComparingTo("700");
        assertThat(target.getBalance()).isEqualByComparingTo("500");

        ArgumentCaptor<TransactionHistory> captor = ArgumentCaptor.forClass(TransactionHistory.class);
        verify(historyRepository, times(2)).save(captor.capture());
        List<TransactionHistory> entries = captor.getAllValues();

        TransactionHistory outEntry = entries.stream()
                .filter(e -> e.getType() == TransactionType.TRANSFER_OUT)
                .findFirst().orElseThrow();
        TransactionHistory inEntry = entries.stream()
                .filter(e -> e.getType() == TransactionType.TRANSFER_IN)
                .findFirst().orElseThrow();

        assertThat(outEntry.getAccountId()).isEqualTo(source.getId());
        assertThat(outEntry.getAmount()).isEqualByComparingTo("-300");
        assertThat(inEntry.getAccountId()).isEqualTo(target.getId());
        assertThat(inEntry.getAmount()).isEqualByComparingTo("300");
    }

    @Test
    void transfer_isIdempotent_returnsExistingTransfer_onDuplicateReference() {
        Account source = activeAccount(BigDecimal.valueOf(1000));
        Account target = activeAccount(BigDecimal.valueOf(200));

        TransferRequest request = new TransferRequest(
                "dup-ref", source.getId(), target.getId(), BigDecimal.valueOf(300), null);

        Transfer existing = Transfer.builder()
                .id(UUID.randomUUID())
                .referenceId("dup-ref")
                .sourceAccountId(source.getId())
                .targetAccountId(target.getId())
                .amount(BigDecimal.valueOf(300))
                .currency("THB")
                .status(TransferStatus.COMPLETED)
                .createdAt(Instant.now())
                .build();

        when(transferRepository.existsByReferenceId("dup-ref")).thenReturn(true);
        when(transferRepository.findByReferenceId("dup-ref")).thenReturn(Optional.of(existing));

        TransferResponse response = transferService.transfer(request);

        assertThat(response.id()).isEqualTo(existing.getId());
        verify(accountRepository, never()).findByIdWithLock(any());
        verifyNoInteractions(historyRepository);
    }

    @Test
    void transfer_throwsIllegalArgumentException_whenSourceAndTargetAreSame() {
        UUID accountId = UUID.randomUUID();
        TransferRequest request = new TransferRequest(
                "ref-002", accountId, accountId, BigDecimal.TEN, null);

        when(transferRepository.existsByReferenceId("ref-002")).thenReturn(false);

        assertThatThrownBy(() -> transferService.transfer(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("same account");

        verifyNoInteractions(accountRepository);
    }

    @Test
    void transfer_throwsIllegalStateException_whenSourceHasInsufficientFunds() {
        Account source = activeAccount(BigDecimal.valueOf(50));
        Account target = activeAccount(BigDecimal.valueOf(200));

        TransferRequest request = new TransferRequest(
                "ref-003", source.getId(), target.getId(), BigDecimal.valueOf(300), null);

        when(transferRepository.existsByReferenceId("ref-003")).thenReturn(false);
        when(accountRepository.findByIdWithLock(source.getId())).thenReturn(Optional.of(source));
        when(accountRepository.findByIdWithLock(target.getId())).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> transferService.transfer(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient funds");

        verify(transferRepository, never()).save(any());
        verifyNoInteractions(historyRepository);
    }

    @Test
    void transfer_throwsIllegalStateException_whenTargetAccountNotActive() {
        Account source = activeAccount(BigDecimal.valueOf(1000));
        Account target = activeAccount(BigDecimal.valueOf(200));
        target.setStatus(AccountStatus.CLOSED);

        TransferRequest request = new TransferRequest(
                "ref-004", source.getId(), target.getId(), BigDecimal.valueOf(100), null);

        when(transferRepository.existsByReferenceId("ref-004")).thenReturn(false);
        when(accountRepository.findByIdWithLock(source.getId())).thenReturn(Optional.of(source));
        when(accountRepository.findByIdWithLock(target.getId())).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> transferService.transfer(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Target account is not active");
    }

    @Test
    void transfer_throwsAccountNotFoundException_whenSourceAccountMissing() {
        UUID sourceId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        TransferRequest request = new TransferRequest(
                "ref-005", sourceId, targetId, BigDecimal.TEN, null);

        when(transferRepository.existsByReferenceId("ref-005")).thenReturn(false);
        when(accountRepository.findByIdWithLock(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transferService.transfer(request))
                .isInstanceOf(AccountNotFoundException.class);
    }
}
