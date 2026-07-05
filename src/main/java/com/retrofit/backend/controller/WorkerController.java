package com.retrofit.backend.controller;

import com.retrofit.backend.dto.WorkerCreateDTO;
import com.retrofit.backend.dto.WorkerDTO;
import com.retrofit.backend.service.WorkerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/workers")
@RequiredArgsConstructor
public class WorkerController {
    private final WorkerService workerService;

    @PostMapping
    @PreAuthorize("hasAuthority('WORKER_CREATE')")
    public ResponseEntity<WorkerDTO> create(@Valid @RequestBody WorkerCreateDTO dto) {
        return new ResponseEntity<>(workerService.createWorker(dto), HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('WORKER_READ')")
    public ResponseEntity<Page<WorkerDTO>> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return ResponseEntity.ok(workerService.getAllWorkers(search, active, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('WORKER_READ')")
    public ResponseEntity<WorkerDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(workerService.getWorkerById(id));
    }

    @GetMapping("/available")
    @PreAuthorize("hasAuthority('WORKER_READ')")
    public ResponseEntity<List<WorkerDTO>> getAvailableWorkers() {
        return ResponseEntity.ok(workerService.getAvailableWorkers());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('WORKER_UPDATE')")
    public ResponseEntity<WorkerDTO> update(@PathVariable Long id, @Valid @RequestBody WorkerCreateDTO dto) {
        return ResponseEntity.ok(workerService.updateWorker(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('WORKER_DELETE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        workerService.deleteWorker(id);
        return ResponseEntity.noContent().build();
    }
}