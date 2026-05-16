package com.retrofit.backend.controller;

import com.retrofit.backend.dto.ResourceRequestDto;
import com.retrofit.backend.dto.ResourceResponseDto;
import com.retrofit.backend.service.LaborCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/labor-categories")
@RequiredArgsConstructor
public class LaborCategoryController {

    private final LaborCategoryService service;

    @GetMapping
    public ResponseEntity<List<ResourceResponseDto>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @PostMapping
    public ResponseEntity<ResourceResponseDto> create(@RequestBody @Valid ResourceRequestDto dto) {
        return new ResponseEntity<>(service.create(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResourceResponseDto> update(@PathVariable Long id, @RequestBody @Valid ResourceRequestDto dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}