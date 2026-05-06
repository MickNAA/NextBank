package com.example.nexbank.account_service.transfer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransferRepository extends JpaRepository<Transfer, UUID> {

    boolean existsByReferenceId(String referenceId);

    Optional<Transfer> findByReferenceId(String referenceId);
}