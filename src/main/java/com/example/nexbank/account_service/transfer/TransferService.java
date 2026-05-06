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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransferService {

    private final TransferRepository transferRepository;
    private final AccountRepository accountRepository;
    private final TransactionHistoryRepository historyRepository;

    /**
     * โอนเงินระหว่างบัญชี — ป้องกัน race condition + deadlock
     *
     * Senior concepts ที่ใช้:
     * 1. Idempotency check (referenceId)
     * 2. Pessimistic locking (SELECT FOR UPDATE)
     * 3. Deadlock prevention (lock ตามลำดับ UUID)
     * 4. ACID transaction (@Transactional)
     * 5. Double-entry bookkeeping (ledger)
     */
    @Transactional
    public TransferResponse transfer(TransferRequest request) {
        log.info("Processing transfer: ref={}, from={}, to={}, amount={}",
                request.referenceId(),
                request.sourceAccountId(),
                request.targetAccountId(),
                request.amount());

        // ===== Step 1: Idempotency Check =====
        if (transferRepository.existsByReferenceId(request.referenceId())) {
            log.warn("Duplicate transfer request: ref={}", request.referenceId());
            Transfer existing = transferRepository.findByReferenceId(request.referenceId())
                    .orElseThrow();
            return TransferResponse.from(existing);
        }

        // ===== Step 2: Validate — ห้ามโอนให้ตัวเอง =====
        if (request.sourceAccountId().equals(request.targetAccountId())) {
            throw new IllegalArgumentException("Cannot transfer to same account");
        }

        // ===== Step 3: Lock accounts ตามลำดับ UUID (ป้องกัน Deadlock) =====
        // ถ้า Thread A โอน A→B, Thread B โอน B→A
        // ถ้า lock ตามลำดับ request: Thread A lock A, Thread B lock B → Deadlock!
        // ถ้า lock ตามลำดับ UUID: ทั้ง 2 thread lock ตัวเดียวกันก่อน → ไม่ Deadlock!
        UUID firstId;
        UUID secondId;

        if (request.sourceAccountId().compareTo(request.targetAccountId()) < 0) {
            firstId = request.sourceAccountId();
            secondId = request.targetAccountId();
        } else {
            firstId = request.targetAccountId();
            secondId = request.sourceAccountId();
        }

        // Lock ตัวที่ UUID น้อยกว่าก่อนเสมอ
        Account first = accountRepository.findByIdWithLock(firstId)
                .orElseThrow(() -> new AccountNotFoundException(firstId.toString()));

        Account second = accountRepository.findByIdWithLock(secondId)
                .orElseThrow(() -> new AccountNotFoundException(secondId.toString()));

        // Map กลับเป็น source / target
        Account source = first.getId().equals(request.sourceAccountId()) ? first : second;
        Account target = first.getId().equals(request.targetAccountId()) ? first : second;

        // ===== Step 4: Business Validation =====
        if (source.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Source account is not active: " + source.getStatus());
        }
        if (target.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Target account is not active: " + target.getStatus());
        }
        if (source.getBalance().compareTo(request.amount()) < 0) {
            throw new IllegalStateException(
                    "Insufficient funds: balance=" + source.getBalance()
                            + ", requested=" + request.amount());
        }

        // ===== Step 5: Execute Transfer (debit + credit) =====
        BigDecimal sourceBalanceBefore = source.getBalance();
        BigDecimal targetBalanceBefore = target.getBalance();

        source.withdraw(request.amount());  // debit
        target.deposit(request.amount());   // credit

        accountRepository.save(source);
        accountRepository.save(target);

        // ===== Step 6: Create Transfer Record =====
        Transfer transfer = Transfer.builder()
                .referenceId(request.referenceId())
                .sourceAccountId(source.getId())
                .targetAccountId(target.getId())
                .amount(request.amount())
                .currency(source.getCurrency())
                .description(request.description())
                .build();

        transfer.markCompleted();
        Transfer saved = transferRepository.save(transfer);

        // ===== Step 7: Record Transaction History (Double-Entry) =====
        // ฝั่งผู้โอน: TRANSFER_OUT
        historyRepository.save(TransactionHistory.builder()
                .accountId(source.getId())
                .transferId(saved.getId())
                .type(TransactionType.TRANSFER_OUT)
                .amount(request.amount().negate())  // ติดลบ (เงินออก)
                .balanceBefore(sourceBalanceBefore)
                .balanceAfter(source.getBalance())
                .description("Transfer to " + target.getAccountNumber()
                        + (request.description() != null ? ": " + request.description() : ""))
                .build());

        // ฝั่งผู้รับ: TRANSFER_IN
        historyRepository.save(TransactionHistory.builder()
                .accountId(target.getId())
                .transferId(saved.getId())
                .type(TransactionType.TRANSFER_IN)
                .amount(request.amount())  // บวก (เงินเข้า)
                .balanceBefore(targetBalanceBefore)
                .balanceAfter(target.getBalance())
                .description("Transfer from " + source.getAccountNumber()
                        + (request.description() != null ? ": " + request.description() : ""))
                .build());

        log.info("Transfer completed: ref={}, id={}, amount={} {}",
                saved.getReferenceId(), saved.getId(),
                saved.getAmount(), saved.getCurrency());

        return TransferResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public TransferResponse getTransfer(UUID id) {
        return transferRepository.findById(id)
                .map(TransferResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("Transfer not found: " + id));
    }

    @Transactional(readOnly = true)
    public TransferResponse getTransferByReference(String referenceId) {
        return transferRepository.findByReferenceId(referenceId)
                .map(TransferResponse::from)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Transfer not found: ref=" + referenceId));
    }
}