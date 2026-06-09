package com.retrofit.backend.service.impl;

import com.retrofit.backend.annotation.AuditChange;
import com.retrofit.backend.dto.ResourceRequestDto;
import com.retrofit.backend.dto.ResourceResponseDto;
import com.retrofit.backend.exceptions.ResourceNotFoundException;
import com.retrofit.backend.model.Equipment;
import com.retrofit.backend.repository.EquipmentRepository;
import com.retrofit.backend.service.AuditService;
import com.retrofit.backend.service.EquipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EquipmentServiceImpl implements EquipmentService {

    private final EquipmentRepository repository;
    private final AuditService auditService;

    @Override
    public List<ResourceResponseDto> getAll() {
        return repository.findAll().stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    @AuditChange(action = "CREATE", module = "Recursos")
    public ResourceResponseDto create(ResourceRequestDto dto) {
        Equipment equipment = new Equipment();
        updateEntity(equipment, dto);
        return mapToDto(repository.save(equipment));
    }

    @Override
    public ResourceResponseDto update(Long id, ResourceRequestDto dto) {
        Equipment equipment = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Maquinaria no encontrado"));
        ResourceResponseDto estadoAnterior = mapToDto(equipment);
        updateEntity(equipment, dto);
        ResourceResponseDto estadoNuevo = mapToDto(equipment);
        auditService.logAction("UPDATE", "Recursos", equipment.getId(), estadoAnterior, estadoNuevo);
        return mapToDto(repository.save(equipment));
    }

    @Override
    @AuditChange(action = "DELETE", module = "Recursos")
    public void delete(Long id) {
        if (!repository.existsById(id)) throw new ResourceNotFoundException("Maquinaria no encontrado");
        // Aquí luego le pondremos una validación para no borrar si está usándose en una partida
        repository.deleteById(id);
    }

    // Funciones utilitarias para no repetir código
    private void updateEntity(Equipment entity, ResourceRequestDto dto) {
        entity.setName(dto.getName());
        entity.setUnit(dto.getUnit());
        entity.setBasePrice(dto.getBasePrice());
    }

    private ResourceResponseDto mapToDto(Equipment entity) {
        ResourceResponseDto dto = new ResourceResponseDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setUnit(entity.getUnit());
        dto.setBasePrice(entity.getBasePrice());
        return dto;
    }
}