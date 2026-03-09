package com.skycarwash.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record ExpenseRequest(
        @NotBlank String category,
        @Min(1)   int amount,
        String description,
        LocalDate expenseDate
) {}
