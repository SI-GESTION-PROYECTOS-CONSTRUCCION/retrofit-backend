package com.retrofit.backend.controller;

import com.retrofit.backend.dto.ProjectAssignmentDTO;
import com.retrofit.backend.service.ProjectAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/project-assignments")
@RequiredArgsConstructor
public class ProjectAssignmentController {
    private final ProjectAssignmentService assignmentService;

    @PostMapping
    public ResponseEntity<ProjectAssignmentDTO> assign(@RequestBody ProjectAssignmentDTO dto) {
        return new ResponseEntity<>(assignmentService.assignWorker(dto), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<Page<ProjectAssignmentDTO>> list(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("assignedAt").descending());
        return ResponseEntity.ok(assignmentService.getWorkersByProject(projectId, search, pageable));
    }

    @GetMapping("/active")
    public ResponseEntity<List<ProjectAssignmentDTO>> getActiveAssignments() {
        return ResponseEntity.ok(assignmentService.getActiveAssignments());
    }

    @PatchMapping("/{id}/release")
    public ResponseEntity<Void> release(@PathVariable Long id) {
        assignmentService.releaseWorker(id);
        return ResponseEntity.noContent().build();
    }
}
