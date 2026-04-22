package com.example.nexbank.account_service.account;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {

    Optional<Account> findByAccountNumber(String accountNumber);

    Optional<Account> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByAccountNumber(String accountNumber);

    Page<Account> findByStatus(AccountStatus status, Pageable pageable);
}