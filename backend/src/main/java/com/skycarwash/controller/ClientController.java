package com.skycarwash.controller;

import com.skycarwash.dto.*;
import com.skycarwash.entity.Client;
import com.skycarwash.service.ClientService;
import com.skycarwash.service.CrmService;
import com.skycarwash.service.VehicleService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;
    private final VehicleService vehicleService;
    private final CrmService crmService;

    /**
     * Segmentable, stats-enriched client list.
     * Optional filters: q, type, status (active|inactive|all), tag, segment,
     * sort (name|recent|spent|visits|created).
     */
    @GetMapping
    public ResponseEntity<List<ClientListItemDto>> getAll(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Client.ClientType type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) ClientSegment segment,
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(clientService.findEnriched(q, type, status, tag, segment, sort));
    }

    /** CRM cockpit: segment counts, follow-ups due, expiring subscriptions, top clients. */
    @GetMapping("/crm/overview")
    public ResponseEntity<CrmOverviewDto> crmOverview() {
        return ResponseEntity.ok(crmService.overview());
    }

    /** Search clients by name or phone — used by caisse ABONNEMENT picker. */
    @GetMapping("/search")
    public ResponseEntity<List<ClientDto>> search(@RequestParam String q) {
        if (q == null || q.length() < 2) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(clientService.search(q));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClientDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(clientService.findById(id));
    }

    /** Full "Client 360" profile: identity + stats + vehicles + history. */
    @GetMapping("/{id}/summary")
    public ResponseEntity<ClientSummaryDto> getSummary(@PathVariable Long id) {
        return ResponseEntity.ok(clientService.getSummary(id));
    }

    @PostMapping
    public ResponseEntity<ClientDto> create(@Valid @RequestBody ClientDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clientService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClientDto> update(@PathVariable Long id, @Valid @RequestBody ClientDto dto) {
        return ResponseEntity.ok(clientService.update(id, dto));
    }

    /** Add passages to a client's CARTE balance (2-click top-up). */
    @PostMapping("/{id}/add-passages")
    public ResponseEntity<ClientDto> addPassages(
            @PathVariable Long id,
            @RequestBody Map<String, @Min(1) Integer> body) {
        int passages = body.getOrDefault("passages", 0);
        return ResponseEntity.ok(clientService.addPassages(id, passages));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        clientService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    // ── Vehicles (sub-resource) ──────────────────────────────────────── //

    @GetMapping("/{id}/vehicles")
    public ResponseEntity<List<VehicleDto>> listVehicles(@PathVariable Long id) {
        return ResponseEntity.ok(vehicleService.findByClient(id));
    }

    @PostMapping("/{id}/vehicles")
    public ResponseEntity<VehicleDto> addVehicle(
            @PathVariable Long id, @Valid @RequestBody VehicleDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vehicleService.add(id, dto));
    }

    @PutMapping("/{id}/vehicles/{vehicleId}")
    public ResponseEntity<VehicleDto> updateVehicle(
            @PathVariable Long id, @PathVariable Long vehicleId, @Valid @RequestBody VehicleDto dto) {
        return ResponseEntity.ok(vehicleService.update(id, vehicleId, dto));
    }

    @DeleteMapping("/{id}/vehicles/{vehicleId}")
    public ResponseEntity<Void> deleteVehicle(
            @PathVariable Long id, @PathVariable Long vehicleId) {
        vehicleService.delete(id, vehicleId);
        return ResponseEntity.noContent().build();
    }

    // ── Interaction journal (sub-resource) ───────────────────────────── //

    @GetMapping("/{id}/interactions")
    public ResponseEntity<List<InteractionDto>> listInteractions(@PathVariable Long id) {
        return ResponseEntity.ok(crmService.listInteractions(id));
    }

    @PostMapping("/{id}/interactions")
    public ResponseEntity<InteractionDto> addInteraction(
            @PathVariable Long id,
            @Valid @RequestBody CreateInteractionRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        String phone = principal != null ? principal.getUsername() : null;
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(crmService.addInteraction(id, request, phone));
    }

    /** Mark a follow-up ("relance") as handled. */
    @PutMapping("/{id}/interactions/{interactionId}/done")
    public ResponseEntity<InteractionDto> markFollowUpDone(
            @PathVariable Long id, @PathVariable Long interactionId) {
        return ResponseEntity.ok(crmService.markFollowUpDone(id, interactionId));
    }

    @DeleteMapping("/{id}/interactions/{interactionId}")
    public ResponseEntity<Void> deleteInteraction(
            @PathVariable Long id, @PathVariable Long interactionId) {
        crmService.deleteInteraction(id, interactionId);
        return ResponseEntity.noContent().build();
    }

    // ── Loyalty program (sub-resource) ───────────────────────────────── //

    @GetMapping("/{id}/loyalty")
    public ResponseEntity<LoyaltyStatusDto> getLoyalty(@PathVariable Long id) {
        return ResponseEntity.ok(crmService.getLoyalty(id));
    }

    @PostMapping("/{id}/loyalty/redeem")
    public ResponseEntity<LoyaltyStatusDto> redeemPoints(
            @PathVariable Long id, @Valid @RequestBody RedeemPointsRequest request) {
        return ResponseEntity.ok(crmService.redeem(id, request));
    }
}
