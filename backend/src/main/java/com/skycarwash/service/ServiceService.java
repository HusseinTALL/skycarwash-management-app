package com.skycarwash.service;

import com.skycarwash.dto.ServiceDto;
import com.skycarwash.entity.ServiceEntity;
import com.skycarwash.repository.ServiceRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ServiceService {

    private final ServiceRepository serviceRepository;

    public List<ServiceDto> findAll() {
        return serviceRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(this::toDto)
                .toList();
    }

    public ServiceDto findById(Long id) {
        return serviceRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new EntityNotFoundException("Service not found: " + id));
    }

    @Transactional
    public ServiceDto create(ServiceDto dto) {
        ServiceEntity entity = ServiceEntity.builder()
                .name(dto.name())
                .price(dto.price())
                .category(dto.category())
                .productConsumption(dto.productConsumption())
                .build();
        return toDto(serviceRepository.save(entity));
    }

    @Transactional
    public ServiceDto update(Long id, ServiceDto dto) {
        ServiceEntity entity = serviceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Service not found: " + id));
        entity.setName(dto.name());
        entity.setPrice(dto.price());
        entity.setCategory(dto.category());
        entity.setProductConsumption(dto.productConsumption());
        entity.setActive(dto.active());
        return toDto(serviceRepository.save(entity));
    }

    @Transactional
    public void deactivate(Long id) {
        ServiceEntity entity = serviceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Service not found: " + id));
        entity.setActive(false);
        serviceRepository.save(entity);
    }

    private ServiceDto toDto(ServiceEntity e) {
        return new ServiceDto(e.getId(), e.getName(), e.getPrice(),
                e.getCategory(), e.getProductConsumption(), e.isActive());
    }
}
