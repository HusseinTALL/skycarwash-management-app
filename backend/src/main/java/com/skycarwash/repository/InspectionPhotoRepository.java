package com.skycarwash.repository;

import com.skycarwash.entity.InspectionPhoto;
import com.skycarwash.entity.InspectionPhoto.Phase;
import com.skycarwash.entity.InspectionPhoto.Zone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface InspectionPhotoRepository extends JpaRepository<InspectionPhoto, Long> {

    /** Metadata only (closed projection) — avoids loading the image bytea. */
    interface PhotoMeta {
        Long getId();
        Phase getPhase();
        Zone getZone();
        String getCaption();
        LocalDateTime getCreatedAt();
    }

    List<PhotoMeta> findMetaByReportIdOrderByCreatedAtAsc(Long reportId);

    int countByReportIdAndPhase(Long reportId, Phase phase);
}
