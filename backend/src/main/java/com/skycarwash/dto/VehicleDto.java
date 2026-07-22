package com.skycarwash.dto;

import com.skycarwash.entity.Vehicle.VehicleType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record VehicleDto(
        Long id,

        @Size(max = 20) String plate,

        @Size(max = 100) String label,

        @NotNull VehicleType type,

        LocalDateTime createdAt
) {}
