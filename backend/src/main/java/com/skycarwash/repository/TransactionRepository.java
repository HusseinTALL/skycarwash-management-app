package com.skycarwash.repository;

import com.skycarwash.entity.Transaction;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    List<Transaction> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime from, LocalDateTime to);

    @Query("SELECT t FROM Transaction t WHERE t.createdAt >= :from AND t.createdAt < :to AND t.cancelledAt IS NULL")
    List<Transaction> findActiveTransactionsBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.createdAt >= :from AND t.createdAt < :to AND t.cancelledAt IS NULL")
    long countActiveTransactionsBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // ── CRM: per-client history & aggregates ─────────────────────────── //

    /** Full non-cancelled wash history for a single client, most recent first. */
    List<Transaction> findByClientIdAndCancelledAtIsNullOrderByCreatedAtDesc(Long clientId);

    /** Aggregate stats [clientId, totalSpent, visitCount, lastVisit] for every client (non-cancelled). */
    @Query("""
            SELECT t.client.id, COALESCE(SUM(t.amount), 0), COUNT(t), MAX(t.createdAt)
            FROM Transaction t
            WHERE t.client IS NOT NULL AND t.cancelledAt IS NULL
            GROUP BY t.client.id
            """)
    List<Object[]> aggregateStatsByClient();
}
