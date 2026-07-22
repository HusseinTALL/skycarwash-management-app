package com.skycarwash.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Full "Client 360" profile: identity + stats + vehicles + wash history. */
public record ClientSummaryDto(
        ClientDto client,
        long totalSpent,
        long visitCount,
        LocalDateTime lastVisitAt,
        long loyaltyPoints,
        List<VehicleDto> vehicles,
        List<ClientHistoryItemDto> history
) {}
