package com.skycarwash.service;

import com.skycarwash.dto.ClientDto;
import com.skycarwash.dto.ClientListItemDto;
import com.skycarwash.dto.ClientSummaryDto;
import com.skycarwash.entity.Client;
import com.skycarwash.entity.ServiceEntity;
import com.skycarwash.entity.Transaction;
import com.skycarwash.entity.Transaction.PaymentMethod;
import com.skycarwash.repository.ClientRepository;
import com.skycarwash.repository.TransactionRepository;
import com.skycarwash.repository.VehicleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock ClientRepository clientRepository;
    @Mock TransactionRepository transactionRepository;
    @Mock VehicleRepository vehicleRepository;

    @InjectMocks ClientService clientService;

    private Client client(long id, String name, Client.ClientType type, boolean active, String tags) {
        return Client.builder()
                .id(id).name(name).phone("7000000" + id)
                .type(type).active(active).tags(tags).build();
    }

    // ── Tags normalisation ───────────────────────────────────────────── //

    @Test
    void joinTags_trimsDeduplicatesAndDropsBlanks() {
        assertThat(ClientService.joinTags(List.of(" flotte ", "fidèle", "flotte", "  ")))
                .isEqualTo("flotte,fidèle");
        assertThat(ClientService.joinTags(List.of())).isNull();
        assertThat(ClientService.joinTags(null)).isNull();
    }

    @Test
    void parseTags_splitsAndTrims() {
        assertThat(ClientService.parseTags("flotte, fidèle ,")).containsExactly("flotte", "fidèle");
        assertThat(ClientService.parseTags(null)).isEmpty();
        assertThat(ClientService.parseTags("  ")).isEmpty();
    }

    // ── Segmentation ─────────────────────────────────────────────────── //

    @Test
    void findEnriched_filtersByTypeStatusTag_andSortsBySpend() {
        Client a = client(1, "Alice", Client.ClientType.VIP, true, "flotte");
        Client b = client(2, "Bob", Client.ClientType.CARTE, true, "fidèle");
        Client c = client(3, "Carl", Client.ClientType.VIP, false, "flotte");

        when(clientRepository.findAll()).thenReturn(List.of(a, b, c));
        // a spent 5000 (2 visits), b spent 9000 (1 visit)
        when(transactionRepository.aggregateStatsByClient()).thenReturn(List.of(
                new Object[]{1L, 5000L, 2L, LocalDateTime.now()},
                new Object[]{2L, 9000L, 1L, LocalDateTime.now()}
        ));
        when(vehicleRepository.aggregateCountsByClient()).thenReturn(List.<Object[]>of(
                new Object[]{1L, 2L}
        ));

        // active VIPs only → excludes Bob (CARTE) and Carl (inactive)
        List<ClientListItemDto> vips = clientService.findEnriched(null, Client.ClientType.VIP, "active", null, "spent");
        assertThat(vips).extracting(ClientListItemDto::name).containsExactly("Alice");
        assertThat(vips.get(0).totalSpent()).isEqualTo(5000);
        assertThat(vips.get(0).vehicleCount()).isEqualTo(2);

        // tag filter
        List<ClientListItemDto> fideles = clientService.findEnriched(null, null, "active", "fidèle", "name");
        assertThat(fideles).extracting(ClientListItemDto::name).containsExactly("Bob");

        // status=all, sort by spend desc → Bob (9000) before Alice (5000)
        List<ClientListItemDto> all = clientService.findEnriched(null, null, "all", null, "spent");
        assertThat(all).extracting(ClientListItemDto::name).containsExactly("Bob", "Alice", "Carl");
    }

    // ── Client 360 summary ───────────────────────────────────────────── //

    @Test
    void getSummary_computesSpendVisitsAndLoyalty() {
        Client bob = client(1, "Bob", Client.ClientType.CARTE, true, null);
        ServiceEntity svc = ServiceEntity.builder().id(1L).name("Lavage complet").price(4000).active(true).build();

        LocalDateTime recent = LocalDateTime.now();
        Transaction t1 = Transaction.builder().id(10L).service(svc).amount(4000)
                .paymentMethod(PaymentMethod.CASH).createdAt(recent).build();
        Transaction t2 = Transaction.builder().id(11L).service(svc).amount(2000)
                .paymentMethod(PaymentMethod.ORANGE).createdAt(recent.minusDays(3)).build();

        when(clientRepository.findById(1L)).thenReturn(Optional.of(bob));
        when(transactionRepository.findByClientIdAndCancelledAtIsNullOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(t1, t2));
        when(vehicleRepository.findByClientIdOrderByCreatedAtAsc(1L)).thenReturn(List.of());

        ClientSummaryDto summary = clientService.getSummary(1L);

        assertThat(summary.totalSpent()).isEqualTo(6000);
        assertThat(summary.visitCount()).isEqualTo(2);
        assertThat(summary.loyaltyPoints()).isEqualTo(2);
        assertThat(summary.lastVisitAt()).isEqualTo(recent);
        assertThat(summary.history()).hasSize(2);
        assertThat(summary.history().get(0).serviceName()).isEqualTo("Lavage complet");
    }

    @Test
    void toDto_exposesTagsAsList() {
        Client c = client(1, "Alice", Client.ClientType.VIP, true, "flotte,fidèle");
        ClientDto dto = clientService.toDto(c);
        assertThat(dto.tags()).containsExactly("flotte", "fidèle");
    }
}
