package com.retrofit.backend.controller;

import com.retrofit.backend.dto.ResourcePageResponseDto;
import com.retrofit.backend.service.ResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/resources")
@RequiredArgsConstructor
public class ResourceController {
    private final ResourceService resourceService;

    @GetMapping("/paginated")
    public ResponseEntity<ResourcePageResponseDto> getResources(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "ALL") String type) { // 🚨 NUEVO PARÁMETRO

        return ResponseEntity.ok(resourceService.getResourcesPaginated(page, size, search, type));
    }
}
