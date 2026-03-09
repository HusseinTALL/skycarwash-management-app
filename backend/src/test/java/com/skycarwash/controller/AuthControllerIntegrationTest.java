package com.skycarwash.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skycarwash.dto.LoginRequest;
import com.skycarwash.entity.User;
import com.skycarwash.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired MockMvc       mockMvc;
    @Autowired ObjectMapper  objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private static final String PHONE    = "70000099";
    private static final String PASSWORD = "secret123";

    @BeforeEach
    void seedUser() {
        if (userRepository.findByPhone(PHONE).isEmpty()) {
            userRepository.save(User.builder()
                    .name("Test Manager")
                    .phone(PHONE)
                    .role(User.Role.MANAGER)
                    .passwordHash(passwordEncoder.encode(PASSWORD))
                    .build());
        }
    }

    // ── Successful login ─────────────────────────────────────────────── //

    @Test
    void login_validCredentials_returns200WithToken() throws Exception {
        LoginRequest body = new LoginRequest(PHONE, PASSWORD);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("MANAGER"))
                .andExpect(jsonPath("$.name").value("Test Manager"));
    }

    // ── Wrong password ───────────────────────────────────────────────── //

    @Test
    void login_wrongPassword_returns401() throws Exception {
        LoginRequest body = new LoginRequest(PHONE, "wrong-password");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").exists());
    }

    // ── Unknown user ─────────────────────────────────────────────────── //

    @Test
    void login_unknownPhone_returns401() throws Exception {
        LoginRequest body = new LoginRequest("00000000", PASSWORD);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }

    // ── Missing fields (validation) ──────────────────────────────────── //

    @Test
    void login_missingPhone_returns400() throws Exception {
        String json = "{\"password\":\"abc\"}";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_missingPassword_returns400() throws Exception {
        String json = "{\"phone\":\"70000099\"}";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }
}
