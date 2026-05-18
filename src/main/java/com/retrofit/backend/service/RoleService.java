package com.retrofit.backend.service;

import com.retrofit.backend.dto.RoleRequestDto;
import com.retrofit.backend.dto.RoleResponseDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface RoleService {
    List<RoleResponseDto> getAllRoles();
    RoleResponseDto getRoleById(Long id);
    RoleResponseDto createRole(RoleRequestDto dto);
    RoleResponseDto updateRole(Long id, RoleRequestDto dto);
    void deleteRole(Long id);
}
