package com.retrofit.backend.service.impl;

import com.retrofit.backend.dto.PermissionDto;
import com.retrofit.backend.dto.RoleRequestDto;
import com.retrofit.backend.dto.RoleResponseDto;
import com.retrofit.backend.model.Permission;
import com.retrofit.backend.model.RoleE;
import com.retrofit.backend.repository.PermissionRepository;
import com.retrofit.backend.repository.RoleRepository;
import com.retrofit.backend.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Transactional(readOnly = true)
    public List<RoleResponseDto> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RoleResponseDto getRoleById(Long id) {
        RoleE role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rol no encontrado"));
        return mapToResponseDto(role);
    }

    @Transactional
    public RoleResponseDto createRole(RoleRequestDto dto) {
        RoleE role = new RoleE();
        role.setName(dto.getName());
        role.setDescription(dto.getDescription());

        Set<Permission> permissions = permissionRepository.findAllById(dto.getPermissionIds())
                .stream().collect(Collectors.toSet());
        role.setPermissions(permissions);

        return mapToResponseDto(roleRepository.save(role));
    }

    @Transactional
    public RoleResponseDto updateRole(Long id, RoleRequestDto dto) {
        RoleE role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rol no encontrado"));

        role.setName(dto.getName());
        role.setDescription(dto.getDescription());

        Set<Permission> permissions = permissionRepository.findAllById(dto.getPermissionIds())
                .stream().collect(Collectors.toSet());
        role.setPermissions(permissions);

        return mapToResponseDto(roleRepository.save(role));
    }

    @Transactional
    public void deleteRole(Long id) {
        RoleE role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rol no encontrado"));
        if (role.getName().equals("ADMIN")) {
            throw new RuntimeException("No se puede eliminar el rol de superadministrador.");
        }
        roleRepository.deleteById(id);
    }

    private RoleResponseDto mapToResponseDto(RoleE role) {
        RoleResponseDto dto = new RoleResponseDto();
        dto.setId(role.getId());
        dto.setName(role.getName());
        dto.setDescription(role.getDescription());

        Set<PermissionDto> permDtos = role.getPermissions().stream()
                .map(p -> new PermissionDto(p.getId(), p.getName()))
                .collect(Collectors.toSet());
        dto.setPermissions(permDtos);

        return dto;
    }
}