package com.skycarwash.entity;

import com.skycarwash.entity.Vehicle.VehicleType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * État des lieux d'un véhicule au moment d'une commande.
 * Sert de preuve contre les litiges (dommages, objets oubliés…).
 */
@Entity
@Table(name = "inspection_report")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InspectionReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** La commande associée — une seule inspection par transaction. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", unique = true)
    private Transaction transaction;

    /** Client enregistré éventuel (null pour un client de passage). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    @Column(name = "customer_name", length = 100)
    private String customerName;

    /** Numéro utilisé pour l'accès au portail client. */
    @Column(name = "customer_phone", length = 20)
    private String customerPhone;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", length = 20)
    private VehicleType vehicleType;

    @Column(length = 20)
    private String plate;

    @Column(name = "vehicle_label", length = 100)
    private String vehicleLabel;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.VALIDATED;

    /** L'employé/caissier qui a réalisé le contrôle. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<InspectionDamage> damages = new ArrayList<>();

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<InspectionFoundItem> foundItems = new ArrayList<>();

    public void addDamage(InspectionDamage d) {
        d.setReport(this);
        damages.add(d);
    }

    public void addFoundItem(InspectionFoundItem i) {
        i.setReport(this);
        foundItems.add(i);
    }

    public enum Status {
        /** En cours de saisie. */
        DRAFT,
        /** État initial enregistré avant le lavage. */
        VALIDATED,
        /** Photos « après lavage » ajoutées. */
        COMPLETED
    }
}
