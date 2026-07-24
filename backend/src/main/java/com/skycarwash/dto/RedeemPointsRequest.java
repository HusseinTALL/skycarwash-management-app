package com.skycarwash.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record RedeemPointsRequest(
        @Min(1) int points,
        @Size(max = 255) String note
) {}
