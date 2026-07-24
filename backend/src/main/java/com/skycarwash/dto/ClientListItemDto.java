package com.skycarwash.dto;

import com.skycarwash.entity.Client.ClientType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Client row enriched with aggregate stats — powers the segmentable list view. */
public record ClientListItemDto(
        Long id,
        String name,
        String phone,
        ClientType type,
        int balance,
        LocalDate expiresAt,
        List<String> tags,
        boolean active,
        LocalDateTime createdAt,
        long totalSpent,
        long visitCount,
        LocalDateTime lastVisitAt,
        long vehicleCount,
        int loyaltyPoints,
        ClientSegment segment
) {}
