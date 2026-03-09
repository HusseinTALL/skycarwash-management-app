package com.skycarwash.scheduler;

import com.skycarwash.entity.Client;
import com.skycarwash.repository.ClientRepository;
import com.skycarwash.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Runs daily checks and fires notification alerts for:
 *  - Subscriptions expiring within 5 days (BOUCLIER / VIP with expiresAt set)
 *  - CARTE clients with ≤ 1 passage remaining
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlertScheduler {

    private static final int EXPIRY_WARN_DAYS   = 5;
    private static final int LOW_BALANCE_THRESHOLD = 1;

    private final ClientRepository    clientRepository;
    private final NotificationService notificationService;

    /** Daily at 08:00 — subscription expiry alerts. */
    @Scheduled(cron = "0 0 8 * * *")
    public void checkExpiringSubscriptions() {
        LocalDate today  = LocalDate.now();
        LocalDate cutoff = today.plusDays(EXPIRY_WARN_DAYS);

        List<Client> expiring = clientRepository
                .findByActiveTrueAndExpiresAtBetween(today, cutoff);

        if (expiring.isEmpty()) {
            log.debug("[scheduler] No expiring subscriptions today");
            return;
        }

        log.info("[scheduler] Found {} expiring subscription(s)", expiring.size());
        expiring.forEach(c -> {
            long daysLeft = ChronoUnit.DAYS.between(today, c.getExpiresAt());
            notificationService.sendExpiryAlert(c.getName(), c.getPhone(), daysLeft);
        });
    }

    /** Daily at 08:00 — low-balance CARTE alerts. */
    @Scheduled(cron = "0 0 8 * * *")
    public void checkLowBalanceClients() {
        List<Client> lowBalance = clientRepository
                .findByActiveTrueAndTypeAndBalanceLessThanEqual(
                        Client.ClientType.CARTE, LOW_BALANCE_THRESHOLD);

        if (lowBalance.isEmpty()) {
            log.debug("[scheduler] No low-balance CARTE clients today");
            return;
        }

        log.info("[scheduler] Found {} CARTE client(s) with low balance", lowBalance.size());
        lowBalance.forEach(c ->
                notificationService.sendLowBalanceAlert(c.getName(), c.getPhone(), c.getBalance()));
    }
}
