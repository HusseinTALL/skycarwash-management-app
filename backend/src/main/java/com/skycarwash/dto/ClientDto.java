package com.skycarwash.dto;

import com.skycarwash.entity.Client.ClientType;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ClientDto(
        Long id,

        @NotBlank @Size(max = 100) String name,

        @NotBlank @Size(max = 20) String phone,

        @NotNull ClientType type,

        /** Remaining passage count (CARTE / VIP). Ignored for BOUCLIER — auto-set to 0. */
        @Min(0) int balance,

        /** Subscription expiry date; required for BOUCLIER, optional for others. */
        LocalDate expiresAt,

        boolean active,

        LocalDateTime createdAt
) {
    /** Compact constructor for creation requests (no id / createdAt). */
    public ClientDto(String name, String phone, ClientType type, int balance, LocalDate expiresAt) {
        this(null, name, phone, type, balance, expiresAt, true, null);
    }
}
