package com.skycarwash.service;

import com.skycarwash.dto.ProductDto;
import com.skycarwash.entity.Product;
import com.skycarwash.entity.StockMovement;
import com.skycarwash.exception.BusinessException;
import com.skycarwash.repository.ProductRepository;
import com.skycarwash.repository.StockMovementRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository       productRepository;
    private final StockMovementRepository stockMovementRepository;

    // ── Read ──────────────────────────────────────────────────────────── //

    public List<ProductDto> findAll() {
        return productRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(this::toDto)
                .toList();
    }

    public List<ProductDto> findLowStock() {
        return productRepository.findLowStockProducts().stream()
                .map(this::toDto)
                .toList();
    }

    public ProductDto findById(Long id) {
        return productRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));
    }

    // ── Create ───────────────────────────────────────────────────────── //

    @Transactional
    public ProductDto create(ProductDto dto) {
        Product entity = Product.builder()
                .name(dto.name())
                .stock(dto.stock())
                .alertThreshold(dto.alertThreshold())
                .unit(dto.unit())
                .build();
        Product saved = productRepository.save(entity);

        // Record initial stock as an IN movement if non-zero
        if (dto.stock().compareTo(BigDecimal.ZERO) > 0) {
            stockMovementRepository.save(StockMovement.builder()
                    .product(saved)
                    .quantity(dto.stock())
                    .type(StockMovement.MovementType.IN)
                    .build());
        }

        return toDto(saved);
    }

    // ── Update (name / threshold / unit) ─────────────────────────────── //

    @Transactional
    public ProductDto update(Long id, ProductDto dto) {
        Product entity = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));
        entity.setName(dto.name());
        entity.setAlertThreshold(dto.alertThreshold());
        entity.setUnit(dto.unit());
        entity.setActive(dto.active());
        // Note: stock is NOT updated here — use restock() to add stock
        return toDto(productRepository.save(entity));
    }

    // ── Restock (2-click) ────────────────────────────────────────────── //

    @Transactional
    public ProductDto restock(Long id, BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("La quantité de réapprovisionnement doit être positive");
        }
        Product entity = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));

        entity.setStock(entity.getStock().add(quantity));
        productRepository.save(entity);

        // Record the manual restock as an IN movement (no transaction link)
        stockMovementRepository.save(StockMovement.builder()
                .product(entity)
                .quantity(quantity)
                .type(StockMovement.MovementType.IN)
                .build());

        return toDto(entity);
    }

    // ── Deactivate ───────────────────────────────────────────────────── //

    @Transactional
    public void deactivate(Long id) {
        Product entity = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));
        entity.setActive(false);
        productRepository.save(entity);
    }

    // ── Mapping ──────────────────────────────────────────────────────── //

    public ProductDto toDto(Product p) {
        return new ProductDto(p.getId(), p.getName(), p.getStock(),
                p.getAlertThreshold(), p.getUnit(), p.isActive());
    }
}
