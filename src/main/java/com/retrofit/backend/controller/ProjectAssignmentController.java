package com.retrofit.backend.controller;

import com.retrofit.backend.dto.ProjectAssignmentDTO;
import com.retrofit.backend.service.ProjectAssignmentService;
import lombok.RequiredArgsConstructor;
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

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<ProjectAssignmentDTO>> listByProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(assignmentService.getWorkersByProject(projectId));
    }

    @PatchMapping("/{id}/release")
    public ResponseEntity<Void> release(@PathVariable Long id) {
        assignmentService.releaseWorker(id);
        return ResponseEntity.noContent().build();
    }
}
