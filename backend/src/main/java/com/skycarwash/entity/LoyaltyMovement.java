package com.skycarwash.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Auditable ledger entry for the loyalty program (signed point delta). */
@Entity
@Table(name = "loyalty_movement")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LoyaltyMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    /** Wash that earned the points (null for redemptions / manual adjustments) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    /** Signed delta: positive = earned, negative = redeemed */
    @Column(nullable = false)
    private int points;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MovementType type;

    @Column(length = 255)
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum MovementType {
        EARN, REDEEM, ADJUST
    }
}
