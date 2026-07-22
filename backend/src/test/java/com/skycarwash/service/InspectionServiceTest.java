package com.skycarwash.service;

import com.skycarwash.entity.PortalCustomer;
import com.skycarwash.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InspectionServiceTest {

    private PortalCustomerRepository portalRepo;
    private PasswordEncoder encoder;
    private InspectionService service;

    @BeforeEach
    void setUp() {
        portalRepo = mock(PortalCustomerRepository.class);
        encoder = new BCryptPasswordEncoder(4);
        service = new InspectionService(
                mock(InspectionReportRepository.class),
                mock(InspectionPhotoRepository.class),
                mock(InspectionDamageRepository.class),
                mock(InspectionFoundItemRepository.class),
                portalRepo,
                mock(TransactionRepository.class),
                mock(ClientRepository.class),
                mock(UserRepository.class),
                encoder
        );
    }

    @Test
    void provisionPortalAccess_defaultCodeIsLast4Digits() {
        when(portalRepo.findByPhone("70123456")).thenReturn(Optional.empty());

        service.provisionPortalAccess("70 12 34 56");

        ArgumentCaptor<PortalCustomer> captor = ArgumentCaptor.forClass(PortalCustomer.class);
        verify(portalRepo).save(captor.capture());
        PortalCustomer saved = captor.getValue();

        assertThat(saved.getPhone()).isEqualTo("70123456");
        assertThat(saved.isMustChangeCode()).isTrue();
        assertThat(encoder.matches("3456", saved.getAccessCodeHash())).isTrue();
        assertThat(encoder.matches("2345", saved.getAccessCodeHash())).isFalse();
    }

    @Test
    void provisionPortalAccess_doesNothingWhenAlreadyExists() {
        when(portalRepo.findByPhone("70123456"))
                .thenReturn(Optional.of(PortalCustomer.builder().phone("70123456").build()));

        service.provisionPortalAccess("70123456");

        verify(portalRepo, never()).save(any());
    }

    @Test
    void provisionPortalAccess_skipsBlankOrTooShortPhone() {
        service.provisionPortalAccess(null);
        service.provisionPortalAccess("   ");
        service.provisionPortalAccess("12"); // fewer than 4 digits

        verify(portalRepo, never()).save(any());
    }
}
