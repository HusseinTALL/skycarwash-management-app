package com.skycarwash.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PortalChangeCodeRequest(
        /** Nouveau code : 4 à 6 chiffres. */
        @NotBlank @Pattern(regexp = "^[0-9]{4,6}$", message = "Le code doit contenir 4 à 6 chiffres")
        String newCode
) {}
