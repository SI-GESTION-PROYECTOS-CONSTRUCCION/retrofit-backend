package com.retrofit.backend.service;
import com.retrofit.backend.dto.ResourceRequestDto;
import com.retrofit.backend.dto.ResourceResponseDto;
import java.util.List;

public interface MaterialService {
    List<ResourceResponseDto> getAll();
    ResourceResponseDto create(ResourceRequestDto dto);
    ResourceResponseDto update(Long id, ResourceRequestDto dto);
    void delete(Long id);
}