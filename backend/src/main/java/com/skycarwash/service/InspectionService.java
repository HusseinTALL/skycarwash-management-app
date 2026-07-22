package com.skycarwash.service;

import com.skycarwash.dto.CreateInspectionRequest;
import com.skycarwash.dto.InspectionReportResponse;
import com.skycarwash.dto.InspectionReportResponse.*;
import com.skycarwash.dto.InspectionSummaryResponse;
import com.skycarwash.entity.*;
import com.skycarwash.entity.InspectionPhoto.Phase;
import com.skycarwash.entity.InspectionPhoto.Zone;
import com.skycarwash.exception.BusinessException;
import com.skycarwash.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InspectionService {

    private final InspectionReportRepository   reportRepository;
    private final InspectionPhotoRepository    photoRepository;
    private final InspectionDamageRepository   damageRepository;
    private final InspectionFoundItemRepository foundItemRepository;
    private final PortalCustomerRepository      portalCustomerRepository;
    private final TransactionRepository         transactionRepository;
    private final ClientRepository              clientRepository;
    private final UserRepository                userRepository;
    private final PasswordEncoder               passwordEncoder;

    @Value("${app.agency-name:SkyCarWash}")
    private String agencyName;

    // ================================================================== //
    //  CREATE REPORT                                                       //
    // ================================================================== //

    @Transactional
    public InspectionReportResponse create(CreateInspectionRequest req, String userPhone) {
        User user = userRepository.findByPhone(userPhone)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur introuvable : " + userPhone));

        Transaction transaction = null;
        if (req.transactionId() != null) {
            transaction = transactionRepository.findById(req.transactionId())
                    .orElseThrow(() -> new EntityNotFoundException("Commande introuvable : " + req.transactionId()));
            if (reportRepository.findByTransactionId(transaction.getId()).isPresent()) {
                throw new BusinessException("Un rapport existe déjà pour cette commande");
            }
        }

        Client client = null;
        if (req.clientId() != null) {
            client = clientRepository.findById(req.clientId())
                    .orElseThrow(() -> new EntityNotFoundException("Client introuvable : " + req.clientId()));
        }

        String phone = normalizePhone(req.customerPhone());
        // Fall back to the linked client's phone when the caissier didn't retype it.
        if (phone == null && client != null) {
            phone = normalizePhone(client.getPhone());
        }

        InspectionReport report = InspectionReport.builder()
                .transaction(transaction)
                .client(client)
                .customerName(blankToNull(req.customerName()) != null
                        ? req.customerName()
                        : (client != null ? client.getName() : null))
                .customerPhone(phone)
                .vehicleType(req.vehicleType())
                .plate(blankToNull(req.plate()))
                .vehicleLabel(blankToNull(req.vehicleLabel()))
                .remarks(blankToNull(req.remarks()))
                .status(InspectionReport.Status.VALIDATED)
                .createdBy(user)
                .build();

        if (req.damages() != null) {
            req.damages().forEach(d -> report.addDamage(InspectionDamage.builder()
                    .zoneLabel(blankToNull(d.zoneLabel()))
                    .description(d.description())
                    .build()));
        }
        if (req.foundItems() != null) {
            req.foundItems().forEach(i -> report.addFoundItem(InspectionFoundItem.builder()
                    .name(i.name())
                    .description(blankToNull(i.description()))
                    .quantity(i.quantity() != null ? i.quantity() : 1)
                    .remark(blankToNull(i.remark()))
                    .build()));
        }

        InspectionReport saved = reportRepository.save(report);

        // Give the client access to the portal (default code = last 4 digits).
        provisionPortalAccess(phone);

        log.info("Inspection #{} created — vehicle={} plate='{}' by user={}",
                saved.getId(), saved.getVehicleType(), saved.getPlate(), userPhone);

        return toResponse(saved);
    }

    // ================================================================== //
    //  PHOTOS                                                              //
    // ================================================================== //

    @Transactional
    public PhotoDto addPhoto(Long reportId, MultipartFile file, Phase phase, Zone zone,
                             String caption, Long damageId, Long foundItemId) {
        InspectionReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new EntityNotFoundException("Rapport introuvable : " + reportId));

        if (file == null || file.isEmpty()) {
            throw new BusinessException("Fichier photo manquant");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException("Le fichier doit être une image");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BusinessException("Impossible de lire le fichier photo");
        }

        InspectionPhoto photo = photoRepository.save(InspectionPhoto.builder()
                .report(report)
                .phase(phase)
                .zone(zone)
                .contentType(contentType)
                .imageData(bytes)
                .caption(blankToNull(caption))
                .build());

        // Optionally attach the photo to a specific damage or found item.
        if (damageId != null) {
            InspectionDamage d = damageRepository.findById(damageId)
                    .orElseThrow(() -> new EntityNotFoundException("Dommage introuvable : " + damageId));
            assertBelongsToReport(d.getReport().getId(), reportId);
            d.setPhotoId(photo.getId());
            damageRepository.save(d);
        }
        if (foundItemId != null) {
            InspectionFoundItem i = foundItemRepository.findById(foundItemId)
                    .orElseThrow(() -> new EntityNotFoundException("Objet introuvable : " + foundItemId));
            assertBelongsToReport(i.getReport().getId(), reportId);
            i.setPhotoId(photo.getId());
            foundItemRepository.save(i);
        }

        // Adding an "after" photo marks the report as completed.
        if (phase == Phase.AFTER && report.getStatus() != InspectionReport.Status.COMPLETED) {
            report.setStatus(InspectionReport.Status.COMPLETED);
            report.setUpdatedAt(LocalDateTime.now());
            reportRepository.save(report);
        }

        return new PhotoDto(photo.getId(), phase.name(), zone.name(), photo.getCaption(), photo.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public InspectionPhoto getPhoto(Long photoId) {
        return photoRepository.findById(photoId)
                .orElseThrow(() -> new EntityNotFoundException("Photo introuvable : " + photoId));
    }

    // ================================================================== //
    //  READ (staff)                                                        //
    // ================================================================== //

    @Transactional(readOnly = true)
    public InspectionReportResponse getReport(Long id) {
        return toResponse(reportRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Rapport introuvable : " + id)));
    }

    @Transactional(readOnly = true)
    public InspectionReportResponse getByTransaction(Long transactionId) {
        return reportRepository.findByTransactionId(transactionId)
                .map(this::toResponse)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<InspectionSummaryResponse> listRecent() {
        return reportRepository.findTop100ByOrderByCreatedAtDesc().stream()
                .map(this::toSummary)
                .toList();
    }

    // ================================================================== //
    //  READ (portal – scoped to a phone)                                   //
    // ================================================================== //

    @Transactional(readOnly = true)
    public List<InspectionSummaryResponse> listByPhone(String phone) {
        return reportRepository.findByCustomerPhoneOrderByCreatedAtDesc(normalizePhone(phone)).stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public InspectionReportResponse getReportForPhone(Long id, String phone) {
        InspectionReport report = reportRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Rapport introuvable : " + id));
        assertOwnedByPhone(report, phone);
        return toResponse(report);
    }

    @Transactional(readOnly = true)
    public InspectionPhoto getPhotoForPhone(Long photoId, String phone) {
        InspectionPhoto photo = getPhoto(photoId);
        assertOwnedByPhone(photo.getReport(), phone);
        return photo;
    }

    // ================================================================== //
    //  PORTAL PROVISIONING                                                 //
    // ================================================================== //

    /** Ensures a portal account exists; default code = last 4 digits of the phone. */
    @Transactional
    public void provisionPortalAccess(String phone) {
        String normalized = normalizePhone(phone);
        if (normalized == null) return;
        String digits = normalized.replaceAll("\\D", "");
        if (digits.length() < 4) return; // can't derive a default code
        if (portalCustomerRepository.findByPhone(normalized).isPresent()) return;

        String defaultCode = digits.substring(digits.length() - 4);
        portalCustomerRepository.save(PortalCustomer.builder()
                .phone(normalized)
                .accessCodeHash(passwordEncoder.encode(defaultCode))
                .mustChangeCode(true)
                .build());
        log.info("Portal access provisioned for {}", normalized);
    }

    // ================================================================== //
    //  MAPPING & HELPERS                                                   //
    // ================================================================== //

    private InspectionReportResponse toResponse(InspectionReport r) {
        List<PhotoDto> photos = photoRepository.findMetaByReportIdOrderByCreatedAtAsc(r.getId()).stream()
                .map(p -> new PhotoDto(p.getId(), p.getPhase().name(), p.getZone().name(),
                        p.getCaption(), p.getCreatedAt()))
                .toList();

        List<DamageDto> damages = r.getDamages().stream()
                .map(d -> new DamageDto(d.getId(), d.getZoneLabel(), d.getDescription(), d.getPhotoId()))
                .toList();

        List<FoundItemDto> items = r.getFoundItems().stream()
                .map(i -> new FoundItemDto(i.getId(), i.getName(), i.getDescription(),
                        i.getQuantity(), i.getRemark(), i.getPhotoId()))
                .toList();

        Transaction tx = r.getTransaction();
        return new InspectionReportResponse(
                r.getId(),
                tx != null ? tx.getId() : null,
                r.getCustomerName(),
                r.getCustomerPhone(),
                r.getVehicleType() != null ? r.getVehicleType().name() : null,
                r.getPlate(),
                r.getVehicleLabel(),
                r.getRemarks(),
                r.getStatus().name(),
                agencyName,
                r.getCreatedBy() != null ? r.getCreatedBy().getName() : null,
                r.getCreatedAt(),
                tx != null ? tx.getService().getName() : null,
                tx != null ? tx.getAmount() : null,
                tx != null ? tx.getPaymentMethod().name() : null,
                tx != null ? tx.getCreatedAt() : null,
                tx != null ? (tx.getCancelledAt() != null) : null,
                photos, damages, items
        );
    }

    private InspectionSummaryResponse toSummary(InspectionReport r) {
        Transaction tx = r.getTransaction();
        return new InspectionSummaryResponse(
                r.getId(),
                r.getCustomerName(),
                r.getCustomerPhone(),
                r.getVehicleType() != null ? r.getVehicleType().name() : null,
                r.getPlate(),
                r.getStatus().name(),
                r.getCreatedAt(),
                tx != null ? tx.getService().getName() : null,
                tx != null ? tx.getAmount() : null,
                photoRepository.countByReportIdAndPhase(r.getId(), Phase.BEFORE),
                photoRepository.countByReportIdAndPhase(r.getId(), Phase.AFTER),
                r.getDamages().size(),
                r.getFoundItems().size()
        );
    }

    private void assertOwnedByPhone(InspectionReport report, String phone) {
        if (report.getCustomerPhone() == null
                || !report.getCustomerPhone().equals(normalizePhone(phone))) {
            throw new BusinessException("Ce rapport n'est pas accessible",
                    org.springframework.http.HttpStatus.FORBIDDEN);
        }
    }

    private void assertBelongsToReport(Long actualReportId, Long expectedReportId) {
        if (!actualReportId.equals(expectedReportId)) {
            throw new BusinessException("Élément lié à un autre rapport");
        }
    }

    private static String normalizePhone(String phone) {
        if (phone == null) return null;
        String trimmed = phone.replaceAll("\\s+", "");
        return trimmed.isBlank() ? null : trimmed;
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
