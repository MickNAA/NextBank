package com.example.nexbank.account_service.transfer;

import com.example.nexbank.account_service.transfer.dto.TransferRequest;
import com.example.nexbank.account_service.transfer.dto.TransferResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    public ResponseEntity<TransferResponse> transfer(
            @Valid @RequestBody TransferRequest request) {
        log.debug("POST /api/v1/transfers — ref={}", request.referenceId());
        TransferResponse response = service.transfer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransferResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getTransfer(id));
    }

    @GetMapping("/reference/{referenceId}")
    public ResponseEntity<TransferResponse> getByReference(
            @PathVariable String referenceId) {
        return ResponseEntity.ok(service.getTransferByReference(referenceId));
    }
}