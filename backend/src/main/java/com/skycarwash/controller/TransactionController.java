package com.skycarwash.controller;

import com.skycarwash.dto.CancelRequest;
import com.skycarwash.dto.TransactionRequest;
import com.skycarwash.dto.TransactionResponse;
import com.skycarwash.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<TransactionResponse> create(
            @Valid @RequestBody TransactionRequest req,
            @AuthenticationPrincipal UserDetails principal) {

        TransactionResponse response = transactionService.create(req, principal.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<TransactionResponse> cancel(
            @PathVariable Long id,
            @Valid @RequestBody CancelRequest req,
            @AuthenticationPrincipal UserDetails principal) {

        return ResponseEntity.ok(transactionService.cancel(id, req, principal.getUsername()));
    }

    @GetMapping("/today")
    public ResponseEntity<List<TransactionResponse>> today() {
        return ResponseEntity.ok(transactionService.findToday());
    }

    @GetMapping("/history")
    public ResponseEntity<Page<TransactionResponse>> history(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String method,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(transactionService.findHistory(from, to, method, page, size));
    }
}
