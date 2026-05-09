package com.retrofit.backend.service;

import com.retrofit.backend.dto.WorkerCreateDTO;
import com.retrofit.backend.dto.WorkerDTO;

import java.util.List;

public interface WorkerService {
    WorkerDTO createWorker(WorkerCreateDTO dto);
    WorkerDTO updateWorker(Long id, WorkerCreateDTO dto);
    WorkerDTO getWorkerById(Long id);
    List<WorkerDTO> getAllWorkers();
    void deleteWorker(Long id);
}
