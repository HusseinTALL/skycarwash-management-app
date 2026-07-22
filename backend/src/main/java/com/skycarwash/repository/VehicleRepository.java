package com.skycarwash.repository;

import com.skycarwash.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    List<Vehicle> findByClientIdOrderByCreatedAtAsc(Long clientId);

    long countByClientId(Long clientId);

    /** [clientId, vehicleCount] for every client that owns at least one vehicle. */
    @Query("SELECT v.client.id, COUNT(v) FROM Vehicle v GROUP BY v.client.id")
    List<Object[]> aggregateCountsByClient();
}
