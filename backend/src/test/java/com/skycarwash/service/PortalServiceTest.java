package com.skycarwash.service;

import com.skycarwash.dto.PortalLoginRequest;
import com.skycarwash.dto.PortalLoginResponse;
import com.skycarwash.entity.PortalCustomer;
import com.skycarwash.exception.BusinessException;
import com.skycarwash.repository.PortalCustomerRepository;
import com.skycarwash.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PortalServiceTest {

    private PortalCustomerRepository repo;
    private PasswordEncoder encoder;
    private JwtUtil jwtUtil;
    private PortalService service;

    @BeforeEach
    void setUp() {
        repo = mock(PortalCustomerRepository.class);
        encoder = new BCryptPasswordEncoder(4); // low cost for tests
        jwtUtil = mock(JwtUtil.class);
        service = new PortalService(repo, encoder, jwtUtil);
    }

    private PortalCustomer customer(String phone, String code, boolean mustChange) {
        return PortalCustomer.builder()
                .id(1L).phone(phone)
                .accessCodeHash(encoder.encode(code))
                .mustChangeCode(mustChange)
                .build();
    }

    @Test
    void login_withCorrectCode_returnsTokenAndMustChangeFlag() {
        when(repo.findByPhone("70123456")).thenReturn(Optional.of(customer("70123456", "3456", true)));
        when(jwtUtil.generatePortalToken("70123456")).thenReturn("portal-token");

        PortalLoginResponse res = service.login(new PortalLoginRequest("70 12 34 56", "3456"));

        assertThat(res.token()).isEqualTo("portal-token");
        assertThat(res.phone()).isEqualTo("70123456");
        assertThat(res.mustChangeCode()).isTrue();
    }

    @Test
    void login_withWrongCode_isUnauthorized() {
        when(repo.findByPhone("70123456")).thenReturn(Optional.of(customer("70123456", "3456", true)));

        assertThatThrownBy(() -> service.login(new PortalLoginRequest("70123456", "0000")))
                .isInstanceOf(BusinessException.class);
        verify(jwtUtil, never()).generatePortalToken(any());
    }

    @Test
    void login_withUnknownPhone_isUnauthorized() {
        when(repo.findByPhone("99999999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new PortalLoginRequest("99999999", "1234")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void changeCode_updatesHashAndClearsMustChangeFlag() {
        PortalCustomer c = customer("70123456", "3456", true);
        when(repo.findByPhone("70123456")).thenReturn(Optional.of(c));

        service.changeCode("70123456", "9911");

        assertThat(c.isMustChangeCode()).isFalse();
        assertThat(encoder.matches("9911", c.getAccessCodeHash())).isTrue();
        assertThat(encoder.matches("3456", c.getAccessCodeHash())).isFalse();
        verify(repo).save(c);
    }
}
