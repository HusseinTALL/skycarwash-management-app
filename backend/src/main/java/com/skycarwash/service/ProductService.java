package com.skycarwash.service;

import com.skycarwash.dto.ProductDto;
import com.skycarwash.entity.Product;
import com.skycarwash.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

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

    @Transactional
    public ProductDto create(ProductDto dto) {
        Product entity = Product.builder()
                .name(dto.name())
                .stock(dto.stock())
                .alertThreshold(dto.alertThreshold())
                .unit(dto.unit())
                .build();
        return toDto(productRepository.save(entity));
    }

    @Transactional
    public ProductDto update(Long id, ProductDto dto) {
        Product entity = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));
        entity.setName(dto.name());
        entity.setStock(dto.stock());
        entity.setAlertThreshold(dto.alertThreshold());
        entity.setUnit(dto.unit());
        entity.setActive(dto.active());
        return toDto(productRepository.save(entity));
    }

    @Transactional
    public void deactivate(Long id) {
        Product entity = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));
        entity.setActive(false);
        productRepository.save(entity);
    }

    private ProductDto toDto(Product p) {
        return new ProductDto(p.getId(), p.getName(), p.getStock(),
                p.getAlertThreshold(), p.getUnit(), p.isActive());
    }
}
