package com.example.nexbank.account_service.transfer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TransferRepository extends JpaRepository<Transfer, UUID> {

    @Query("SELECT t FROM Transfer t WHERE t.sourceAccountId = :accountId OR t.destinationAccountId = :accountId")
    Page<Transfer> findByAccountId(@Param("accountId") UUID accountId, Pageable pageable);
}
