package com.retrofit.backend.service.impl;

import com.retrofit.backend.dto.PermissionDto;
import com.retrofit.backend.repository.PermissionRepository;
import com.retrofit.backend.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {
    private final PermissionRepository permissionRepository;

    public List<PermissionDto> getAllPermissions() {
        return permissionRepository.findAll().stream()
                .map(p -> new PermissionDto(p.getId(), p.getName()))
                .collect(Collectors.toList());
    }
}
