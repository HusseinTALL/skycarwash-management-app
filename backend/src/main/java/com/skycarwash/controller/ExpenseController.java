package com.skycarwash.controller;

import com.skycarwash.dto.ExpenseRequest;
import com.skycarwash.dto.ExpenseResponse;
import com.skycarwash.service.ExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @GetMapping
    public ResponseEntity<List<ExpenseResponse>> list(@RequestParam(required = false) String month) {
        YearMonth ym;
        try {
            ym = month != null ? YearMonth.parse(month) : YearMonth.now();
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(expenseService.findByMonth(ym));
    }

    @PostMapping
    public ResponseEntity<ExpenseResponse> create(
            @Valid @RequestBody ExpenseRequest req,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(expenseService.create(req, principal.getUsername()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        expenseService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
