package com.skycarwash.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Daily dashboard payload.
 *
 * @param date               The requested date
 * @param vehiclesWashed     Active (non-cancelled) transactions for the day
 * @param totalRevenue       Sum of transaction amounts (FCFA)
 * @param revenueByMethod    CASH / ORANGE / MOOV / ABONNEMENT → FCFA
 * @param revenueByService   Service name → FCFA
 * @param lastWeekVehicles   Same weekday last week — for comparison
 * @param lastWeekRevenue    Same weekday last week revenue
 * @param revenueDelta       totalRevenue - lastWeekRevenue (can be negative)
 * @param vehiclesDelta      vehiclesWashed - lastWeekVehicles
 * @param recentTransactions Last 10 transactions (for activity feed)
 * @param cancelledCount     Number of cancelled transactions for the day
 * @param cancelledAmount    Sum of cancelled transaction amounts (for closing report)
 */
public record DailyDashboardDto(
        LocalDate date,
        int vehiclesWashed,
        int totalRevenue,
        Map<String, Integer> revenueByMethod,
        Map<String, Integer> revenueByService,
        int lastWeekVehicles,
        int lastWeekRevenue,
        int revenueDelta,
        int vehiclesDelta,
        List<TransactionResponse> recentTransactions,
        int cancelledCount,
        int cancelledAmount
) {}
