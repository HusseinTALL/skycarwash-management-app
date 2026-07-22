package com.skycarwash.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Authentifie les jetons du portail client ({@code type=PORTAL}).
 * Le principal est le numéro de téléphone ; l'autorité est {@code ROLE_PORTAL}.
 */
@Component
@RequiredArgsConstructor
public class PortalAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String token = authHeader.substring(7);

        if (jwtUtil.isTokenValid(token)
                && "PORTAL".equals(jwtUtil.extractType(token))
                && SecurityContextHolder.getContext().getAuthentication() == null) {

            String phone = jwtUtil.extractPhone(token);
            var authToken = new UsernamePasswordAuthenticationToken(
                    phone, null, List.of(new SimpleGrantedAuthority("ROLE_PORTAL")));
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        filterChain.doFilter(request, response);
    }
}
