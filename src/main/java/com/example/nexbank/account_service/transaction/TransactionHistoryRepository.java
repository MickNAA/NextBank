package com.example.nexbank.account_service.transaction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TransactionHistoryRepository extends JpaRepository<TransactionHistory, UUID> {

    Page<TransactionHistory> findByAccountIdOrderByCreatedAtDesc(
            UUID accountId, Pageable pageable);
    Page<TransactionHistory> findByAccountIdAndType(
            UUID accountId, TransactionType type, Pageable pageable);
}