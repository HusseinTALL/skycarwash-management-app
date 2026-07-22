package com.skycarwash.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Objet retrouvé dans le véhicule (argent, téléphone, documents…). */
@Entity
@Table(name = "inspection_found_item")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InspectionFoundItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id", nullable = false)
    private InspectionReport report;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private int quantity = 1;

    @Column(columnDefinition = "TEXT")
    private String remark;

    /** Photo associée (optionnelle) — id d'une inspection_photo. */
    @Column(name = "photo_id")
    private Long photoId;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
