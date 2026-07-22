package com.skycarwash.dto;

import java.time.LocalDateTime;

/** Ligne d'historique (liste des rapports) — sans les détails lourds. */
public record InspectionSummaryResponse(
        Long id,
        String customerName,
        String customerPhone,
        String vehicleType,
        String plate,
        String status,
        LocalDateTime createdAt,
        String serviceName,
        Integer amount,
        int beforePhotoCount,
        int afterPhotoCount,
        int damageCount,
        int foundItemCount
) {}
