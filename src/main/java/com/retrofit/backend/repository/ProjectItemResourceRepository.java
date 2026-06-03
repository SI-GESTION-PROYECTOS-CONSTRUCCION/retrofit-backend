package com.retrofit.backend.repository;

import com.retrofit.backend.dto.PlannedResourceDTO;
import com.retrofit.backend.model.ProjectItemResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProjectItemResourceRepository extends JpaRepository<ProjectItemResource, Long> {
    List<ProjectItemResource> findByProjectItemId(Long projectItemId);
    @Query("SELECT DISTINCT new com.retrofit.backend.dto.PlannedResourceDTO(" +
            "r.id, r.name, r.unit) " +
            "FROM ProjectItemResource pir " +
            "JOIN pir.resource r " +
            "JOIN pir.projectItem pi " +
            "WHERE pi.project.id = :projectId " +
            "AND TYPE(r) = Material " +
            "ORDER BY r.name ASC")
    List<PlannedResourceDTO> findPlannedMaterialsByProjectId(@Param("projectId") Long projectId);
}