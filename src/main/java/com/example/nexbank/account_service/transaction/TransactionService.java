package com.example.nexbank.account_service.transaction;

import com.example.nexbank.account_service.transaction.dto.TransactionResponse;
import com.example.nexbank.account_service.transaction.exception.TransactionNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionHistoryRepository repository;

    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(UUID id) {
        return repository.findById(id)
                .map(TransactionResponse::from)
                .orElseThrow(() -> new TransactionNotFoundException(id.toString()));
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> listByAccount(UUID accountId, TransactionType type, Pageable pageable) {
        Page<TransactionHistory> page = (type != null)
                ? repository.findByAccountIdAndType(accountId, type, pageable)
                : repository.findByAccountIdOrderByCreatedAtDesc(accountId, pageable);
        return page.map(TransactionResponse::from);
    }
}
