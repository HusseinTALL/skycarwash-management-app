package com.skycarwash.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Dommage déjà présent sur le véhicule avant le lavage. */
@Entity
@Table(name = "inspection_damage")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InspectionDamage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id", nullable = false)
    private InspectionReport report;

    /** Zone, ex : « Aile avant gauche ». */
    @Column(name = "zone_label", length = 100)
    private String zoneLabel;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    /** Photo associée (optionnelle) — id d'une inspection_photo. */
    @Column(name = "photo_id")
    private Long photoId;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
