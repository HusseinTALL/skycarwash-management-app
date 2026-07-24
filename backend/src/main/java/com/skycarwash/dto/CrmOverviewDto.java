package com.skycarwash.dto;

import com.skycarwash.entity.Client.ClientType;
import com.skycarwash.entity.ClientInteraction.InteractionType;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** CRM cockpit: segment counts, follow-ups due, expiring subscriptions, top clients. */
public record CrmOverviewDto(
        Map<String, Long> segmentCounts,
        List<FollowUpDto> followUpsDue,
        List<ExpiringClientDto> expiringSoon,
        List<TopClientDto> topClients
) {
    /** Pending follow-up ("relance") due today or overdue. */
    public record FollowUpDto(
            Long interactionId,
            Long clientId,
            String clientName,
            String clientPhone,
            InteractionType type,
            String notes,
            LocalDate followUpAt
    ) {}

    /** Active subscription expiring within the next 7 days (or already expired). */
    public record ExpiringClientDto(
            Long clientId,
            String name,
            String phone,
            ClientType type,
            LocalDate expiresAt,
            long daysLeft
    ) {}

    public record TopClientDto(
            Long clientId,
            String name,
            long totalSpent,
            long visitCount,
            int loyaltyPoints
    ) {}
}
