package com.retrofit.backend.dto;

import lombok.Data;
import java.util.Set;

@Data
public class RoleRequestDto {
    private String name;
    private String description;
    private Set<Long> permissionIds;
}