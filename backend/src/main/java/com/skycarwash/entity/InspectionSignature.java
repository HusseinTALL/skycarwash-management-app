package com.skycarwash.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Signature électronique du client validant l'état initial du véhicule
 * (une par rapport). Image tracée sur tablette/smartphone, stockée en bytea.
 */
@Entity
@Table(name = "inspection_signature")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InspectionSignature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id", nullable = false, unique = true)
    private InspectionReport report;

    @Column(name = "signer_name", length = 100)
    private String signerName;

    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType;

    @Column(name = "image_data", nullable = false)
    private byte[] imageData;

    @Column(name = "signed_at", nullable = false)
    @Builder.Default
    private LocalDateTime signedAt = LocalDateTime.now();
}
