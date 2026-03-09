package com.skycarwash.service;

import com.skycarwash.dto.ProductDto;
import com.skycarwash.entity.Product;
import com.skycarwash.entity.StockMovement;
import com.skycarwash.exception.BusinessException;
import com.skycarwash.repository.ProductRepository;
import com.skycarwash.repository.StockMovementRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock ProductRepository       productRepository;
    @Mock StockMovementRepository stockMovementRepository;

    @InjectMocks ProductService productService;

    private Product shampoo;

    @BeforeEach
    void setUp() {
        shampoo = Product.builder()
                .id(1L).name("Shampoo").stock(new BigDecimal("5.000"))
                .alertThreshold(new BigDecimal("1.000")).unit("L").build();
    }

    // ── RESTOCK ─────────────────────────────────────────────────────── //

    @Test
    void restock_addsQuantityToStock() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(shampoo));
        when(productRepository.save(any())).thenReturn(shampoo);

        ProductDto result = productService.restock(1L, new BigDecimal("3.000"));

        assertThat(shampoo.getStock()).isEqualByComparingTo("8.000"); // 5 + 3
        assertThat(result.stock()).isEqualByComparingTo("8.000");
    }

    @Test
    void restock_createsInMovement() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(shampoo));
        when(productRepository.save(any())).thenReturn(shampoo);

        productService.restock(1L, new BigDecimal("2.500"));

        verify(stockMovementRepository).save(argThat(mv ->
                mv.getType() == StockMovement.MovementType.IN
                && mv.getQuantity().compareTo(new BigDecimal("2.500")) == 0));
    }

    @Test
    void restock_zeroQuantity_throwsBusinessException() {
        assertThatThrownBy(() -> productService.restock(1L, BigDecimal.ZERO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("positive");
    }

    @Test
    void restock_negativeQuantity_throwsBusinessException() {
        assertThatThrownBy(() -> productService.restock(1L, new BigDecimal("-1")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("positive");
    }

    @Test
    void restock_nullQuantity_throwsBusinessException() {
        assertThatThrownBy(() -> productService.restock(1L, null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void restock_unknownProduct_throwsEntityNotFoundException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.restock(99L, new BigDecimal("1")))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ── CREATE ──────────────────────────────────────────────────────── //

    @Test
    void create_persistsProductAndRecordsInitialStock() {
        ProductDto dto = new ProductDto(null, "Wax", new BigDecimal("10.000"),
                new BigDecimal("2.000"), "g", true);

        Product saved = Product.builder()
                .id(2L).name("Wax").stock(new BigDecimal("10.000"))
                .alertThreshold(new BigDecimal("2.000")).unit("g").build();

        when(productRepository.save(any())).thenReturn(saved);

        ProductDto result = productService.create(dto);

        assertThat(result.id()).isEqualTo(2L);
        assertThat(result.name()).isEqualTo("Wax");
        // Initial stock > 0 → should create an IN movement
        verify(stockMovementRepository).save(argThat(mv ->
                mv.getType() == StockMovement.MovementType.IN));
    }

    @Test
    void create_zeroInitialStock_doesNotCreateMovement() {
        ProductDto dto = new ProductDto(null, "NewProd", BigDecimal.ZERO,
                new BigDecimal("1.000"), "L", true);

        Product saved = Product.builder()
                .id(3L).name("NewProd").stock(BigDecimal.ZERO)
                .alertThreshold(new BigDecimal("1.000")).unit("L").build();

        when(productRepository.save(any())).thenReturn(saved);

        productService.create(dto);

        verify(stockMovementRepository, never()).save(any());
    }

    // ── FIND ────────────────────────────────────────────────────────── //

    @Test
    void findAll_returnsMappedList() {
        when(productRepository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(shampoo));

        List<ProductDto> all = productService.findAll();

        assertThat(all).hasSize(1);
        assertThat(all.get(0).name()).isEqualTo("Shampoo");
    }

    @Test
    void findById_unknownId_throwsEntityNotFoundException() {
        when(productRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findById(42L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ── DEACTIVATE ──────────────────────────────────────────────────── //

    @Test
    void deactivate_setsActiveToFalse() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(shampoo));
        when(productRepository.save(any())).thenReturn(shampoo);

        productService.deactivate(1L);

        assertThat(shampoo.isActive()).isFalse();
        verify(productRepository).save(shampoo);
    }
}
