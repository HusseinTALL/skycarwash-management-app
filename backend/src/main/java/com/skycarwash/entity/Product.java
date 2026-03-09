package com.skycarwash.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "product")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, precision = 10, scale = 3)
    @Builder.Default
    private BigDecimal stock = BigDecimal.ZERO;

    @Column(name = "alert_threshold", nullable = false, precision = 10, scale = 3)
    @Builder.Default
    private BigDecimal alertThreshold = BigDecimal.ZERO;

    /** L, kg, unit, etc. */
    @Column(nullable = false, length = 20)
    private String unit;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
