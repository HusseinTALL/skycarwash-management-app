package com.skycarwash.dto;

import java.time.LocalDateTime;

/** One line of a client's wash history. */
public record ClientHistoryItemDto(
        Long id,
        String serviceName,
        int amount,
        String paymentMethod,
        LocalDateTime createdAt,
        boolean cancelled
) {}
