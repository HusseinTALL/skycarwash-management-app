package com.skycarwash.dto;

import com.skycarwash.entity.LoyaltyMovement.MovementType;

import java.time.LocalDateTime;

/** One movement of the loyalty ledger (signed point delta). */
public record LoyaltyMovementDto(
        Long id,
        int points,
        MovementType type,
        String note,
        LocalDateTime createdAt
) {}
