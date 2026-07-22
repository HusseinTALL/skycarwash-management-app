package com.skycarwash.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Accès du client au portail « Rapports de lavage » sans compte complet.
 * Connexion par téléphone + code (par défaut, les 4 derniers chiffres).
 */
@Entity
@Table(name = "portal_customer")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PortalCustomer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String phone;

    @Column(name = "access_code_hash", nullable = false)
    private String accessCodeHash;

    @Column(name = "must_change_code", nullable = false)
    @Builder.Default
    private boolean mustChangeCode = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
