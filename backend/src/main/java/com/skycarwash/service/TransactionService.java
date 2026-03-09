package com.skycarwash.service;

import com.skycarwash.dto.CancelRequest;
import com.skycarwash.dto.TransactionRequest;
import com.skycarwash.dto.TransactionResponse;
import com.skycarwash.entity.*;
import com.skycarwash.entity.Transaction.PaymentMethod;
import com.skycarwash.exception.BusinessException;
import com.skycarwash.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private static final int CANCEL_WINDOW_MINUTES = 2;

    private final TransactionRepository   transactionRepository;
    private final ServiceRepository       serviceRepository;
    private final ClientRepository        clientRepository;
    private final UserRepository          userRepository;
    private final ProductRepository       productRepository;
    private final StockMovementRepository stockMovementRepository;
    private final SimpMessagingTemplate   messagingTemplate;

    // ------------------------------------------------------------------ //
    //  CREATE TRANSACTION                                                  //
    // ------------------------------------------------------------------ //

    @Transactional
    public TransactionResponse create(TransactionRequest req, String phone) {

        // 1. Load & validate service
        ServiceEntity service = serviceRepository.findById(req.serviceId())
                .orElseThrow(() -> new EntityNotFoundException("Service not found: " + req.serviceId()));
        if (!service.isActive()) {
            throw new BusinessException("Service is no longer active");
        }

        // 2. Handle subscription (ABONNEMENT) payment
        Client client = null;
        if (req.paymentMethod() == PaymentMethod.ABONNEMENT) {
            if (req.clientId() == null) {
                throw new BusinessException("Un client doit être sélectionné pour un paiement par abonnement");
            }
            client = clientRepository.findById(req.clientId())
                    .orElseThrow(() -> new EntityNotFoundException("Client not found: " + req.clientId()));
            if (!client.isActive()) {
                throw new BusinessException("Ce client n'a pas d'abonnement actif");
            }
            if (client.getBalance() <= 0) {
                throw new BusinessException("Solde insuffisant — ce client n'a plus de passages disponibles");
            }
            client.setBalance(client.getBalance() - 1);
            clientRepository.save(client);
        }

        // 3. Load the employee/manager recording the transaction
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new EntityNotFoundException("User not found for phone: " + phone));

        // 4. Persist the transaction
        Transaction tx = Transaction.builder()
                .service(service)
                .client(client)
                .user(user)
                .amount(service.getPrice())
                .paymentMethod(req.paymentMethod())
                .build();
        tx = transactionRepository.save(tx);

        // 5. Decrement product stock for each consumed product
        decrementStock(service.getProductConsumption(), tx);

        // 6. Notify dashboard via WebSocket
        notifyDashboard("transaction.created", tx.getId());

        log.info("Transaction #{} created — service='{}' method={} user={}",
                tx.getId(), service.getName(), req.paymentMethod(), phone);

        return toResponse(tx, client);
    }

    // ------------------------------------------------------------------ //
    //  CANCEL TRANSACTION                                                  //
    // ------------------------------------------------------------------ //

    @Transactional
    public TransactionResponse cancel(Long txId, CancelRequest req, String phone) {

        Transaction tx = transactionRepository.findById(txId)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found: " + txId));

        // Already cancelled?
        if (tx.getCancelledAt() != null) {
            throw new BusinessException("Cette transaction a déjà été annulée");
        }

        // 2-minute cancellation window
        LocalDateTime deadline = tx.getCreatedAt().plusMinutes(CANCEL_WINDOW_MINUTES);
        if (LocalDateTime.now().isAfter(deadline)) {
            throw new BusinessException(
                    "Le délai d'annulation de " + CANCEL_WINDOW_MINUTES + " minutes est dépassé");
        }

        // Restore subscription balance
        Client client = tx.getClient();
        if (tx.getPaymentMethod() == PaymentMethod.ABONNEMENT && client != null) {
            client.setBalance(client.getBalance() + 1);
            clientRepository.save(client);
        }

        // Reverse stock movements (create IN for each OUT linked to this transaction)
        reverseStockMovements(tx);

        // Mark as cancelled
        tx.setCancelledAt(LocalDateTime.now());
        tx.setCancelReason(req.reason());
        tx = transactionRepository.save(tx);

        notifyDashboard("transaction.cancelled", tx.getId());

        log.info("Transaction #{} cancelled by user={} — reason='{}'", txId, phone, req.reason());

        return toResponse(tx, client);
    }

    // ------------------------------------------------------------------ //
    //  TODAY'S TRANSACTIONS                                                //
    // ------------------------------------------------------------------ //

    @Transactional(readOnly = true)
    public List<TransactionResponse> findToday() {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime endOfDay   = startOfDay.plusDays(1);
        return transactionRepository
                .findByCreatedAtBetweenOrderByCreatedAtDesc(startOfDay, endOfDay)
                .stream()
                .map(tx -> toResponse(tx, tx.getClient()))
                .toList();
    }

    // ------------------------------------------------------------------ //
    //  PRIVATE HELPERS                                                     //
    // ------------------------------------------------------------------ //

    private void decrementStock(Map<String, Double> consumption, Transaction tx) {
        if (consumption == null || consumption.isEmpty()) return;

        consumption.forEach((productName, qty) -> {
            productRepository.findByNameIgnoreCase(productName).ifPresent(product -> {
                BigDecimal decrement = BigDecimal.valueOf(qty);
                BigDecimal newStock  = product.getStock().subtract(decrement);
                product.setStock(newStock.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : newStock);
                productRepository.save(product);

                stockMovementRepository.save(StockMovement.builder()
                        .product(product)
                        .quantity(decrement)
                        .type(StockMovement.MovementType.OUT)
                        .transaction(tx)
                        .build());

                if (product.getStock().compareTo(product.getAlertThreshold()) <= 0) {
                    log.warn("LOW STOCK — product='{}' stock={} threshold={}",
                            product.getName(), product.getStock(), product.getAlertThreshold());
                    notifyDashboard("stock.low", product.getId());
                }
            });
        });
    }

    private void reverseStockMovements(Transaction tx) {
        stockMovementRepository.findByTransactionId(tx.getId()).forEach(mv -> {
            Product product = mv.getProduct();
            product.setStock(product.getStock().add(mv.getQuantity()));
            productRepository.save(product);

            stockMovementRepository.save(StockMovement.builder()
                    .product(product)
                    .quantity(mv.getQuantity())
                    .type(StockMovement.MovementType.IN)
                    .transaction(tx)
                    .build());
        });
    }

    private void notifyDashboard(String event, Long id) {
        try {
            messagingTemplate.convertAndSend("/topic/dashboard", Map.of("event", event, "id", id));
        } catch (Exception e) {
            log.warn("WebSocket notification failed: {}", e.getMessage());
        }
    }

    private TransactionResponse toResponse(Transaction tx, Client client) {
        return new TransactionResponse(
                tx.getId(),
                tx.getService().getId(),
                tx.getService().getName(),
                tx.getService().getPrice(),
                client != null ? client.getId()      : null,
                client != null ? client.getName()    : null,
                client != null ? client.getBalance() : null,
                tx.getUser().getId(),
                tx.getUser().getName(),
                tx.getAmount(),
                tx.getPaymentMethod().name(),
                tx.getCreatedAt(),
                tx.getCancelledAt(),
                tx.getCancelReason()
        );
    }
}
