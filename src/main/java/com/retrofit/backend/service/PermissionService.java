package com.retrofit.backend.service;

import com.retrofit.backend.dto.PermissionDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface PermissionService {
    public List<PermissionDto> getAllPermissions();
}