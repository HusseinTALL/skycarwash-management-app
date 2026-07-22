package com.skycarwash.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Vue complète d'un rapport d'état — partagée par le staff et le portail client. */
public record InspectionReportResponse(
        Long id,
        Long transactionId,
        String customerName,
        String customerPhone,
        String vehicleType,
        String plate,
        String vehicleLabel,
        String remarks,
        String status,
        String agency,
        String createdByName,
        LocalDateTime createdAt,
        // ── Infos commande (si liée) ──────────────────────────────
        String serviceName,
        Integer amount,
        String paymentMethod,
        LocalDateTime orderCreatedAt,
        Boolean orderCancelled,
        // ── Détails ───────────────────────────────────────────────
        List<PhotoDto> photos,
        List<DamageDto> damages,
        List<FoundItemDto> foundItems
) {
    public record PhotoDto(
            Long id,
            String phase,
            String zone,
            String caption,
            LocalDateTime createdAt
    ) {}

    public record DamageDto(
            Long id,
            String zoneLabel,
            String description,
            Long photoId
    ) {}

    public record FoundItemDto(
            Long id,
            String name,
            String description,
            int quantity,
            String remark,
            Long photoId
    ) {}
}
