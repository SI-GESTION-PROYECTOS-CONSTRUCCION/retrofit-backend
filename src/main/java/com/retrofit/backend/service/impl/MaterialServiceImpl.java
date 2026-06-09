package com.retrofit.backend.service.impl;

import com.retrofit.backend.annotation.AuditChange;
import com.retrofit.backend.dto.ResourceRequestDto;
import com.retrofit.backend.dto.ResourceResponseDto;
import com.retrofit.backend.exceptions.ResourceNotFoundException;
import com.retrofit.backend.model.Material;
import com.retrofit.backend.repository.MaterialRepository;
import com.retrofit.backend.service.AuditService;
import com.retrofit.backend.service.MaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MaterialServiceImpl implements MaterialService {

    private final MaterialRepository repository;
    private final AuditService auditService;

    @Override
    public List<ResourceResponseDto> getAll() {
        return repository.findAll().stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    @AuditChange(action = "CREATE", module = "Recursos")
    public ResourceResponseDto create(ResourceRequestDto dto) {
        Material material = new Material();
        updateEntity(material, dto);
        return mapToDto(repository.save(material));
    }

    @Override
    public ResourceResponseDto update(Long id, ResourceRequestDto dto) {
        Material material = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Material no encontrado"));
        ResourceResponseDto estadoAnterior = mapToDto(material);
        updateEntity(material, dto);
        ResourceResponseDto estadoNuevo = mapToDto(material);
        auditService.logAction("UPDATE", "Recursos", material.getId(), estadoAnterior, estadoNuevo);
        return mapToDto(repository.save(material));
    }

    @Override
    @AuditChange(action = "DELETE", module = "Recursos")
    public void delete(Long id) {
        if (!repository.existsById(id)) throw new ResourceNotFoundException("Material no encontrado");
        // Aquí luego le pondremos una validación para no borrar si está usándose en una partida
        repository.deleteById(id);
    }

    // Funciones utilitarias para no repetir código
    private void updateEntity(Material entity, ResourceRequestDto dto) {
        entity.setName(dto.getName());
        entity.setUnit(dto.getUnit());
        entity.setBasePrice(dto.getBasePrice());
    }

    private ResourceResponseDto mapToDto(Material entity) {
        ResourceResponseDto dto = new ResourceResponseDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setUnit(entity.getUnit());
        dto.setBasePrice(entity.getBasePrice());
        return dto;
    }
}