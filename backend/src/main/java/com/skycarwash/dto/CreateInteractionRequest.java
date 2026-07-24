package com.skycarwash.dto;

import com.skycarwash.entity.ClientInteraction.InteractionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateInteractionRequest(
        @NotNull InteractionType type,
        @NotBlank @Size(max = 5000) String notes,
        LocalDate followUpAt
) {}
