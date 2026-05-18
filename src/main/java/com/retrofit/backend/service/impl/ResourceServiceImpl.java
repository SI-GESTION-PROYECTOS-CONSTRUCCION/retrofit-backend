package com.retrofit.backend.service.impl;

import com.retrofit.backend.dto.ProjectItemResourceResponseDto;
import com.retrofit.backend.dto.ResourcePageResponseDto;
import com.retrofit.backend.model.Resource;
import com.retrofit.backend.repository.ResourceRepository;
import com.retrofit.backend.service.ResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResourceServiceImpl implements ResourceService {
    private final ResourceRepository resourceRepository;
    @Override
    public ResourcePageResponseDto getResourcesPaginated(int page, int size, String search, String type) {
        // Ordenamos alfabéticamente por nombre
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("name").ascending());

        // Si viene nulo o vacío por seguridad, lo seteamos en "ALL"
        String resourceType = (type == null || type.trim().isEmpty()) ? "ALL" : type.trim().toUpperCase();
        String searchKeyword = (search == null) ? "" : search.trim().toLowerCase();

        // Ejecutamos nuestra query inteligente
        Page<Resource> resourcePage = resourceRepository.findFilteredResources(resourceType, searchKeyword, pageable);

        // Mapeamos a DTOs usando polimorfismo puro (.fetchResourceType())
        List<ProjectItemResourceResponseDto> dtos = resourcePage.getContent().stream().map(res -> {
            ProjectItemResourceResponseDto dto = new ProjectItemResourceResponseDto();
            dto.setResourceId(res.getId());
            dto.setResourceName(res.getName());
            dto.setResourceUnit(res.getUnit());
            dto.setResourceBasePrice(res.getBasePrice());
            dto.setResourceType(res.fetchResourceType());
            return dto;
        }).collect(Collectors.toList());

        // Empaquetamos la respuesta estructurada
        ResourcePageResponseDto response = new ResourcePageResponseDto();
        response.setContent(dtos);
        response.setTotalPages(resourcePage.getTotalPages());
        response.setTotalElements(resourcePage.getTotalElements());
        response.setCurrentPage(page);

        return response;
    }
}
