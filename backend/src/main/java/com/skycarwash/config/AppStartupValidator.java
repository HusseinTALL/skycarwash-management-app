package com.skycarwash.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates critical configuration at startup so the app fails fast with a
 * clear message rather than silently misbehaving in production.
 *
 * <p>Skipped in the "test" profile to keep unit/integration tests lightweight.
 */
@Slf4j
@Component
@Profile("!test")
public class AppStartupValidator {

    private static final String DEFAULT_JWT_SECRET =
            "change-me-in-production-must-be-at-least-256-bits-long-secret";

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${cors.allowed-origins}")
    private String corsOrigins;

    @PostConstruct
    public void validate() {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // ── JWT secret ────────────────────────────────────────────────────────
        if (DEFAULT_JWT_SECRET.equals(jwtSecret)) {
            warnings.add("JWT_SECRET is set to the default placeholder – set a strong secret in production!");
        }
        if (jwtSecret == null || jwtSecret.length() < 32) {
            errors.add("jwt.secret (JWT_SECRET) must be at least 32 characters (256 bits).");
        }

        // ── Database URL ──────────────────────────────────────────────────────
        if (datasourceUrl == null || datasourceUrl.isBlank()) {
            errors.add("DATABASE_URL must be set.");
        }

        // ── CORS origins ──────────────────────────────────────────────────────
        if (corsOrigins == null || corsOrigins.isBlank()) {
            errors.add("CORS_ORIGINS must contain at least one allowed origin.");
        } else if (corsOrigins.contains("localhost") && isLikelyProduction()) {
            warnings.add("CORS_ORIGINS contains 'localhost' – make sure this is intentional in production.");
        }

        // ── Report ────────────────────────────────────────────────────────────
        warnings.forEach(w -> log.warn("[CONFIG WARNING] {}", w));

        if (!errors.isEmpty()) {
            String msg = "[CONFIG ERROR] Application startup aborted due to misconfiguration:\n  • "
                    + String.join("\n  • ", errors);
            log.error(msg);
            throw new IllegalStateException(msg);
        }

        log.info("Application configuration validation passed.");
    }

    /** Heuristic: if DATABASE_URL points to Supabase/remote host, treat as production. */
    private boolean isLikelyProduction() {
        return datasourceUrl != null
                && !datasourceUrl.contains("localhost")
                && !datasourceUrl.contains("127.0.0.1");
    }
}
