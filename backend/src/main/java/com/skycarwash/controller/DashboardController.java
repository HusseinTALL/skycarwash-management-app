package com.skycarwash.controller;

import com.skycarwash.dto.DailyDashboardDto;
import com.skycarwash.dto.MonthlyDashboardDto;
import com.skycarwash.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * Daily metrics.
     * @param date ISO date string (yyyy-MM-dd). Defaults to today.
     */
    @GetMapping("/daily")
    public ResponseEntity<DailyDashboardDto> daily(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        LocalDate target = date != null ? date : LocalDate.now();
        return ResponseEntity.ok(dashboardService.getDaily(target));
    }

    /**
     * Monthly metrics.
     * @param month YYYY-MM string. Defaults to current month.
     */
    @GetMapping("/monthly")
    public ResponseEntity<MonthlyDashboardDto> monthly(
            @RequestParam(required = false) String month) {

        YearMonth target;
        try {
            target = month != null ? YearMonth.parse(month) : YearMonth.now();
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(dashboardService.getMonthly(target));
    }
}
