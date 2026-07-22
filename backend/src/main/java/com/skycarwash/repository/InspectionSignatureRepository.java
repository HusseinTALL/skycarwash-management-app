package com.skycarwash.repository;

import com.skycarwash.entity.InspectionSignature;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface InspectionSignatureRepository extends JpaRepository<InspectionSignature, Long> {

    Optional<InspectionSignature> findByReportId(Long reportId);

    /** Metadata only (closed projection) — avoids loading the signature bytes. */
    interface SignatureMeta {
        String getSignerName();
        LocalDateTime getSignedAt();
    }

    Optional<SignatureMeta> findMetaByReportId(Long reportId);
}
