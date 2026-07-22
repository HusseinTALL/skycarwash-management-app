package com.skycarwash.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Photo horodatée d'un véhicule, avant ou après lavage.
 * Les octets sont stockés en base (bytea) — voir V4 migration.
 */
@Entity
@Table(name = "inspection_photo")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InspectionPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id", nullable = false)
    private InspectionReport report;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Phase phase;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Zone zone;

    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType;

    @Column(name = "image_data", nullable = false)
    private byte[] imageData;

    @Column(length = 255)
    private String caption;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum Phase {
        BEFORE, AFTER
    }

    public enum Zone {
        AVANT, ARRIERE, GAUCHE, DROIT, INTERIEUR, COFFRE, DOMMAGE, AUTRE
    }
}
