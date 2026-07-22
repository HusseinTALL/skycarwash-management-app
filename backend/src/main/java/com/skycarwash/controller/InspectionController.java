package com.skycarwash.controller;

import com.skycarwash.dto.CreateInspectionRequest;
import com.skycarwash.dto.InspectionReportResponse;
import com.skycarwash.dto.InspectionReportResponse.PhotoDto;
import com.skycarwash.dto.InspectionSummaryResponse;
import com.skycarwash.entity.InspectionPhoto;
import com.skycarwash.entity.InspectionPhoto.Phase;
import com.skycarwash.entity.InspectionPhoto.Zone;
import com.skycarwash.service.InspectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.List;

/** Endpoints staff : capture & consultation des rapports d'état. */
@RestController
@RequestMapping("/api/inspections")
@RequiredArgsConstructor
public class InspectionController {

    private final InspectionService inspectionService;

    @PostMapping
    public ResponseEntity<InspectionReportResponse> create(
            @Valid @RequestBody CreateInspectionRequest req,
            @AuthenticationPrincipal UserDetails principal) {
        InspectionReportResponse res = inspectionService.create(req, principal.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    @PostMapping(value = "/{id}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PhotoDto> addPhoto(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam("phase") Phase phase,
            @RequestParam("zone") Zone zone,
            @RequestParam(value = "caption", required = false) String caption,
            @RequestParam(value = "damageId", required = false) Long damageId,
            @RequestParam(value = "foundItemId", required = false) Long foundItemId) {
        PhotoDto dto = inspectionService.addPhoto(id, file, phase, zone, caption, damageId, foundItemId);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/{id}")
    public ResponseEntity<InspectionReportResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(inspectionService.getReport(id));
    }

    @GetMapping
    public ResponseEntity<?> list(@RequestParam(value = "transactionId", required = false) Long transactionId) {
        if (transactionId != null) {
            InspectionReportResponse res = inspectionService.getByTransaction(transactionId);
            return res != null ? ResponseEntity.ok(res) : ResponseEntity.noContent().build();
        }
        List<InspectionSummaryResponse> summaries = inspectionService.listRecent();
        return ResponseEntity.ok(summaries);
    }

    @GetMapping("/photos/{photoId}")
    public ResponseEntity<byte[]> photo(@PathVariable Long photoId) {
        InspectionPhoto photo = inspectionService.getPhoto(photoId);
        return imageResponse(photo);
    }

    static ResponseEntity<byte[]> imageResponse(InspectionPhoto photo) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.getContentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePrivate())
                .body(photo.getImageData());
    }
}
