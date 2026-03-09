package com.skycarwash.service;

import com.skycarwash.dto.CancelRequest;
import com.skycarwash.dto.TransactionRequest;
import com.skycarwash.dto.TransactionResponse;
import com.skycarwash.entity.*;
import com.skycarwash.entity.Transaction.PaymentMethod;
import com.skycarwash.exception.BusinessException;
import com.skycarwash.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock TransactionRepository   transactionRepository;
    @Mock ServiceRepository       serviceRepository;
    @Mock ClientRepository        clientRepository;
    @Mock UserRepository          userRepository;
    @Mock ProductRepository       productRepository;
    @Mock StockMovementRepository stockMovementRepository;
    @Mock SimpMessagingTemplate   messagingTemplate;

    @InjectMocks TransactionService transactionService;

    private ServiceEntity service;
    private User           user;
    private Client         client;

    @BeforeEach
    void setUp() {
        service = ServiceEntity.builder()
                .id(1L).name("Lavage simple").price(2000)
                .active(true).build();

        user = User.builder()
                .id(1L).name("Alice").phone("70000001")
                .role(User.Role.EMPLOYEE).passwordHash("hash").build();

        client = Client.builder()
                .id(1L).name("Bob").phone("70000002")
                .type(Client.ClientType.CARTE).balance(5).active(true).build();
    }

    // ── CREATE ──────────────────────────────────────────────────────── //

    @Test
    void createCashTransaction_returnsMappedResponse() {
        TransactionRequest req = new TransactionRequest(1L, null, PaymentMethod.CASH);

        when(serviceRepository.findById(1L)).thenReturn(Optional.of(service));
        when(userRepository.findByPhone("70000001")).thenReturn(Optional.of(user));

        Transaction saved = Transaction.builder()
                .id(10L).service(service).user(user)
                .amount(2000).paymentMethod(PaymentMethod.CASH)
                .createdAt(LocalDateTime.now()).build();
        when(transactionRepository.save(any())).thenReturn(saved);

        TransactionResponse resp = transactionService.create(req, "70000001");

        assertThat(resp.id()).isEqualTo(10L);
        assertThat(resp.amount()).isEqualTo(2000);
        assertThat(resp.paymentMethod()).isEqualTo("CASH");
        assertThat(resp.clientId()).isNull();
        verify(transactionRepository).save(any(Transaction.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/dashboard"), any(Map.class));
    }

    @Test
    void createAbonnement_decrementsClientBalance() {
        TransactionRequest req = new TransactionRequest(1L, 1L, PaymentMethod.ABONNEMENT);

        when(serviceRepository.findById(1L)).thenReturn(Optional.of(service));
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(userRepository.findByPhone("70000001")).thenReturn(Optional.of(user));

        Transaction saved = Transaction.builder()
                .id(11L).service(service).client(client).user(user)
                .amount(2000).paymentMethod(PaymentMethod.ABONNEMENT)
                .createdAt(LocalDateTime.now()).build();
        when(transactionRepository.save(any())).thenReturn(saved);

        transactionService.create(req, "70000001");

        assertThat(client.getBalance()).isEqualTo(4); // 5 → 4
        verify(clientRepository).save(client);
    }

    @Test
    void createAbonnement_insufficientBalance_throwsBusinessException() {
        client.setBalance(0);
        TransactionRequest req = new TransactionRequest(1L, 1L, PaymentMethod.ABONNEMENT);

        when(serviceRepository.findById(1L)).thenReturn(Optional.of(service));
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));

        assertThatThrownBy(() -> transactionService.create(req, "70000001"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Solde insuffisant");
    }

    @Test
    void createAbonnement_withoutClientId_throwsBusinessException() {
        TransactionRequest req = new TransactionRequest(1L, null, PaymentMethod.ABONNEMENT);

        when(serviceRepository.findById(1L)).thenReturn(Optional.of(service));

        assertThatThrownBy(() -> transactionService.create(req, "70000001"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("client");
    }

    @Test
    void createAbonnement_inactiveClient_throwsBusinessException() {
        client.setActive(false);
        TransactionRequest req = new TransactionRequest(1L, 1L, PaymentMethod.ABONNEMENT);

        when(serviceRepository.findById(1L)).thenReturn(Optional.of(service));
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));

        assertThatThrownBy(() -> transactionService.create(req, "70000001"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("abonnement actif");
    }

    @Test
    void create_inactiveService_throwsBusinessException() {
        service.setActive(false);
        TransactionRequest req = new TransactionRequest(1L, null, PaymentMethod.CASH);

        when(serviceRepository.findById(1L)).thenReturn(Optional.of(service));

        assertThatThrownBy(() -> transactionService.create(req, "70000001"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void create_unknownService_throwsEntityNotFoundException() {
        TransactionRequest req = new TransactionRequest(99L, null, PaymentMethod.CASH);

        when(serviceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.create(req, "70000001"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void create_stockDecremented_onProductConsumption() {
        service.setProductConsumption(Map.of("shampoo", 0.05));
        TransactionRequest req = new TransactionRequest(1L, null, PaymentMethod.CASH);

        Product shampoo = Product.builder()
                .id(5L).name("shampoo").stock(new BigDecimal("2.000"))
                .alertThreshold(new BigDecimal("0.500")).unit("L").build();

        when(serviceRepository.findById(1L)).thenReturn(Optional.of(service));
        when(userRepository.findByPhone("70000001")).thenReturn(Optional.of(user));
        when(productRepository.findByNameIgnoreCase("shampoo")).thenReturn(Optional.of(shampoo));

        Transaction saved = Transaction.builder()
                .id(12L).service(service).user(user)
                .amount(2000).paymentMethod(PaymentMethod.CASH)
                .createdAt(LocalDateTime.now()).build();
        when(transactionRepository.save(any())).thenReturn(saved);

        transactionService.create(req, "70000001");

        assertThat(shampoo.getStock()).isEqualByComparingTo("1.950");
        verify(stockMovementRepository).save(argThat(mv ->
                mv.getType() == StockMovement.MovementType.OUT));
    }

    // ── CANCEL ──────────────────────────────────────────────────────── //

    @Test
    void cancel_withinWindow_marksAsCancelled() {
        Transaction tx = Transaction.builder()
                .id(10L).service(service).user(user)
                .amount(2000).paymentMethod(PaymentMethod.CASH)
                .createdAt(LocalDateTime.now().minusSeconds(30)).build();

        when(transactionRepository.findById(10L)).thenReturn(Optional.of(tx));
        when(stockMovementRepository.findByTransactionId(10L)).thenReturn(List.of());
        when(transactionRepository.save(any())).thenReturn(tx);

        transactionService.cancel(10L, new CancelRequest("Saisie erronée"), "70000001");

        assertThat(tx.getCancelledAt()).isNotNull();
        assertThat(tx.getCancelReason()).isEqualTo("Saisie erronée");
    }

    @Test
    void cancel_afterTwoMinutes_throwsBusinessException() {
        Transaction tx = Transaction.builder()
                .id(10L).service(service).user(user)
                .amount(2000).paymentMethod(PaymentMethod.CASH)
                .createdAt(LocalDateTime.now().minusMinutes(5)).build();

        when(transactionRepository.findById(10L)).thenReturn(Optional.of(tx));

        assertThatThrownBy(() -> transactionService.cancel(10L, new CancelRequest("late"), "70000001"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("délai d'annulation");
    }

    @Test
    void cancel_alreadyCancelled_throwsBusinessException() {
        Transaction tx = Transaction.builder()
                .id(10L).service(service).user(user)
                .amount(2000).paymentMethod(PaymentMethod.CASH)
                .createdAt(LocalDateTime.now().minusSeconds(10))
                .cancelledAt(LocalDateTime.now().minusSeconds(5)).build();

        when(transactionRepository.findById(10L)).thenReturn(Optional.of(tx));

        assertThatThrownBy(() -> transactionService.cancel(10L, new CancelRequest("dup"), "70000001"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("déjà été annulée");
    }

    @Test
    void cancel_abonnementTransaction_restoresClientBalance() {
        client.setBalance(4); // already decremented
        Transaction tx = Transaction.builder()
                .id(11L).service(service).client(client).user(user)
                .amount(2000).paymentMethod(PaymentMethod.ABONNEMENT)
                .createdAt(LocalDateTime.now().minusSeconds(30)).build();

        when(transactionRepository.findById(11L)).thenReturn(Optional.of(tx));
        when(stockMovementRepository.findByTransactionId(11L)).thenReturn(List.of());
        when(transactionRepository.save(any())).thenReturn(tx);

        transactionService.cancel(11L, new CancelRequest("erreur client"), "70000001");

        assertThat(client.getBalance()).isEqualTo(5); // restored
        verify(clientRepository).save(client);
    }

    @Test
    void cancel_revertsStockMovements() {
        Transaction tx = Transaction.builder()
                .id(12L).service(service).user(user)
                .amount(2000).paymentMethod(PaymentMethod.CASH)
                .createdAt(LocalDateTime.now().minusSeconds(20)).build();

        Product shampoo = Product.builder()
                .id(5L).name("shampoo").stock(new BigDecimal("1.950"))
                .alertThreshold(new BigDecimal("0.500")).unit("L").build();

        StockMovement outMove = StockMovement.builder()
                .id(1L).product(shampoo).quantity(new BigDecimal("0.050"))
                .type(StockMovement.MovementType.OUT).transaction(tx).build();

        when(transactionRepository.findById(12L)).thenReturn(Optional.of(tx));
        when(stockMovementRepository.findByTransactionId(12L)).thenReturn(List.of(outMove));
        when(transactionRepository.save(any())).thenReturn(tx);

        transactionService.cancel(12L, new CancelRequest("test"), "70000001");

        assertThat(shampoo.getStock()).isEqualByComparingTo("2.000"); // 1.950 + 0.050
        verify(productRepository).save(shampoo);
        verify(stockMovementRepository).save(argThat(mv ->
                mv.getType() == StockMovement.MovementType.IN));
    }
}
