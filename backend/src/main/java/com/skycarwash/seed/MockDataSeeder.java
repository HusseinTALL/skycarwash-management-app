package com.skycarwash.seed;

import com.skycarwash.entity.*;
import com.skycarwash.entity.Client.ClientType;
import com.skycarwash.entity.Transaction.PaymentMethod;
import com.skycarwash.entity.User.Role;
import com.skycarwash.entity.Vehicle.VehicleType;
import com.skycarwash.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Fills the database with a large, realistic demo dataset — clients, vehicles,
 * services, products, months of transactions and expenses.
 *
 * <p><b>Only runs when the "seed" profile is active</b>, so it never touches a
 * normal or production startup. Enable it for a one-off run with:
 * <pre>SPRING_PROFILES_ACTIVE=seed ./mvnw spring-boot:run</pre>
 * (or add {@code seed} to the existing active profiles, e.g. {@code prod,seed}).
 *
 * <p>It is guarded: if the database already holds more than a handful of clients
 * it skips, unless {@code seed.force=true} is set.
 */
@Slf4j
@Component
@Profile("seed")
@RequiredArgsConstructor
public class MockDataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final VehicleRepository vehicleRepository;
    private final ServiceRepository serviceRepository;
    private final ProductRepository productRepository;
    private final TransactionRepository transactionRepository;
    private final ExpenseRepository expenseRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${seed.clients:200}")      private int nClients;
    @Value("${seed.transactions:5000}") private int nTransactions;
    @Value("${seed.expenses:150}")     private int nExpenses;
    @Value("${seed.months:6}")         private int months;
    @Value("${seed.force:false}")      private boolean force;

    private final Random rnd = new Random(42);

    private static final String[] FIRST = {
            "Adama", "Aïcha", "Ali", "Aminata", "Awa", "Boureima", "Djeneba", "Fatou",
            "Hamidou", "Ibrahim", "Issouf", "Karim", "Mariam", "Moussa", "Oumar", "Rasmané",
            "Safiatou", "Salif", "Seydou", "Yacouba", "Zalissa", "Nafissatou", "Idrissa", "Ousséni"
    };
    private static final String[] LAST = {
            "Ouédraogo", "Sawadogo", "Traoré", "Kaboré", "Compaoré", "Zongo", "Kondé",
            "Nikiéma", "Sanogo", "Diallo", "Bationo", "Ilboudo", "Congo", "Tapsoba", "Sankara"
    };
    private static final String[] TAG_POOL = {
            "fidèle", "flotte", "entreprise", "taxi", "particulier", "vip", "premium", "nouveau"
    };
    private static final String[] CAR_MODELS = {
            "Toyota Corolla", "Toyota Hilux", "Mercedes Classe C", "Hyundai Tucson", "Peugeot 308",
            "Nissan Patrol", "Kia Sportage", "Ford Ranger", "Renault Duster", "Toyota RAV4", "Suzuki Alto"
    };
    private static final String[] NOTES_POOL = {
            "Préfère le lavage à la main.", "Client de longue date, très ponctuel.",
            "Sensible aux rayures — attention au vernis.", "Paie souvent par Orange Money.",
            "Gère une petite flotte de taxis.", "Aime un habitacle parfumé."
    };

    @Override
    @Transactional
    public void run(String... args) {
        long existing = clientRepository.count();
        if (existing > 20 && !force) {
            log.info("[seed] Skipped — {} clients already present (set seed.force=true to seed anyway).", existing);
            return;
        }
        log.info("[seed] Seeding demo data: {} clients, {} transactions, {} expenses over {} months…",
                nClients, nTransactions, nExpenses, months);

        List<User> staff = seedStaff();
        List<ServiceEntity> services = seedServices();
        seedProducts();
        List<Client> clients = seedClients();
        seedVehicles(clients);
        seedTransactions(staff, services, clients);
        seedExpenses(staff);

        log.info("[seed] Done. users={}, services={}, clients={}, vehicles={}, transactions={}, expenses={}",
                userRepository.count(), serviceRepository.count(), clientRepository.count(),
                vehicleRepository.count(), transactionRepository.count(), expenseRepository.count());
    }

    // ── Staff ────────────────────────────────────────────────────────── //

    private List<User> seedStaff() {
        String hash = passwordEncoder.encode("Demo1234!");
        List<User> staff = new ArrayList<>();
        staff.add(user("Awa Compaoré", "+22670000001", Role.PARTNER, hash));
        staff.add(user("Salif Ouédraogo", "+22670000002", Role.MANAGER, hash));
        staff.add(user("Karim Sawadogo", "+22670000003", Role.EMPLOYEE, hash));
        staff.add(user("Fatou Traoré", "+22670000004", Role.EMPLOYEE, hash));
        staff.add(user("Ibrahim Kaboré", "+22670000005", Role.EMPLOYEE, hash));
        staff.add(user("Mariam Zongo", "+22670000006", Role.EMPLOYEE, hash));
        return userRepository.saveAll(staff);
    }

    private User user(String name, String phone, Role role, String hash) {
        return User.builder().name(name).phone(phone).role(role).passwordHash(hash).active(true).build();
    }

    // ── Services ─────────────────────────────────────────────────────── //

    private List<ServiceEntity> seedServices() {
        List<ServiceEntity> services = List.of(
                service("Lavage moto", 1000, "LAVAGE", Map.of("shampoo_l", 0.03)),
                service("Lavage extérieur", 2000, "LAVAGE", Map.of("shampoo_l", 0.05)),
                service("Lavage intérieur", 2500, "LAVAGE", Map.of()),
                service("Lavage complet", 4000, "LAVAGE", Map.of("shampoo_l", 0.08)),
                service("Aspiration", 1500, "LAVAGE", Map.of()),
                service("Polissage / Brillance", 15000, "PREMIUM", Map.of("wax_g", 30.0)),
                service("Protection / Cire", 10000, "PREMIUM", Map.of("wax_g", 50.0))
        );
        return serviceRepository.saveAll(services);
    }

    private ServiceEntity service(String name, int price, String category, Map<String, Double> consumption) {
        return ServiceEntity.builder()
                .name(name).price(price).category(category)
                .productConsumption(consumption).active(true).build();
    }

    // ── Products ─────────────────────────────────────────────────────── //

    private void seedProducts() {
        productRepository.saveAll(List.of(
                product("Shampoing auto", "80", "15", "L"),
                product("Cire / Wax", "12.5", "3", "kg"),
                product("Microfibre", "60", "10", "unité"),
                product("Produit vitres", "25", "5", "L"),
                product("Désodorisant", "40", "8", "unité")
        ));
    }

    private Product product(String name, String stock, String alert, String unit) {
        return Product.builder()
                .name(name).stock(new BigDecimal(stock))
                .alertThreshold(new BigDecimal(alert)).unit(unit).active(true).build();
    }

    // ── Clients ──────────────────────────────────────────────────────── //

    private List<Client> seedClients() {
        List<Client> clients = new ArrayList<>();
        for (int i = 0; i < nClients; i++) {
            ClientType type = weighted(ClientType.CARTE, 55, ClientType.BOUCLIER, 25, ClientType.VIP, 20);
            LocalDate expires = switch (type) {
                case BOUCLIER -> LocalDate.now().plusDays(rnd.nextInt(60) - 15);      // some already expired
                case VIP -> LocalDate.now().plusMonths(rnd.nextInt(12));
                case CARTE -> null;
            };
            int balance = type == ClientType.CARTE ? rnd.nextInt(21)
                    : type == ClientType.VIP ? rnd.nextInt(30) : 0;

            clients.add(Client.builder()
                    .name(pick(FIRST) + " " + pick(LAST))
                    .phone("+226" + (70_100_000 + i))
                    .type(type)
                    .balance(balance)
                    .expiresAt(expires)
                    .tags(randomTags())
                    .notes(rnd.nextInt(3) == 0 ? pick(NOTES_POOL) : null)
                    .active(rnd.nextInt(10) != 0)   // ~10% inactive
                    .createdAt(backdate(rnd.nextInt(months * 30 + 60)))
                    .build());
        }
        return clientRepository.saveAll(clients);
    }

    private String randomTags() {
        int n = rnd.nextInt(3);   // 0–2 tags
        if (n == 0) return null;
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        while (tags.size() < n) tags.add(pick(TAG_POOL));
        return String.join(",", tags);
    }

    // ── Vehicles ─────────────────────────────────────────────────────── //

    private void seedVehicles(List<Client> clients) {
        List<Vehicle> vehicles = new ArrayList<>();
        for (Client c : clients) {
            if (rnd.nextInt(10) < 3) continue;          // ~30% have no registered vehicle
            int count = 1 + rnd.nextInt(3);             // 1–3 vehicles
            for (int i = 0; i < count; i++) {
                VehicleType type = weighted(
                        VehicleType.VOITURE, 50, VehicleType.SUV, 25,
                        VehicleType.MOTO, 15, VehicleType.PICKUP, 10);
                vehicles.add(Vehicle.builder()
                        .client(c)
                        .type(type)
                        .plate(randomPlate())
                        .label(rnd.nextInt(3) == 0 ? null : pick(CAR_MODELS))
                        .build());
            }
        }
        vehicleRepository.saveAll(vehicles);
    }

    private String randomPlate() {
        char l1 = (char) ('A' + rnd.nextInt(26));
        char l2 = (char) ('A' + rnd.nextInt(26));
        return String.format("%02d %c%c %04d", 11 + rnd.nextInt(89), l1, l2, 1000 + rnd.nextInt(9000));
    }

    // ── Transactions ─────────────────────────────────────────────────── //

    private void seedTransactions(List<User> staff, List<ServiceEntity> services, List<Client> clients) {
        List<User> operators = staff.stream()
                .filter(u -> u.getRole() != Role.PARTNER).toList();
        List<Client> subscribers = clients.stream()
                .filter(Client::isActive).toList();

        List<Transaction> batch = new ArrayList<>();
        for (int i = 0; i < nTransactions; i++) {
            ServiceEntity svc = weightedService(services);
            PaymentMethod method = weighted(
                    PaymentMethod.CASH, 50, PaymentMethod.ORANGE, 20,
                    PaymentMethod.MOOV, 15, PaymentMethod.ABONNEMENT, 15);

            Client client = null;
            if (method == PaymentMethod.ABONNEMENT) {
                client = pick(subscribers);
            } else if (rnd.nextInt(10) < 4) {           // ~40% of cash/mobile are known clients
                client = pick(subscribers);
            }

            LocalDateTime when = backdate(rnd.nextInt(months * 30))
                    .withHour(7 + rnd.nextInt(13)).withMinute(rnd.nextInt(60));

            Transaction tx = Transaction.builder()
                    .service(svc)
                    .client(client)
                    .user(pick(operators))
                    .amount(svc.getPrice())
                    .paymentMethod(method)
                    .createdAt(when)
                    .build();

            if (rnd.nextInt(100) < 3) {                 // ~3% cancelled
                tx.setCancelledAt(when.plusMinutes(1 + rnd.nextInt(30)));
                tx.setCancelReason("Erreur de saisie");
            }
            batch.add(tx);

            if (batch.size() >= 500) {                  // flush in chunks
                transactionRepository.saveAll(batch);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) transactionRepository.saveAll(batch);
    }

    private ServiceEntity weightedService(List<ServiceEntity> services) {
        // Favour everyday washes over premium care
        ServiceEntity premiumOrWash = rnd.nextInt(100) < 12
                ? services.get(5 + rnd.nextInt(2))      // premium (indices 5,6)
                : services.get(rnd.nextInt(5));         // washes (indices 0–4)
        return premiumOrWash;
    }

    // ── Expenses ─────────────────────────────────────────────────────── //

    private void seedExpenses(List<User> staff) {
        User manager = staff.stream()
                .filter(u -> u.getRole() == Role.MANAGER).findFirst().orElse(staff.get(0));

        List<Expense> expenses = new ArrayList<>();
        for (int i = 0; i < nExpenses; i++) {
            Expense.Category cat = weighted(
                    Expense.Category.PRODUIT, 45, Expense.Category.SALAIRE, 30, Expense.Category.AUTRE, 25);
            int amount = switch (cat) {
                case SALAIRE -> 40_000 + rnd.nextInt(80) * 1000;
                case PRODUIT -> 5_000 + rnd.nextInt(45) * 1000;
                case AUTRE -> 2_000 + rnd.nextInt(30) * 1000;
            };
            expenses.add(Expense.builder()
                    .category(cat)
                    .amount(amount)
                    .description(switch (cat) {
                        case PRODUIT -> "Réassort produits";
                        case SALAIRE -> "Salaire employé";
                        case AUTRE -> "Frais divers";
                    })
                    .expenseDate(backdate(rnd.nextInt(months * 30)).toLocalDate())
                    .createdBy(manager)
                    .build());
        }
        expenseRepository.saveAll(expenses);
    }

    // ── Helpers ──────────────────────────────────────────────────────── //

    private LocalDateTime backdate(int daysAgo) {
        return LocalDateTime.now().minusDays(daysAgo);
    }

    private <T> T pick(T[] arr) {
        return arr[rnd.nextInt(arr.length)];
    }

    private <T> T pick(List<T> list) {
        return list.get(rnd.nextInt(list.size()));
    }

    /** Weighted random choice: pass value, weight, value, weight, … */
    @SuppressWarnings("unchecked")
    private <T> T weighted(Object... valueWeightPairs) {
        int total = 0;
        for (int i = 1; i < valueWeightPairs.length; i += 2) total += (int) valueWeightPairs[i];
        int roll = rnd.nextInt(total);
        int cursor = 0;
        for (int i = 0; i < valueWeightPairs.length; i += 2) {
            cursor += (int) valueWeightPairs[i + 1];
            if (roll < cursor) return (T) valueWeightPairs[i];
        }
        return (T) valueWeightPairs[0];
    }
}
