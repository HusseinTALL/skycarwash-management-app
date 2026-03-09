package com.skycarwash.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Token-bucket rate limiting filter.
 *
 * <p>Limits:
 * <ul>
 *   <li>/api/auth/** – 10 requests per minute per IP (brute-force protection)</li>
 *   <li>All other /api/** – 120 requests per minute per IP</li>
 * </ul>
 */
@Slf4j
@Component
@Order(1)
public class RateLimitFilter extends OncePerRequestFilter {

    // IP → Bucket for auth endpoints (strict)
    private final ConcurrentHashMap<String, Bucket> authBuckets = new ConcurrentHashMap<>();
    // IP → Bucket for general API endpoints
    private final ConcurrentHashMap<String, Bucket> apiBuckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI();

        // Only rate-limit API calls
        if (!path.startsWith("/api/")) {
            chain.doFilter(request, response);
            return;
        }

        String clientIp = resolveClientIp(request);
        Bucket bucket = path.startsWith("/api/auth/")
                ? authBuckets.computeIfAbsent(clientIp, k -> buildAuthBucket())
                : apiBuckets.computeIfAbsent(clientIp, k -> buildApiBucket());

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded for IP={} path={}", clientIp, path);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Too many requests – please slow down.\"}");
        }
    }

    // 10 requests per minute (auth – brute-force guard)
    private Bucket buildAuthBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(10, Refill.greedy(10, Duration.ofMinutes(1))))
                .build();
    }

    // 120 requests per minute (general API)
    private Bucket buildApiBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(120, Refill.greedy(120, Duration.ofMinutes(1))))
                .build();
    }

    /** Resolve real client IP behind proxies (Railway, Render, Vercel, etc.). */
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
