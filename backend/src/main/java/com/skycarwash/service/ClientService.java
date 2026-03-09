package com.skycarwash.service;

import com.skycarwash.dto.ClientDto;
import com.skycarwash.entity.Client;
import com.skycarwash.exception.BusinessException;
import com.skycarwash.repository.ClientRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;

    // ── List / search ────────────────────────────────────────────────── //

    public List<ClientDto> findAll() {
        return clientRepository.findAllByActiveTrueOrderByNameAsc().stream()
                .map(this::toDto)
                .toList();
    }

    public List<ClientDto> search(String q) {
        List<Client> byName  = clientRepository.findByNameContainingIgnoreCaseAndActiveTrue(q);
        List<Client> byPhone = clientRepository.findByPhoneContainingAndActiveTrue(q);

        List<Client> merged = new ArrayList<>(byName);
        byPhone.stream()
                .filter(c -> merged.stream().noneMatch(m -> m.getId().equals(c.getId())))
                .forEach(merged::add);

        return merged.stream().map(this::toDto).toList();
    }

    public ClientDto findById(Long id) {
        return clientRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new EntityNotFoundException("Client not found: " + id));
    }

    // ── Create ───────────────────────────────────────────────────────── //

    @Transactional
    public ClientDto create(ClientDto dto) {
        if (clientRepository.findByPhone(dto.phone()).isPresent()) {
            throw new BusinessException("Un client avec ce numéro existe déjà");
        }
        Client entity = Client.builder()
                .name(dto.name())
                .phone(dto.phone())
                .type(dto.type())
                .balance(dto.balance())
                .expiresAt(dto.expiresAt())
                .build();
        return toDto(clientRepository.save(entity));
    }

    // ── Update ───────────────────────────────────────────────────────── //

    @Transactional
    public ClientDto update(Long id, ClientDto dto) {
        Client entity = clientRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Client not found: " + id));

        // Phone change: ensure uniqueness
        if (!entity.getPhone().equals(dto.phone()) &&
                clientRepository.findByPhone(dto.phone()).isPresent()) {
            throw new BusinessException("Ce numéro de téléphone est déjà utilisé");
        }

        entity.setName(dto.name());
        entity.setPhone(dto.phone());
        entity.setType(dto.type());
        entity.setBalance(dto.balance());
        entity.setExpiresAt(dto.expiresAt());
        entity.setActive(dto.active());
        return toDto(clientRepository.save(entity));
    }

    // ── Restock / top-up balance ─────────────────────────────────────── //

    @Transactional
    public ClientDto addPassages(Long id, int passages) {
        if (passages <= 0) throw new BusinessException("Le nombre de passages doit être positif");
        Client entity = clientRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Client not found: " + id));
        entity.setBalance(entity.getBalance() + passages);
        return toDto(clientRepository.save(entity));
    }

    // ── Deactivate ───────────────────────────────────────────────────── //

    @Transactional
    public void deactivate(Long id) {
        Client entity = clientRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Client not found: " + id));
        entity.setActive(false);
        clientRepository.save(entity);
    }

    // ── Mapping ──────────────────────────────────────────────────────── //

    public ClientDto toDto(Client c) {
        return new ClientDto(
                c.getId(), c.getName(), c.getPhone(),
                c.getType(), c.getBalance(), c.getExpiresAt(),
                c.isActive(), c.getCreatedAt()
        );
    }
}
