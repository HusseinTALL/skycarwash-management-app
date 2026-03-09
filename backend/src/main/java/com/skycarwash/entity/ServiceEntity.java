package com.skycarwash.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "service")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ServiceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    /** Price in FCFA */
    @Column(nullable = false)
    private int price;

    @Column(length = 50)
    private String category;

    /** e.g. {"shampoo_l": 0.05, "wax_g": 10} stored as JSONB */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "product_consumption", columnDefinition = "jsonb")
    private Map<String, Double> productConsumption;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
