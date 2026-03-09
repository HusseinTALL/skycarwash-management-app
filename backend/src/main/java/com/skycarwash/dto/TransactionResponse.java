package com.skycarwash.dto;

import java.time.LocalDateTime;

public record TransactionResponse(
        Long id,
        Long serviceId,
        String serviceName,
        int servicePrice,
        Long clientId,
        String clientName,
        /** Remaining subscription balance after this transaction; null if not applicable */
        Integer clientBalanceAfter,
        Long userId,
        String userName,
        int amount,
        String paymentMethod,
        LocalDateTime createdAt,
        LocalDateTime cancelledAt,
        String cancelReason
) {}
