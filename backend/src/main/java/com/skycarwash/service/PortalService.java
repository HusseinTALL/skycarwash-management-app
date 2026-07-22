package com.skycarwash.service;

import com.skycarwash.dto.PortalLoginRequest;
import com.skycarwash.dto.PortalLoginResponse;
import com.skycarwash.entity.PortalCustomer;
import com.skycarwash.exception.BusinessException;
import com.skycarwash.repository.PortalCustomerRepository;
import com.skycarwash.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortalService {

    private final PortalCustomerRepository portalCustomerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    private static final String BAD_CREDS = "Numéro ou code incorrect";

    @Transactional(readOnly = true)
    public PortalLoginResponse login(PortalLoginRequest req) {
        String phone = normalizePhone(req.phone());
        PortalCustomer customer = portalCustomerRepository.findByPhone(phone)
                .orElseThrow(() -> new BusinessException(BAD_CREDS, HttpStatus.UNAUTHORIZED));

        if (!passwordEncoder.matches(req.code(), customer.getAccessCodeHash())) {
            throw new BusinessException(BAD_CREDS, HttpStatus.UNAUTHORIZED);
        }

        String token = jwtUtil.generatePortalToken(phone);
        return new PortalLoginResponse(token, phone, customer.isMustChangeCode());
    }

    @Transactional
    public void changeCode(String phone, String newCode) {
        PortalCustomer customer = portalCustomerRepository.findByPhone(normalizePhone(phone))
                .orElseThrow(() -> new BusinessException("Compte introuvable", HttpStatus.UNAUTHORIZED));
        customer.setAccessCodeHash(passwordEncoder.encode(newCode));
        customer.setMustChangeCode(false);
        customer.setUpdatedAt(LocalDateTime.now());
        portalCustomerRepository.save(customer);
        log.info("Portal code changed for {}", customer.getPhone());
    }

    private static String normalizePhone(String phone) {
        return phone == null ? null : phone.replaceAll("\\s+", "");
    }
}
