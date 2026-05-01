package com.example.nexbank.account_service.transfer;

import com.example.nexbank.account_service.account.Account;
import com.example.nexbank.account_service.account.AccountRepository;
import com.example.nexbank.account_service.account.AccountStatus;
import com.example.nexbank.account_service.account.exception.AccountNotFoundException;
import com.example.nexbank.account_service.transaction.Transaction;
import com.example.nexbank.account_service.transaction.TransactionRepository;
import com.example.nexbank.account_service.transaction.TransactionType;
import com.example.nexbank.account_service.transfer.dto.TransferRequest;
import com.example.nexbank.account_service.transfer.dto.TransferResponse;
import com.example.nexbank.account_service.transfer.exception.TransferException;
import com.example.nexbank.account_service.transfer.exception.TransferNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final TransactionRepository transactionRepository;

    @Transactional
    public TransferResponse transfer(TransferRequest request) {
        if (request.sourceAccountId().equals(request.destinationAccountId())) {
            throw new TransferException("Source and destination accounts must be different");
        }

        Account source = accountRepository.findById(request.sourceAccountId())
                .orElseThrow(() -> new AccountNotFoundException(request.sourceAccountId().toString()));
        Account destination = accountRepository.findById(request.destinationAccountId())
                .orElseThrow(() -> new AccountNotFoundException(request.destinationAccountId().toString()));

        if (source.getStatus() != AccountStatus.ACTIVE) {
            throw new TransferException("Source account is not active");
        }
        if (destination.getStatus() != AccountStatus.ACTIVE) {
            throw new TransferException("Destination account is not active");
        }
        if (!source.getCurrency().equals(destination.getCurrency())) {
            throw new TransferException("Currency mismatch: source is " + source.getCurrency()
                    + ", destination is " + destination.getCurrency());
        }

        BigDecimal sourceBalanceBefore = source.getBalance();
        BigDecimal destBalanceBefore = destination.getBalance();

        source.withdraw(request.amount());
        destination.deposit(request.amount());

        accountRepository.save(source);
        accountRepository.save(destination);

        Transfer transfer = transferRepository.save(Transfer.builder()
                .sourceAccountId(source.getId())
                .destinationAccountId(destination.getId())
                .amount(request.amount())
                .currency(source.getCurrency())
                .status(TransferStatus.COMPLETED)
                .description(request.description())
                .build());

        transactionRepository.save(Transaction.builder()
                .accountId(source.getId())
                .type(TransactionType.TRANSFER_OUT)
                .amount(request.amount())
                .balanceBefore(sourceBalanceBefore)
                .balanceAfter(source.getBalance())
                .currency(source.getCurrency())
                .referenceId(transfer.getId())
                .description(request.description())
                .build());

        transactionRepository.save(Transaction.builder()
                .accountId(destination.getId())
                .type(TransactionType.TRANSFER_IN)
                .amount(request.amount())
                .balanceBefore(destBalanceBefore)
                .balanceAfter(destination.getBalance())
                .currency(destination.getCurrency())
                .referenceId(transfer.getId())
                .description(request.description())
                .build());

        log.info("Transfer {} {} from {} to {}",
                request.amount(), source.getCurrency(), source.getId(), destination.getId());

        return TransferResponse.from(transfer);
    }

    @Transactional(readOnly = true)
    public TransferResponse getTransfer(UUID id) {
        return transferRepository.findById(id)
                .map(TransferResponse::from)
                .orElseThrow(() -> new TransferNotFoundException(id.toString()));
    }

    @Transactional(readOnly = true)
    public Page<TransferResponse> listByAccount(UUID accountId, Pageable pageable) {
        return transferRepository.findByAccountId(accountId, pageable)
                .map(TransferResponse::from);
    }
}
