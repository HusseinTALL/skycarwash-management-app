package com.skycarwash.config;

import com.skycarwash.security.JwtAuthFilter;
import com.skycarwash.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;

import java.util.List;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsServiceImpl userDetailsService;

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // ── Security response headers ──────────────────────────────────────
                .headers(headers -> headers
                        // X-Frame-Options: DENY
                        .frameOptions(fo -> fo.deny())
                        // X-Content-Type-Options: nosniff  (explicit lambda, not deprecated ::and ref)
                        .contentTypeOptions(co -> { /* enable with defaults */ })
                        // HSTS – only sent over HTTPS connections (correct per spec)
                        .httpStrictTransportSecurity(hsts -> hsts
                                .maxAgeInSeconds(31536000)
                                .includeSubDomains(true))
                        .referrerPolicy(rp -> rp
                                .policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .permissionsPolicy(pp -> pp
                                .policy("camera=(), microphone=(), geolocation=(), payment=()")))
                .authorizeHttpRequests(auth -> auth
                        // CORS preflight must always pass through without auth checks
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        // Actuator health probes (Railway / Render / k8s)
                        .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                        // Swagger UI – restrict to authenticated users
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").authenticated()
                        // WebSocket endpoint
                        .requestMatchers("/ws/**").permitAll()
                        // Service CRUD – manager only
                        .requestMatchers(HttpMethod.POST, "/api/services/**").hasRole("MANAGER")
                        .requestMatchers(HttpMethod.PUT,  "/api/services/**").hasRole("MANAGER")
                        .requestMatchers(HttpMethod.DELETE, "/api/services/**").hasRole("MANAGER")
                        .requestMatchers(HttpMethod.GET, "/api/services/**").authenticated()
                        // Product CRUD – manager only
                        .requestMatchers("/api/products/**").hasRole("MANAGER")
                        // Clients – search open to EMPLOYEE (caisse); full CRUD to MANAGER
                        .requestMatchers(HttpMethod.GET,    "/api/clients/search").hasAnyRole("EMPLOYEE", "MANAGER")
                        .requestMatchers(HttpMethod.GET,    "/api/clients/**").hasAnyRole("MANAGER", "PARTNER")
                        .requestMatchers(HttpMethod.POST,   "/api/clients/**").hasRole("MANAGER")
                        .requestMatchers(HttpMethod.PUT,    "/api/clients/**").hasRole("MANAGER")
                        .requestMatchers(HttpMethod.DELETE, "/api/clients/**").hasRole("MANAGER")
                        // Transactions – employee + manager can create/cancel; manager/partner can read
                        .requestMatchers(HttpMethod.POST, "/api/transactions/**").hasAnyRole("EMPLOYEE", "MANAGER")
                        .requestMatchers(HttpMethod.GET,  "/api/transactions/today").hasAnyRole("MANAGER", "PARTNER")
                        .requestMatchers(HttpMethod.GET,  "/api/transactions/history").hasAnyRole("MANAGER", "PARTNER")
                        // Dashboard – manager + partner
                        .requestMatchers("/api/dashboard/**").hasAnyRole("MANAGER", "PARTNER")
                        // User management – manager only
                        .requestMatchers("/api/manager/**").hasRole("MANAGER")
                        // Everything else requires authentication
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
