package com.skycarwash.dto;

import com.skycarwash.entity.Vehicle.VehicleType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Création d'un rapport d'état (état initial, avant lavage).
 * Les photos sont envoyées séparément (multipart) après création.
 */
public record CreateInspectionRequest(
        /** Commande associée (optionnelle si le rapport est autonome). */
        Long transactionId,
        Long clientId,
        @Size(max = 100) String customerName,
        /** Numéro requis pour donner au client l'accès au portail. */
        @Size(max = 20) String customerPhone,
        VehicleType vehicleType,
        @Size(max = 20) String plate,
        @Size(max = 100) String vehicleLabel,
        String remarks,
        @Valid List<DamageInput> damages,
        @Valid List<FoundItemInput> foundItems
) {
    public record DamageInput(
            @Size(max = 100) String zoneLabel,
            @NotBlank String description
    ) {}

    public record FoundItemInput(
            @NotBlank @Size(max = 100) String name,
            String description,
            @Positive Integer quantity,
            String remark
    ) {}
}
