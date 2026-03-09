package com.skycarwash.controller;

import com.skycarwash.entity.Client;
import com.skycarwash.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Minimal client endpoints needed for S3–S4 (caisse client search).
 * Full CRUD (create, update, detail) is implemented in S5–S6.
 */
@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientRepository clientRepository;

    /**
     * Search active clients by name or phone fragment.
     * Used by the caisse when selecting an ABONNEMENT client.
     */
    @GetMapping("/search")
    public ResponseEntity<List<Map<String, Object>>> search(@RequestParam String q) {
        if (q == null || q.isBlank() || q.length() < 2) {
            return ResponseEntity.badRequest().build();
        }

        // Search by name or phone — merge results, deduplicate by id
        List<Client> byName  = clientRepository.findByNameContainingIgnoreCaseAndActiveTrue(q);
        List<Client> byPhone = clientRepository.findByPhoneContainingAndActiveTrue(q);

        List<Client> merged = byName.stream()
                .filter(c -> c.getBalance() > 0)
                .collect(java.util.stream.Collectors.toList());
        byPhone.stream()
                .filter(c -> c.getBalance() > 0)
                .filter(c -> merged.stream().noneMatch(m -> m.getId().equals(c.getId())))
                .forEach(merged::add);

        List<Map<String, Object>> result = merged.stream().map(c -> Map.<String, Object>of(
                "id",      c.getId(),
                "name",    c.getName(),
                "phone",   c.getPhone(),
                "type",    c.getType().name(),
                "balance", c.getBalance()
        )).toList();

        return ResponseEntity.ok(result);
    }
}
