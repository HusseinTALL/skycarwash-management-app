package com.skycarwash.dto;

import java.util.List;

/** Loyalty balance + recent ledger for a client. */
public record LoyaltyStatusDto(
        int points,
        List<LoyaltyMovementDto> movements
) {}
