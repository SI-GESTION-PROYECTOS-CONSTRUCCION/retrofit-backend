package com.retrofit.backend.service;

import com.retrofit.backend.dto.WorkerCreateDTO;
import com.retrofit.backend.dto.WorkerDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface WorkerService {
    WorkerDTO createWorker(WorkerCreateDTO dto);
    WorkerDTO updateWorker(Long id, WorkerCreateDTO dto);
    WorkerDTO getWorkerById(Long id);
    Page<WorkerDTO> getAllWorkers(String search, Pageable pageable);
    List<WorkerDTO> getAvailableWorkers();
    void deleteWorker(Long id);
}
