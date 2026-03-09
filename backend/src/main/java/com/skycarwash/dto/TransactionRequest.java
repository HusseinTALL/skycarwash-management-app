package com.skycarwash.dto;

import com.skycarwash.entity.Transaction.PaymentMethod;
import jakarta.validation.constraints.NotNull;

public record TransactionRequest(
        @NotNull Long serviceId,
        /** Required when paymentMethod == ABONNEMENT */
        Long clientId,
        @NotNull PaymentMethod paymentMethod
) {}
