package com.skycarwash.dto;

public record PortalLoginResponse(
        String token,
        String phone,
        boolean mustChangeCode
) {}
