package com.skycarwash.repository;

import com.skycarwash.entity.InspectionReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InspectionReportRepository extends JpaRepository<InspectionReport, Long> {

    Optional<InspectionReport> findByTransactionId(Long transactionId);

    List<InspectionReport> findByCustomerPhoneOrderByCreatedAtDesc(String customerPhone);

    List<InspectionReport> findTop100ByOrderByCreatedAtDesc();
}
