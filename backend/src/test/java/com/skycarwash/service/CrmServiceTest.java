package com.skycarwash.service;

import com.skycarwash.dto.*;
import com.skycarwash.entity.Client;
import com.skycarwash.entity.ClientInteraction;
import com.skycarwash.entity.LoyaltyMovement;
import com.skycarwash.entity.ServiceEntity;
import com.skycarwash.entity.Transaction;
import com.skycarwash.exception.BusinessException;
import com.skycarwash.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrmServiceTest {

    @Mock ClientRepository clientRepository;
    @Mock ClientInteractionRepository interactionRepository;
    @Mock LoyaltyMovementRepository loyaltyMovementRepository;
    @Mock TransactionRepository transactionRepository;
    @Mock UserRepository userRepository;

    @InjectMocks CrmService crmService;

    private Client client(long id, int points) {
        return Client.builder()
                .id(id).name("Bob").phone("7000000" + id)
                .type(Client.ClientType.CARTE).loyaltyPoints(points)
                .active(true).createdAt(LocalDateTime.now().minusYears(1)).build();
    }

    // ── Segmentation rules ───────────────────────────────────────────── //

    @Test
    void segment_newClientWithoutVisits_isNouveau() {
        LocalDateTime now = LocalDateTime.now();
        assertThat(ClientSegment.of(0, null, now.minusDays(5), now))
                .isEqualTo(ClientSegment.NOUVEAU);
    }

    @Test
    void segment_oldClientWithoutVisits_isInactif() {
        LocalDateTime now = LocalDateTime.now();
        assertThat(ClientSegment.of(0, null, now.minusDays(60), now))
                .isEqualTo(ClientSegment.INACTIF);
    }

    @Test
    void segment_recentFrequentVisitor_isFidele() {
        LocalDateTime now = LocalDateTime.now();
        assertThat(ClientSegment.of(12, now.minusDays(3), now.minusYears(1), now))
                .isEqualTo(ClientSegment.FIDELE);
    }

    @Test
    void segment_recentOccasionalVisitor_isRegulier() {
        LocalDateTime now = LocalDateTime.now();
        assertThat(ClientSegment.of(4, now.minusDays(10), now.minusYears(1), now))
                .isEqualTo(ClientSegment.REGULIER);
    }

    @Test
    void segment_lapsedVisitor_isARelancer() {
        LocalDateTime now = LocalDateTime.now();
        assertThat(ClientSegment.of(6, now.minusDays(45), now.minusYears(1), now))
                .isEqualTo(ClientSegment.A_RELANCER);
    }

    @Test
    void segment_longGoneVisitor_isInactif() {
        LocalDateTime now = LocalDateTime.now();
        assertThat(ClientSegment.of(6, now.minusDays(120), now.minusYears(1), now))
                .isEqualTo(ClientSegment.INACTIF);
    }

    // ── Loyalty: earn / reverse / redeem ─────────────────────────────── //

    @Test
    void earnPoint_incrementsBalanceAndWritesLedger() {
        Client c = client(1, 3);
        Transaction tx = Transaction.builder().id(10L)
                .service(ServiceEntity.builder().id(1L).name("Lavage complet").price(2000).build())
                .build();

        crmService.earnPoint(c, tx);

        assertThat(c.getLoyaltyPoints()).isEqualTo(4);
        verify(clientRepository).save(c);
        verify(loyaltyMovementRepository).save(argThat(m ->
                m.getPoints() == 1 && m.getType() == LoyaltyMovement.MovementType.EARN));
    }

    @Test
    void reverseEarnedPoints_subtractsWhatTheWashEarned() {
        Client c = client(1, 4);
        Transaction tx = Transaction.builder().id(10L).client(c).build();
        when(loyaltyMovementRepository.findByTransactionIdAndType(10L, LoyaltyMovement.MovementType.EARN))
                .thenReturn(List.of(LoyaltyMovement.builder().points(1)
                        .type(LoyaltyMovement.MovementType.EARN).client(c).build()));

        crmService.reverseEarnedPoints(tx);

        assertThat(c.getLoyaltyPoints()).isEqualTo(3);
        verify(loyaltyMovementRepository).save(argThat(m ->
                m.getPoints() == -1 && m.getType() == LoyaltyMovement.MovementType.ADJUST));
    }

    @Test
    void reverseEarnedPoints_floorsAtZero() {
        Client c = client(1, 0); // points already spent
        Transaction tx = Transaction.builder().id(10L).client(c).build();
        when(loyaltyMovementRepository.findByTransactionIdAndType(10L, LoyaltyMovement.MovementType.EARN))
                .thenReturn(List.of(LoyaltyMovement.builder().points(1)
                        .type(LoyaltyMovement.MovementType.EARN).client(c).build()));

        crmService.reverseEarnedPoints(tx);

        assertThat(c.getLoyaltyPoints()).isZero();
        verify(loyaltyMovementRepository, never()).save(argThat(m -> m.getPoints() < -0));
    }

    @Test
    void redeem_subtractsPointsAndWritesLedger() {
        Client c = client(1, 10);
        when(clientRepository.findById(1L)).thenReturn(Optional.of(c));
        when(loyaltyMovementRepository.findTop30ByClientIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

        LoyaltyStatusDto status = crmService.redeem(1L, new RedeemPointsRequest(8, "Lavage gratuit"));

        assertThat(status.points()).isEqualTo(2);
        verify(loyaltyMovementRepository).save(argThat(m ->
                m.getPoints() == -8 && m.getType() == LoyaltyMovement.MovementType.REDEEM));
    }

    @Test
    void redeem_moreThanBalance_throwsBusinessException() {
        Client c = client(1, 3);
        when(clientRepository.findById(1L)).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> crmService.redeem(1L, new RedeemPointsRequest(5, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("insuffisant");
        verify(loyaltyMovementRepository, never()).save(any());
    }

    // ── Interaction journal ──────────────────────────────────────────── //

    @Test
    void addInteraction_persistsWithClientAndUser() {
        Client c = client(1, 0);
        when(clientRepository.findById(1L)).thenReturn(Optional.of(c));
        when(userRepository.findByPhone("70000001")).thenReturn(Optional.empty());
        when(interactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        InteractionDto dto = crmService.addInteraction(1L,
                new CreateInteractionRequest(ClientInteraction.InteractionType.CALL,
                        "  Rappeler pour renouvellement  ", LocalDate.now().plusDays(3)),
                "70000001");

        assertThat(dto.type()).isEqualTo(ClientInteraction.InteractionType.CALL);
        assertThat(dto.notes()).isEqualTo("Rappeler pour renouvellement");
        assertThat(dto.followUpDone()).isFalse();
    }

    @Test
    void addInteraction_followUpInThePast_throwsBusinessException() {
        Client c = client(1, 0);
        when(clientRepository.findById(1L)).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> crmService.addInteraction(1L,
                new CreateInteractionRequest(ClientInteraction.InteractionType.CALL,
                        "note", LocalDate.now().minusDays(1)), null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void markFollowUpDone_ofAnotherClient_throwsBusinessException() {
        Client other = client(2, 0);
        ClientInteraction interaction = ClientInteraction.builder()
                .id(7L).client(other).type(ClientInteraction.InteractionType.CALL)
                .notes("x").build();
        when(interactionRepository.findById(7L)).thenReturn(Optional.of(interaction));

        assertThatThrownBy(() -> crmService.markFollowUpDone(1L, 7L))
                .isInstanceOf(BusinessException.class);
    }

    // ── Overview ─────────────────────────────────────────────────────── //

    @Test
    void overview_countsSegmentsAndFlagsExpiringSubscriptions() {
        LocalDateTime now = LocalDateTime.now();
        Client fidele = client(1, 12);
        Client dormant = client(2, 0);
        dormant.setExpiresAt(LocalDate.now().plusDays(3));
        Client inactiveAccount = client(3, 0);
        inactiveAccount.setActive(false);

        when(clientRepository.findAll()).thenReturn(List.of(fidele, dormant, inactiveAccount));
        when(transactionRepository.aggregateStatsByClient()).thenReturn(List.of(
                new Object[]{1L, 50_000L, 12L, now.minusDays(2)},
                new Object[]{2L, 8_000L, 4L, now.minusDays(50)}
        ));
        when(interactionRepository
                .findByFollowUpDoneFalseAndFollowUpAtLessThanEqualOrderByFollowUpAtAsc(any()))
                .thenReturn(List.of());

        CrmOverviewDto overview = crmService.overview();

        assertThat(overview.segmentCounts().get("FIDELE")).isEqualTo(1);
        assertThat(overview.segmentCounts().get("A_RELANCER")).isEqualTo(1);
        // deactivated account is excluded everywhere
        assertThat(overview.segmentCounts().values().stream().mapToLong(Long::longValue).sum()).isEqualTo(2);
        assertThat(overview.expiringSoon()).hasSize(1);
        assertThat(overview.expiringSoon().get(0).clientId()).isEqualTo(2L);
        assertThat(overview.topClients()).extracting(CrmOverviewDto.TopClientDto::clientId)
                .containsExactly(1L, 2L);
    }
}
