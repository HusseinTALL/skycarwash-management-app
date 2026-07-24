package com.skycarwash.dto;

import com.skycarwash.entity.ClientInteraction.InteractionType;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** One entry of the client interaction journal. */
public record InteractionDto(
        Long id,
        InteractionType type,
        String notes,
        LocalDate followUpAt,
        boolean followUpDone,
        String userName,
        LocalDateTime createdAt
) {}
