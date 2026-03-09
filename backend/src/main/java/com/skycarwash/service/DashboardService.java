package com.skycarwash.service;

import com.skycarwash.dto.DailyDashboardDto;
import com.skycarwash.dto.MonthlyDashboardDto;
import com.skycarwash.dto.MonthlyDashboardDto.DailyPoint;
import com.skycarwash.dto.MonthlyDashboardDto.ServiceRevenue;
import com.skycarwash.dto.TransactionResponse;
import com.skycarwash.entity.Client;
import com.skycarwash.entity.Transaction;
import com.skycarwash.repository.ClientRepository;
import com.skycarwash.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final double PROFIT_MARGIN = 0.40;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final TransactionRepository transactionRepository;
    private final ClientRepository      clientRepository;

    // ── Daily ─────────────────────────────────────────────────────────── //

    @Transactional(readOnly = true)
    public DailyDashboardDto getDaily(LocalDate date) {
        LocalDateTime dayStart  = date.atStartOfDay();
        LocalDateTime dayEnd    = dayStart.plusDays(1);

        // Current day active transactions
        List<Transaction> txs = transactionRepository.findActiveTransactionsBetween(dayStart, dayEnd);

        int vehiclesWashed = txs.size();
        int totalRevenue   = txs.stream().mapToInt(Transaction::getAmount).sum();

        Map<String, Integer> revenueByMethod = txs.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getPaymentMethod().name(),
                        Collectors.summingInt(Transaction::getAmount)));

        Map<String, Integer> revenueByService = txs.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getService().getName(),
                        Collectors.summingInt(Transaction::getAmount)));

        // Same weekday last week
        LocalDateTime lastWeekStart = dayStart.minusWeeks(1);
        LocalDateTime lastWeekEnd   = dayEnd.minusWeeks(1);
        List<Transaction> lastWeekTxs = transactionRepository
                .findActiveTransactionsBetween(lastWeekStart, lastWeekEnd);

        int lastWeekVehicles = lastWeekTxs.size();
        int lastWeekRevenue  = lastWeekTxs.stream().mapToInt(Transaction::getAmount).sum();

        // Recent 10 transactions (including cancelled) for the activity feed
        List<TransactionResponse> recent = transactionRepository
                .findByCreatedAtBetweenOrderByCreatedAtDesc(dayStart, dayEnd)
                .stream()
                .limit(10)
                .map(this::toResponse)
                .toList();

        return new DailyDashboardDto(
                date, vehiclesWashed, totalRevenue,
                revenueByMethod, revenueByService,
                lastWeekVehicles, lastWeekRevenue,
                totalRevenue - lastWeekRevenue,
                vehiclesWashed - lastWeekVehicles,
                recent
        );
    }

    // ── Monthly ───────────────────────────────────────────────────────── //

    @Transactional(readOnly = true)
    public MonthlyDashboardDto getMonthly(YearMonth yearMonth) {
        LocalDateTime monthStart = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime monthEnd   = yearMonth.atEndOfMonth().plusDays(1).atStartOfDay();

        List<Transaction> txs = transactionRepository
                .findActiveTransactionsBetween(monthStart, monthEnd);

        int totalRevenue  = txs.stream().mapToInt(Transaction::getAmount).sum();
        int totalVehicles = txs.size();

        // Daily curve — one entry per day of the month
        Map<LocalDate, List<Transaction>> byDay = txs.stream()
                .collect(Collectors.groupingBy(t -> t.getCreatedAt().toLocalDate()));

        List<DailyPoint> dailyCurve = yearMonth.atDay(1).datesUntil(yearMonth.atEndOfMonth().plusDays(1))
                .map(day -> {
                    List<Transaction> dayTxs = byDay.getOrDefault(day, List.of());
                    return new DailyPoint(
                            day.format(DATE_FMT),
                            dayTxs.stream().mapToInt(Transaction::getAmount).sum(),
                            dayTxs.size()
                    );
                })
                .toList();

        // Top 3 services by revenue
        List<ServiceRevenue> topServices = txs.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getService().getName(),
                        Collectors.toList()))
                .entrySet().stream()
                .map(e -> new ServiceRevenue(
                        e.getKey(),
                        e.getValue().stream().mapToInt(Transaction::getAmount).sum(),
                        e.getValue().size()))
                .sorted(Comparator.comparingInt(ServiceRevenue::revenue).reversed())
                .limit(3)
                .toList();

        // Client stats
        LocalDate today   = LocalDate.now();
        LocalDate in7Days = today.plusDays(7);

        long activeClients = clientRepository.findAllByActiveTrueOrderByNameAsc().stream()
                .filter(c -> c.getBalance() > 0 ||
                        (c.getExpiresAt() != null && !c.getExpiresAt().isBefore(today)))
                .count();

        long expiringIn7Days = clientRepository
                .findByActiveTrueAndExpiresAtBetween(today, in7Days)
                .size();

        int estimatedProfit = (int) (totalRevenue * PROFIT_MARGIN);

        return new MonthlyDashboardDto(
                yearMonth.toString(),
                totalRevenue, totalVehicles,
                dailyCurve, topServices,
                activeClients, expiringIn7Days,
                estimatedProfit
        );
    }

    // ── Private mapping ───────────────────────────────────────────────── //

    private TransactionResponse toResponse(Transaction tx) {
        Client client = tx.getClient();
        return new TransactionResponse(
                tx.getId(),
                tx.getService().getId(), tx.getService().getName(), tx.getService().getPrice(),
                client != null ? client.getId()      : null,
                client != null ? client.getName()    : null,
                client != null ? client.getBalance() : null,
                tx.getUser().getId(), tx.getUser().getName(),
                tx.getAmount(), tx.getPaymentMethod().name(),
                tx.getCreatedAt(), tx.getCancelledAt(), tx.getCancelReason()
        );
    }
}
