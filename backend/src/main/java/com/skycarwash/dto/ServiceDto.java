package com.skycarwash.dto;

import jakarta.validation.constraints.*;

import java.util.Map;

public record ServiceDto(
        Long id,
        @NotBlank @Size(max = 100) String name,
        @Min(0) int price,
        String category,
        Map<String, Double> productConsumption,
        boolean active
) {
    /** Compact constructor for creation (no id) */
    public ServiceDto(String name, int price, String category, Map<String, Double> productConsumption) {
        this(null, name, price, category, productConsumption, true);
    }
}
