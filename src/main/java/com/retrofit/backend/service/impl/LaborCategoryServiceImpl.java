package com.retrofit.backend.service.impl;

import com.retrofit.backend.dto.ResourceRequestDto;
import com.retrofit.backend.dto.ResourceResponseDto;
import com.retrofit.backend.exceptions.ResourceNotFoundException;
import com.retrofit.backend.model.LaborCategory;
import com.retrofit.backend.repository.LaborCategoryRepository;
import com.retrofit.backend.service.LaborCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LaborCategoryServiceImpl implements LaborCategoryService {

    private final LaborCategoryRepository repository;

    @Override
    public List<ResourceResponseDto> getAll() {
        return repository.findAll().stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    public ResourceResponseDto create(ResourceRequestDto dto) {
        LaborCategory laborCategory = new LaborCategory();
        updateEntity(laborCategory, dto);
        return mapToDto(repository.save(laborCategory));
    }

    @Override
    public ResourceResponseDto update(Long id, ResourceRequestDto dto) {
        LaborCategory laborCategory = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Labor Category no encontrado"));
        updateEntity(laborCategory, dto);
        return mapToDto(repository.save(laborCategory));
    }

    @Override
    public void delete(Long id) {
        if (!repository.existsById(id)) throw new ResourceNotFoundException("Labor Category no encontrado");
        // Aquí luego le pondremos una validación para no borrar si está usándose en una partida
        repository.deleteById(id);
    }

    // Funciones utilitarias para no repetir código
    private void updateEntity(LaborCategory entity, ResourceRequestDto dto) {
        entity.setName(dto.getName());
        entity.setUnit(dto.getUnit());
        entity.setBasePrice(dto.getBasePrice());
    }

    private ResourceResponseDto mapToDto(LaborCategory entity) {
        ResourceResponseDto dto = new ResourceResponseDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setUnit(entity.getUnit());
        dto.setBasePrice(entity.getBasePrice());
        return dto;
    }
}