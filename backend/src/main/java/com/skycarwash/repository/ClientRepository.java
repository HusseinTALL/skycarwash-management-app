package com.skycarwash.repository;

import com.skycarwash.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long> {

    Optional<Client> findByPhone(String phone);

    List<Client> findByNameContainingIgnoreCaseAndActiveTrue(String name);

    List<Client> findByPhoneContainingAndActiveTrue(String phone);

    /** Clients expiring within N days – used by alert scheduler */
    List<Client> findByActiveTrueAndExpiresAtBetween(LocalDate from, LocalDate to);

    /** CARTE clients with 1 passage left */
    List<Client> findByActiveTrueAndTypeAndBalanceLessThanEqual(Client.ClientType type, int balance);
}
