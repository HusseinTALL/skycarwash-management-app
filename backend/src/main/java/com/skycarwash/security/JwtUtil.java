package com.skycarwash.security;

import com.skycarwash.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey key;

    @Value("${jwt.expiration.manager}")
    private long managerExpirationSec;

    @Value("${jwt.expiration.employee}")
    private long employeeExpirationSec;

    @Value("${jwt.expiration.partner}")
    private long partnerExpirationSec;

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        // Always use UTF-8 so key bytes are identical on every platform/container
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            keyBytes = padded;
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(User user) {
        long expirationSec = switch (user.getRole()) {
            case MANAGER  -> managerExpirationSec;
            case PARTNER  -> partnerExpirationSec;
            case EMPLOYEE -> employeeExpirationSec;
        };

        return Jwts.builder()
                .subject(user.getPhone())
                .claim("role", user.getRole().name())
                .claim("userId", user.getId())
                .claim("name", user.getName())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationSec * 1000))
                .signWith(key)
                .compact();
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractPhone(String token) {
        return extractAllClaims(token).getSubject();
    }

    public boolean isTokenValid(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
