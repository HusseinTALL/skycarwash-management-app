package com.skycarwash.controller;

import com.skycarwash.dto.ClientDto;
import com.skycarwash.service.ClientService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;

    /** List all active clients (optionally filter by ?q=). */
    @GetMapping
    public ResponseEntity<List<ClientDto>> getAll(@RequestParam(required = false) String q) {
        if (q != null && q.length() >= 2) {
            return ResponseEntity.ok(clientService.search(q));
        }
        return ResponseEntity.ok(clientService.findAll());
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
}
