package com.retrofit.backend.controller;

import com.retrofit.backend.dto.ProjectRequestDto;
import com.retrofit.backend.dto.ProjectResponseDto;
import com.retrofit.backend.enums.ProjectPriority;
import com.retrofit.backend.enums.ProjectStatus;
import com.retrofit.backend.service.ProjectService;
import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping("/filters/statuses")
    public ResponseEntity<List<String>> getStatuses() {
        List<String> statuses = Arrays.stream(ProjectStatus.values())
                .map(Enum::name)
                .collect(Collectors.toList());
        return ResponseEntity.ok(statuses);
    }

    @GetMapping("/filters/priorities")
    public ResponseEntity<List<String>> getPriorities() {
        List<String> priorities = Arrays.stream(ProjectPriority.values())
                .map(Enum::name)
                .collect(Collectors.toList());
        return ResponseEntity.ok(priorities);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PROJECT_READ')")
    public ResponseEntity<Page<ProjectResponseDto>> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).descending());

        return ResponseEntity.ok(projectService.getAllProjects(search, priority, status, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PROJECT_READ')")
    public ResponseEntity<ProjectResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(projectService.getProjectById(id));
    }

    @GetMapping("/code/{code}")
    @PreAuthorize("hasAuthority('PROJECT_READ')")
    public ResponseEntity<ProjectResponseDto> getByCode(@PathVariable String code) {
        return ResponseEntity.ok(projectService.getProjectByCode(code));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PROJECT_CREATE')")
    public ResponseEntity<ProjectResponseDto> create(@Validated({Default.class, ProjectRequestDto.OnCreate.class}) @RequestBody ProjectRequestDto dto) {
        return new ResponseEntity<>(projectService.createProject(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PROJECT_UPDATE')")
    public ResponseEntity<ProjectResponseDto> update(@PathVariable Long id, @Valid @RequestBody ProjectRequestDto dto) {
        return ResponseEntity.ok(projectService.updateProject(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PROJECT_DELETE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }
}