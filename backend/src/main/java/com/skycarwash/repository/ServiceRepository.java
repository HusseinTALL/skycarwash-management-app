package com.skycarwash.repository;

import com.skycarwash.entity.ServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceRepository extends JpaRepository<ServiceEntity, Long> {

    List<ServiceEntity> findByActiveTrueOrderByNameAsc();

    List<ServiceEntity> findByCategoryAndActiveTrue(String category);
}
