package com.skycarwash.repository;

import com.skycarwash.entity.LoyaltyMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoyaltyMovementRepository extends JpaRepository<LoyaltyMovement, Long> {

    List<LoyaltyMovement> findTop30ByClientIdOrderByCreatedAtDesc(Long clientId);

    /** EARN entries linked to a wash — used to reverse points when it is cancelled. */
    List<LoyaltyMovement> findByTransactionIdAndType(Long transactionId, LoyaltyMovement.MovementType type);
}
