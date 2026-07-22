package com.skycarwash.service;

import com.skycarwash.dto.VehicleDto;
import com.skycarwash.entity.Client;
import com.skycarwash.entity.Vehicle;
import com.skycarwash.exception.BusinessException;
import com.skycarwash.repository.ClientRepository;
import com.skycarwash.repository.VehicleRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private static final int MAX_VEHICLES_PER_CLIENT = 20;

    private final VehicleRepository vehicleRepository;
    private final ClientRepository clientRepository;

    public List<VehicleDto> findByClient(Long clientId) {
        requireClient(clientId);
        return vehicleRepository.findByClientIdOrderByCreatedAtAsc(clientId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public VehicleDto add(Long clientId, VehicleDto dto) {
        Client client = requireClient(clientId);
        if (vehicleRepository.countByClientId(clientId) >= MAX_VEHICLES_PER_CLIENT) {
            throw new BusinessException("Nombre maximum de véhicules atteint pour ce client");
        }
        Vehicle vehicle = Vehicle.builder()
                .client(client)
                .plate(trimToNull(dto.plate()))
                .label(trimToNull(dto.label()))
                .type(dto.type())
                .build();
        return toDto(vehicleRepository.save(vehicle));
    }

    @Transactional
    public VehicleDto update(Long clientId, Long vehicleId, VehicleDto dto) {
        Vehicle vehicle = requireVehicle(clientId, vehicleId);
        vehicle.setPlate(trimToNull(dto.plate()));
        vehicle.setLabel(trimToNull(dto.label()));
        vehicle.setType(dto.type());
        return toDto(vehicleRepository.save(vehicle));
    }

    @Transactional
    public void delete(Long clientId, Long vehicleId) {
        Vehicle vehicle = requireVehicle(clientId, vehicleId);
        vehicleRepository.delete(vehicle);
    }

    // ── Helpers ──────────────────────────────────────────────────────── //

    private Client requireClient(Long clientId) {
        return clientRepository.findById(clientId)
                .orElseThrow(() -> new EntityNotFoundException("Client not found: " + clientId));
    }

    private Vehicle requireVehicle(Long clientId, Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new EntityNotFoundException("Vehicle not found: " + vehicleId));
        if (!vehicle.getClient().getId().equals(clientId)) {
            throw new BusinessException("Ce véhicule n'appartient pas à ce client");
        }
        return vehicle;
    }

    private String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    public VehicleDto toDto(Vehicle v) {
        return new VehicleDto(v.getId(), v.getPlate(), v.getLabel(), v.getType(), v.getCreatedAt());
    }
}
