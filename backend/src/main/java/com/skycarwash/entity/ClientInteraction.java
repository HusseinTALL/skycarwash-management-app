package com.skycarwash.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** A logged contact with a client (call, visit, complaint…) with optional follow-up. */
@Entity
@Table(name = "client_interaction")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ClientInteraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    /** Staff member who logged the interaction (null if the account was deleted) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InteractionType type;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String notes;

    @Column(name = "follow_up_at")
    private LocalDate followUpAt;

    @Column(name = "follow_up_done", nullable = false)
    @Builder.Default
    private boolean followUpDone = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum InteractionType {
        CALL, SMS, WHATSAPP, VISIT, COMPLAINT, FEEDBACK, OTHER
    }
}
