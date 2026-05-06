package com.example.nexbank.account_service.transaction;

import com.example.nexbank.account_service.transaction.dto.TransactionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionHistoryRepository repository;

    @GetMapping("/{accountId}/transactions")
    public ResponseEntity<Page<TransactionResponse>> getTransactions(
            @PathVariable UUID accountId,
            @PageableDefault(size = 20, sort = "createdAt",
                    direction = Sort.Direction.DESC) Pageable pageable) {
        Page<TransactionResponse> transactions = repository
                .findByAccountIdOrderByCreatedAtDesc(accountId, pageable)
                .map(TransactionResponse::from);

        return ResponseEntity.ok(transactions);
    }
}