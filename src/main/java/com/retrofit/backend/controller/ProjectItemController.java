package com.retrofit.backend.controller;

import com.retrofit.backend.dto.ProjectItemRequestDto;
import com.retrofit.backend.dto.ProjectItemResourceRequestDto;
import com.retrofit.backend.dto.ProjectItemResponseDto;
import com.retrofit.backend.service.ProjectItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/projects/{projectId}/items")
@RequiredArgsConstructor
public class ProjectItemController {

    private final ProjectItemService itemService;

    @GetMapping
    @PreAuthorize("hasAuthority('PROJECT_READ')")
    public ResponseEntity<List<ProjectItemResponseDto>> getProjectItems(@PathVariable Long projectId) {
        return ResponseEntity.ok(itemService.getItemsByProjectId(projectId));
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasAuthority('PROJECT_CREATE')")
    public ResponseEntity<List<ProjectItemResponseDto>> createBulkItems(
            @PathVariable Long projectId,
            @RequestBody @Valid List<ProjectItemRequestDto> dtos) {

        List<ProjectItemResponseDto> savedItems = itemService.saveBulkItems(projectId, dtos);
        return new ResponseEntity<>(savedItems, HttpStatus.CREATED);
    }

    @PostMapping("/{itemId}/apu")
    @PreAuthorize("hasAuthority('PROJECT_UPDATE')")
    public ResponseEntity<ProjectItemResponseDto> saveApuDetails(
            @PathVariable Long itemId,
            @RequestParam(defaultValue = "0") Double laborYield,
            @RequestParam(defaultValue = "0") Double equipmentYield,
            @RequestBody @Valid List<ProjectItemResourceRequestDto> dtos) {

        ProjectItemResponseDto updatedItem = itemService.saveApuDetails(itemId, laborYield, equipmentYield, dtos);
        return ResponseEntity.ok(updatedItem);
    }
}