package com.skycarwash.repository;

import com.skycarwash.entity.PortalCustomer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PortalCustomerRepository extends JpaRepository<PortalCustomer, Long> {

    Optional<PortalCustomer> findByPhone(String phone);
}
