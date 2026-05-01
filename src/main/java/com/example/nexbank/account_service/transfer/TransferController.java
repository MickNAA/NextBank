package com.example.nexbank.account_service.transfer;

import com.example.nexbank.account_service.transfer.dto.TransferRequest;
import com.example.nexbank.account_service.transfer.dto.TransferResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
@Slf4j
public class TransferController {

    private final TransferService service;

    @PostMapping
    public ResponseEntity<TransferResponse> transfer(@Valid @RequestBody TransferRequest request) {
        log.debug("POST /api/v1/transfers — {} -> {}",
                request.sourceAccountId(), request.destinationAccountId());
        return ResponseEntity.status(HttpStatus.CREATED).body(service.transfer(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransferResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getTransfer(id));
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<Page<TransferResponse>> listByAccount(
            @PathVariable UUID accountId,
            @PageableDefault(size = 20, sort = "createdAt",
                    direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(service.listByAccount(accountId, pageable));
    }
}
