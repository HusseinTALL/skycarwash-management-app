package com.skycarwash.service;

import com.skycarwash.dto.*;
import com.skycarwash.entity.Client;
import com.skycarwash.entity.Transaction;
import com.skycarwash.exception.BusinessException;
import com.skycarwash.repository.ClientRepository;
import com.skycarwash.repository.TransactionRepository;
import com.skycarwash.repository.VehicleRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;
    private final TransactionRepository transactionRepository;
    private final VehicleRepository vehicleRepository;

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

    /**
     * Enriched, segmentable client list. All filters are optional.
     *
     * @param q       free text matched against name or phone
     * @param type    CARTE / BOUCLIER / VIP
     * @param status  "active" (default), "inactive" or "all"
     * @param tag     single tag the client must carry
     * @param segment computed CRM segment (NOUVEAU, FIDELE, REGULIER, A_RELANCER, INACTIF)
     * @param sort    name (default) | recent | spent | visits | created
     */
    public List<ClientListItemDto> findEnriched(String q, Client.ClientType type,
                                                String status, String tag,
                                                ClientSegment segment, String sort) {
        LocalDateTime now = LocalDateTime.now();
        Map<Long, long[]> stats = new HashMap<>();          // clientId -> [totalSpent, visitCount]
        Map<Long, LocalDateTime> lastVisit = new HashMap<>();
        for (Object[] row : transactionRepository.aggregateStatsByClient()) {
            Long clientId = (Long) row[0];
            stats.put(clientId, new long[]{((Number) row[1]).longValue(), ((Number) row[2]).longValue()});
            lastVisit.put(clientId, (LocalDateTime) row[3]);
        }
        Map<Long, Long> vehicleCounts = loadVehicleCounts();

        String needle = q == null ? "" : q.trim().toLowerCase();
        String wantStatus = status == null ? "active" : status.toLowerCase();
        String wantTag = tag == null ? null : tag.trim().toLowerCase();

        List<ClientListItemDto> rows = new ArrayList<>();
        for (Client c : clientRepository.findAll()) {
            if (!matchesStatus(c, wantStatus)) continue;
            if (type != null && c.getType() != type) continue;
            if (!needle.isEmpty()
                    && !c.getName().toLowerCase().contains(needle)
                    && !c.getPhone().toLowerCase().contains(needle)) continue;
            List<String> tags = parseTags(c.getTags());
            if (wantTag != null && tags.stream().noneMatch(t -> t.equalsIgnoreCase(wantTag))) continue;

            long[] s = stats.getOrDefault(c.getId(), new long[]{0, 0});
            ClientSegment computed = ClientSegment.of(s[1], lastVisit.get(c.getId()), c.getCreatedAt(), now);
            if (segment != null && computed != segment) continue;
            rows.add(new ClientListItemDto(
                    c.getId(), c.getName(), c.getPhone(), c.getType(), c.getBalance(),
                    c.getExpiresAt(), tags, c.isActive(), c.getCreatedAt(),
                    s[0], s[1], lastVisit.get(c.getId()),
                    vehicleCounts.getOrDefault(c.getId(), 0L),
                    c.getLoyaltyPoints(), computed
            ));
        }
        rows.sort(comparatorFor(sort));
        return rows;
    }

    public ClientDto findById(Long id) {
        return clientRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new EntityNotFoundException("Client not found: " + id));
    }

    /** Full "Client 360" profile: identity + stats + vehicles + history. */
    public ClientSummaryDto getSummary(Long id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Client not found: " + id));

        List<Transaction> txs =
                transactionRepository.findByClientIdAndCancelledAtIsNullOrderByCreatedAtDesc(id);

        long totalSpent = txs.stream().mapToLong(Transaction::getAmount).sum();
        long visitCount = txs.size();
        LocalDateTime lastVisit = txs.isEmpty() ? null : txs.get(0).getCreatedAt();

        List<ClientHistoryItemDto> history = txs.stream()
                .map(t -> new ClientHistoryItemDto(
                        t.getId(),
                        t.getService().getName(),
                        t.getAmount(),
                        t.getPaymentMethod().name(),
                        t.getCreatedAt(),
                        t.getCancelledAt() != null))
                .toList();

        List<VehicleDto> vehicles = vehicleRepository.findByClientIdOrderByCreatedAtAsc(id).stream()
                .map(v -> new VehicleDto(v.getId(), v.getPlate(), v.getLabel(), v.getType(), v.getCreatedAt()))
                .toList();

        ClientSegment segment = ClientSegment.of(visitCount, lastVisit,
                client.getCreatedAt(), LocalDateTime.now());
        return new ClientSummaryDto(toDto(client), totalSpent, visitCount, lastVisit,
                client.getLoyaltyPoints(), segment, vehicles, history);
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
                .notes(trimToNull(dto.notes()))
                .tags(joinTags(dto.tags()))
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
        entity.setNotes(trimToNull(dto.notes()));
        entity.setTags(joinTags(dto.tags()));
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

    // ── Aggregate helpers ─────────────────────────────────────────────── //

    private Map<Long, Long> loadVehicleCounts() {
        Map<Long, Long> map = new HashMap<>();
        for (Object[] row : vehicleRepository.aggregateCountsByClient()) {
            map.put((Long) row[0], ((Number) row[1]).longValue());
        }
        return map;
    }

    private boolean matchesStatus(Client c, String wantStatus) {
        return switch (wantStatus) {
            case "inactive" -> !c.isActive();
            case "all" -> true;
            default -> c.isActive();
        };
    }

    private Comparator<ClientListItemDto> comparatorFor(String sort) {
        String key = sort == null ? "name" : sort.toLowerCase();
        return switch (key) {
            case "spent"  -> Comparator.comparingLong(ClientListItemDto::totalSpent).reversed();
            case "visits" -> Comparator.comparingLong(ClientListItemDto::visitCount).reversed();
            case "created" -> Comparator.comparing(ClientListItemDto::createdAt,
                    Comparator.nullsLast(Comparator.reverseOrder()));
            case "recent" -> Comparator.comparing(ClientListItemDto::lastVisitAt,
                    Comparator.nullsLast(Comparator.reverseOrder()));
            default -> Comparator.comparing(ClientListItemDto::name, String.CASE_INSENSITIVE_ORDER);
        };
    }

    // ── Tags helpers ──────────────────────────────────────────────────── //

    /** Normalise a list of tags into a comma-joined string (trimmed, de-duplicated, capped). */
    static String joinTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) return null;
        LinkedHashSet<String> clean = new LinkedHashSet<>();
        for (String t : tags) {
            if (t == null) continue;
            String v = t.trim();
            if (!v.isEmpty()) clean.add(v);
        }
        if (clean.isEmpty()) return null;
        String joined = String.join(",", clean);
        return joined.length() > 255 ? joined.substring(0, 255) : joined;
    }

    static List<String> parseTags(String tags) {
        if (tags == null || tags.isBlank()) return List.of();
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    // ── Mapping ──────────────────────────────────────────────────────── //

    public ClientDto toDto(Client c) {
        return new ClientDto(
                c.getId(), c.getName(), c.getPhone(),
                c.getType(), c.getBalance(), c.getExpiresAt(),
                c.getNotes(), parseTags(c.getTags()),
                c.isActive(), c.getCreatedAt()
        );
    }
}
