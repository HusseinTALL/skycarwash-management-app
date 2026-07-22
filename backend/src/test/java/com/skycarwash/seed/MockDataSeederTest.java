package com.skycarwash.seed;

import com.skycarwash.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Boots the application with the "seed" profile against an isolated H2 database
 * and verifies the seeder actually populated every table. Volumes are kept small
 * here to keep the test fast — production runs use the defaults.
 */
@SpringBootTest
@ActiveProfiles("seed")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:seedtest;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "seed.clients=15",
        "seed.transactions=60",
        "seed.expenses=10",
        "seed.months=3"
})
class MockDataSeederTest {

    @Autowired UserRepository userRepository;
    @Autowired ClientRepository clientRepository;
    @Autowired VehicleRepository vehicleRepository;
    @Autowired ServiceRepository serviceRepository;
    @Autowired ProductRepository productRepository;
    @Autowired TransactionRepository transactionRepository;
    @Autowired ExpenseRepository expenseRepository;

    @Test
    void seederPopulatesEveryTable() {
        assertThat(userRepository.count()).isGreaterThanOrEqualTo(6);
        assertThat(serviceRepository.count()).isEqualTo(7);
        assertThat(productRepository.count()).isEqualTo(5);
        assertThat(clientRepository.count()).isEqualTo(15);
        assertThat(vehicleRepository.count()).isPositive();
        assertThat(transactionRepository.count()).isEqualTo(60);
        assertThat(expenseRepository.count()).isEqualTo(10);
    }

    @Test
    void subscriptionTransactionsAlwaysHaveAClient() {
        boolean anyAbonnementWithoutClient = transactionRepository.findAll().stream()
                .anyMatch(t -> t.getPaymentMethod() == com.skycarwash.entity.Transaction.PaymentMethod.ABONNEMENT
                        && t.getClient() == null);
        assertThat(anyAbonnementWithoutClient).isFalse();
    }
}
