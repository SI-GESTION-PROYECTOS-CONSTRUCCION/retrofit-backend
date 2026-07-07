package com.retrofit.backend.repository;

import com.retrofit.backend.model.ProjectAssignment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProjectAssignmentRepository extends JpaRepository<ProjectAssignment, Long> {
        // Buscar trabajadores activos en un proyecto específico
        List<ProjectAssignment> findByProjectIdAndActiveTrue(Long projectId);

        // Buscar historial de proyectos de un trabajador
        List<ProjectAssignment> findByWorkerId(Long workerId);

        List<ProjectAssignment> findByActiveTrue();

        @Query("SELECT a FROM ProjectAssignment a WHERE " +
                        "(:projectId IS NULL OR a.project.id = :projectId) AND " +
                        "(:search = '' OR LOWER(a.worker.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                        "LOWER(a.project.name) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
                        "a.active = true")
        Page<ProjectAssignment> findActiveAssignments(@Param("projectId") Long projectId,
                        @Param("search") String search,
                        Pageable pageable);
}
