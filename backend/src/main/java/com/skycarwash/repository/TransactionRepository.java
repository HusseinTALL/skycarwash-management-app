package com.skycarwash.repository;

import com.skycarwash.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime from, LocalDateTime to);

    @Query("SELECT t FROM Transaction t WHERE t.createdAt >= :from AND t.createdAt < :to AND t.cancelledAt IS NULL")
    List<Transaction> findActiveTransactionsBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.createdAt >= :from AND t.createdAt < :to AND t.cancelledAt IS NULL")
    long countActiveTransactionsBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
