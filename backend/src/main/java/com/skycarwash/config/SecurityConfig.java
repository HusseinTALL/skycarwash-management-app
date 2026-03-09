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
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
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
                        // Transactions – employee + manager can create/cancel; manager can read today
                        .requestMatchers(HttpMethod.POST, "/api/transactions/**").hasAnyRole("EMPLOYEE", "MANAGER")
                        .requestMatchers(HttpMethod.GET,  "/api/transactions/today").hasAnyRole("MANAGER", "PARTNER")
                        // Dashboard – manager + partner
                        .requestMatchers("/api/dashboard/**").hasAnyRole("MANAGER", "PARTNER")
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
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
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
