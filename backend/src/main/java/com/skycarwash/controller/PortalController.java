package com.skycarwash.controller;

import com.skycarwash.dto.*;
import com.skycarwash.entity.InspectionPhoto;
import com.skycarwash.service.InspectionService;
import com.skycarwash.service.PortalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Portail client « Rapports de lavage » — accès sans compte staff.
 * Le principal (via {@code @AuthenticationPrincipal}) est le numéro de téléphone.
 */
@RestController
@RequestMapping("/api/portal")
@RequiredArgsConstructor
public class PortalController {

    private final PortalService portalService;
    private final InspectionService inspectionService;

    @PostMapping("/login")
    public ResponseEntity<PortalLoginResponse> login(@Valid @RequestBody PortalLoginRequest req) {
        return ResponseEntity.ok(portalService.login(req));
    }

    @PostMapping("/change-code")
    public ResponseEntity<Void> changeCode(
            @Valid @RequestBody PortalChangeCodeRequest req,
            @AuthenticationPrincipal String phone) {
        portalService.changeCode(phone, req.newCode());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/reports")
    public ResponseEntity<List<InspectionSummaryResponse>> reports(@AuthenticationPrincipal String phone) {
        return ResponseEntity.ok(inspectionService.listByPhone(phone));
    }

    @GetMapping("/reports/{id}")
    public ResponseEntity<InspectionReportResponse> report(
            @PathVariable Long id, @AuthenticationPrincipal String phone) {
        return ResponseEntity.ok(inspectionService.getReportForPhone(id, phone));
    }

    @GetMapping("/photos/{photoId}")
    public ResponseEntity<byte[]> photo(
            @PathVariable Long photoId, @AuthenticationPrincipal String phone) {
        InspectionPhoto photo = inspectionService.getPhotoForPhone(photoId, phone);
        return InspectionController.imageResponse(photo);
    }
}
