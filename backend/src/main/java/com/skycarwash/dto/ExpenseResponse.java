package com.skycarwash.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ExpenseResponse(
        Long id,
        String category,
        int amount,
        String description,
        LocalDate expenseDate,
        String createdByName,
        LocalDateTime createdAt
) {}
