package com.skycarwash.repository;

import com.skycarwash.entity.ClientInteraction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ClientInteractionRepository extends JpaRepository<ClientInteraction, Long> {

    List<ClientInteraction> findByClientIdOrderByCreatedAtDesc(Long clientId);

    /** Pending follow-ups due on or before the given date, oldest first. */
    List<ClientInteraction> findByFollowUpDoneFalseAndFollowUpAtLessThanEqualOrderByFollowUpAtAsc(LocalDate date);
}
