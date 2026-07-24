package com.skycarwash.dto;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Automatic CRM segment computed from visit recency & frequency (RFM-lite).
 * Never persisted — always derived from transaction history.
 */
public enum ClientSegment {
    /** First contact: registered less than 30 days ago, at most 2 visits */
    NOUVEAU,
    /** 8+ visits and seen within the last 30 days */
    FIDELE,
    /** Seen within the last 30 days */
    REGULIER,
    /** Last visit 31–90 days ago — worth a win-back call */
    A_RELANCER,
    /** No visit for more than 90 days (or never came at all) */
    INACTIF;

    public static final int RECENT_DAYS = 30;
    public static final int CHURN_DAYS = 90;
    public static final int LOYAL_VISITS = 8;

    public static ClientSegment of(long visitCount, LocalDateTime lastVisitAt,
                                   LocalDateTime createdAt, LocalDateTime now) {
        if (lastVisitAt == null || visitCount == 0) {
            return isNew(createdAt, now) ? NOUVEAU : INACTIF;
        }
        long days = ChronoUnit.DAYS.between(lastVisitAt, now);
        if (days > CHURN_DAYS) return INACTIF;
        if (days > RECENT_DAYS) return A_RELANCER;
        if (visitCount >= LOYAL_VISITS) return FIDELE;
        if (visitCount <= 2 && isNew(createdAt, now)) return NOUVEAU;
        return REGULIER;
    }

    private static boolean isNew(LocalDateTime createdAt, LocalDateTime now) {
        return createdAt != null && ChronoUnit.DAYS.between(createdAt, now) <= RECENT_DAYS;
    }
}
