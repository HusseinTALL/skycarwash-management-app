package com.skycarwash.dto;

import java.time.YearMonth;
import java.util.List;

/**
 * Monthly dashboard payload.
 *
 * @param month              The requested month (YYYY-MM)
 * @param totalRevenue       Total revenue for the month
 * @param totalVehicles      Total active (non-cancelled) transactions
 * @param dailyCurve         Revenue per day — 30 data points for the line chart
 * @param topServices        Top 3 services by revenue
 * @param activeClients      Clients with balance > 0 or non-expired subscription
 * @param expiringIn7Days    Clients expiring within the next 7 days
 * @param estimatedProfit    Rough profit estimate (revenue × 40% margin)
 */
public record MonthlyDashboardDto(
        String month,
        int totalRevenue,
        int totalVehicles,
        List<DailyPoint> dailyCurve,
        List<ServiceRevenue> topServices,
        long activeClients,
        long expiringIn7Days,
        int estimatedProfit
) {
    /** One data point for the 30-day revenue chart. */
    public record DailyPoint(String date, int revenue, int vehicles) {}

    /** A service and its revenue + wash count for the month. */
    public record ServiceRevenue(String name, int revenue, int count) {}
}
