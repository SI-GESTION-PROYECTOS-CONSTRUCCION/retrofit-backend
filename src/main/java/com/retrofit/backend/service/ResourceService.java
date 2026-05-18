package com.retrofit.backend.service;

import com.retrofit.backend.dto.ResourcePageResponseDto;
import org.springframework.stereotype.Service;

@Service
public interface ResourceService {
    ResourcePageResponseDto getResourcesPaginated(int page, int size, String search, String type);
}
