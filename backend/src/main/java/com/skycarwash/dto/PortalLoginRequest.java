package com.skycarwash.dto;

import jakarta.validation.constraints.NotBlank;

public record PortalLoginRequest(
        @NotBlank String phone,
        @NotBlank String code
) {}
