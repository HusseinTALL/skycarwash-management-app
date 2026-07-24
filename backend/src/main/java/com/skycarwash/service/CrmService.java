package com.skycarwash.service;

import com.skycarwash.dto.*;
import com.skycarwash.entity.Client;
import com.skycarwash.entity.ClientInteraction;
import com.skycarwash.entity.LoyaltyMovement;
import com.skycarwash.entity.Transaction;
import com.skycarwash.entity.User;
import com.skycarwash.exception.BusinessException;
import com.skycarwash.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Advanced CRM: interaction journal, loyalty program and cockpit overview.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CrmService {

    /** Subscriptions expiring within this many days appear in the overview. */
    private static final int EXPIRY_WINDOW_DAYS = 7;
    private static final int TOP_CLIENTS_LIMIT = 5;

    private final ClientRepository clientRepository;
    private final ClientInteractionRepository interactionRepository;
    private final LoyaltyMovementRepository loyaltyMovementRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    // ── Interaction journal ──────────────────────────────────────────── //

    public List<InteractionDto> listInteractions(Long clientId) {
        requireClient(clientId);
        return interactionRepository.findByClientIdOrderByCreatedAtDesc(clientId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public InteractionDto addInteraction(Long clientId, CreateInteractionRequest req, String userPhone) {
        Client client = requireClient(clientId);
        User user = userPhone == null ? null : userRepository.findByPhone(userPhone).orElse(null);
        if (req.followUpAt() != null && req.followUpAt().isBefore(LocalDate.now())) {
            throw new BusinessException("La date de relance doit être aujourd'hui ou plus tard");
        }
        ClientInteraction saved = interactionRepository.save(ClientInteraction.builder()
                .client(client)
                .user(user)
                .type(req.type())
                .notes(req.notes().trim())
                .followUpAt(req.followUpAt())
                .build());
        return toDto(saved);
    }

    @Transactional
    public InteractionDto markFollowUpDone(Long clientId, Long interactionId) {
        ClientInteraction interaction = requireInteraction(clientId, interactionId);
        interaction.setFollowUpDone(true);
        return toDto(interactionRepository.save(interaction));
    }

    @Transactional
    public void deleteInteraction(Long clientId, Long interactionId) {
        interactionRepository.delete(requireInteraction(clientId, interactionId));
    }

    // ── Loyalty program ──────────────────────────────────────────────── //

    public LoyaltyStatusDto getLoyalty(Long clientId) {
        Client client = requireClient(clientId);
        List<LoyaltyMovementDto> movements =
                loyaltyMovementRepository.findTop30ByClientIdOrderByCreatedAtDesc(clientId).stream()
                        .map(m -> new LoyaltyMovementDto(m.getId(), m.getPoints(), m.getType(),
                                m.getNote(), m.getCreatedAt()))
                        .toList();
        return new LoyaltyStatusDto(client.getLoyaltyPoints(), movements);
    }

    /** One point per completed wash. Called from TransactionService within the same transaction. */
    @Transactional
    public void earnPoint(Client client, Transaction tx) {
        client.setLoyaltyPoints(client.getLoyaltyPoints() + 1);
        clientRepository.save(client);
        loyaltyMovementRepository.save(LoyaltyMovement.builder()
                .client(client)
                .transaction(tx)
                .points(1)
                .type(LoyaltyMovement.MovementType.EARN)
                .note("Lavage : " + tx.getService().getName())
                .build());
    }

    /** Take back points earned by a wash that is being cancelled (balance floors at 0). */
    @Transactional
    public void reverseEarnedPoints(Transaction tx) {
        Client client = tx.getClient();
        if (client == null) return;
        int earned = loyaltyMovementRepository
                .findByTransactionIdAndType(tx.getId(), LoyaltyMovement.MovementType.EARN)
                .stream()
                .mapToInt(LoyaltyMovement::getPoints)
                .sum();
        if (earned <= 0) return;

        int delta = -Math.min(earned, client.getLoyaltyPoints());
        client.setLoyaltyPoints(client.getLoyaltyPoints() + delta);
        clientRepository.save(client);
        loyaltyMovementRepository.save(LoyaltyMovement.builder()
                .client(client)
                .transaction(tx)
                .points(delta)
                .type(LoyaltyMovement.MovementType.ADJUST)
                .note("Annulation du lavage #" + tx.getId())
                .build());
    }

    @Transactional
    public LoyaltyStatusDto redeem(Long clientId, RedeemPointsRequest req) {
        Client client = requireClient(clientId);
        if (req.points() > client.getLoyaltyPoints()) {
            throw new BusinessException("Solde de points insuffisant (" + client.getLoyaltyPoints() + " disponibles)");
        }
        client.setLoyaltyPoints(client.getLoyaltyPoints() - req.points());
        clientRepository.save(client);
        loyaltyMovementRepository.save(LoyaltyMovement.builder()
                .client(client)
                .points(-req.points())
                .type(LoyaltyMovement.MovementType.REDEEM)
                .note(req.note() == null || req.note().isBlank() ? "Récompense fidélité" : req.note().trim())
                .build());
        log.info("Loyalty redeem — client #{} spent {} points", clientId, req.points());
        return getLoyalty(clientId);
    }

    // ── CRM overview (cockpit) ───────────────────────────────────────── //

    public CrmOverviewDto overview() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        Map<Long, long[]> stats = new HashMap<>();          // clientId -> [totalSpent, visitCount]
        Map<Long, LocalDateTime> lastVisit = new HashMap<>();
        for (Object[] row : transactionRepository.aggregateStatsByClient()) {
            Long clientId = (Long) row[0];
            stats.put(clientId, new long[]{((Number) row[1]).longValue(), ((Number) row[2]).longValue()});
            lastVisit.put(clientId, (LocalDateTime) row[3]);
        }

        Map<String, Long> segmentCounts = new LinkedHashMap<>();
        for (ClientSegment s : ClientSegment.values()) segmentCounts.put(s.name(), 0L);
        List<CrmOverviewDto.ExpiringClientDto> expiring = new ArrayList<>();
        List<CrmOverviewDto.TopClientDto> top = new ArrayList<>();

        for (Client c : clientRepository.findAll()) {
            if (!c.isActive()) continue;
            long[] s = stats.getOrDefault(c.getId(), new long[]{0, 0});

            ClientSegment segment = ClientSegment.of(s[1], lastVisit.get(c.getId()), c.getCreatedAt(), now);
            segmentCounts.merge(segment.name(), 1L, Long::sum);

            if (c.getExpiresAt() != null) {
                long daysLeft = ChronoUnit.DAYS.between(today, c.getExpiresAt());
                if (daysLeft <= EXPIRY_WINDOW_DAYS) {
                    expiring.add(new CrmOverviewDto.ExpiringClientDto(
                            c.getId(), c.getName(), c.getPhone(), c.getType(), c.getExpiresAt(), daysLeft));
                }
            }
            if (s[1] > 0) {
                top.add(new CrmOverviewDto.TopClientDto(
                        c.getId(), c.getName(), s[0], s[1], c.getLoyaltyPoints()));
            }
        }

        expiring.sort(Comparator.comparing(CrmOverviewDto.ExpiringClientDto::expiresAt));
        top.sort(Comparator.comparingLong(CrmOverviewDto.TopClientDto::totalSpent).reversed());

        List<CrmOverviewDto.FollowUpDto> followUps = interactionRepository
                .findByFollowUpDoneFalseAndFollowUpAtLessThanEqualOrderByFollowUpAtAsc(today).stream()
                .map(i -> new CrmOverviewDto.FollowUpDto(
                        i.getId(), i.getClient().getId(), i.getClient().getName(),
                        i.getClient().getPhone(), i.getType(), i.getNotes(), i.getFollowUpAt()))
                .toList();

        return new CrmOverviewDto(segmentCounts, followUps, expiring,
                top.size() > TOP_CLIENTS_LIMIT ? top.subList(0, TOP_CLIENTS_LIMIT) : top);
    }

    // ── Helpers ──────────────────────────────────────────────────────── //

    private Client requireClient(Long clientId) {
        return clientRepository.findById(clientId)
                .orElseThrow(() -> new EntityNotFoundException("Client not found: " + clientId));
    }

    private ClientInteraction requireInteraction(Long clientId, Long interactionId) {
        ClientInteraction interaction = interactionRepository.findById(interactionId)
                .orElseThrow(() -> new EntityNotFoundException("Interaction not found: " + interactionId));
        if (!interaction.getClient().getId().equals(clientId)) {
            throw new BusinessException("Cette interaction n'appartient pas à ce client");
        }
        return interaction;
    }

    private InteractionDto toDto(ClientInteraction i) {
        return new InteractionDto(
                i.getId(), i.getType(), i.getNotes(),
                i.getFollowUpAt(), i.isFollowUpDone(),
                i.getUser() != null ? i.getUser().getName() : null,
                i.getCreatedAt());
    }
}
