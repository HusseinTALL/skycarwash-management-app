package com.skycarwash.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ProductDto(
        Long id,
        @NotBlank @Size(max = 100) String name,
        @NotNull @DecimalMin("0.0") BigDecimal stock,
        @NotNull @DecimalMin("0.0") BigDecimal alertThreshold,
        @NotBlank String unit,
        boolean active
) {
    public ProductDto(String name, BigDecimal stock, BigDecimal alertThreshold, String unit) {
        this(null, name, stock, alertThreshold, unit, true);
    }
}
