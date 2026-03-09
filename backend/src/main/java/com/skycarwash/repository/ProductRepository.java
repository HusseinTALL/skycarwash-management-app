package com.skycarwash.repository;

import com.skycarwash.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByActiveTrueOrderByNameAsc();

    /** Products whose stock is at or below alert threshold */
    @Query("SELECT p FROM Product p WHERE p.active = true AND p.stock <= p.alertThreshold")
    List<Product> findLowStockProducts();

    Optional<Product> findByNameIgnoreCase(String name);
}
