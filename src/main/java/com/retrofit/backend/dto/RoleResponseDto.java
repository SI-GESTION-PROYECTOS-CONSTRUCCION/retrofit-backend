package com.retrofit.backend.dto;

import lombok.Data;
import java.util.Set;

@Data
public class RoleResponseDto {
    private Long id;
    private String name;
    private String description;
    private Set<PermissionDto> permissions;
}