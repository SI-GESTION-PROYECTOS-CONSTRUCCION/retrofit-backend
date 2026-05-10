package com.retrofit.backend.repository;

import com.retrofit.backend.model.ProjectAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectAssignmentRepository extends JpaRepository<ProjectAssignment, Long> {
    // Buscar trabajadores activos en un proyecto específico
    List<ProjectAssignment> findByProjectIdAndActiveTrue(Long projectId);

    // Buscar historial de proyectos de un trabajador
    List<ProjectAssignment> findByWorkerId(Long workerId);
}
