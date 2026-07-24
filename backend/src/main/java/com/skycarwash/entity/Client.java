package com.skycarwash.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "client")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClientType type;

    /** Remaining passage count (CARTE), or 0 for others */
    @Column(nullable = false)
    @Builder.Default
    private int balance = 0;

    @Column(name = "expires_at")
    private LocalDate expiresAt;

    /** Free-text CRM notes about the client */
    @Column(columnDefinition = "TEXT")
    private String notes;

    /** Comma-separated tags (e.g. "flotte,fidèle") — kept flat for simple filtering */
    @Column(length = 255)
    private String tags;

    /** Loyalty balance — earned per wash, spendable via the loyalty ledger */
    @Column(name = "loyalty_points", nullable = false)
    @Builder.Default
    private int loyaltyPoints = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum ClientType {
        CARTE, BOUCLIER, VIP
    }
}
