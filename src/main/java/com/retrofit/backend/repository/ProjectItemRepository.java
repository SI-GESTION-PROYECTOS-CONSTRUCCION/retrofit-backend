package com.retrofit.backend.repository;

import com.retrofit.backend.model.ProjectItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;

public interface ProjectItemRepository extends JpaRepository<ProjectItem, Long> {
    @EntityGraph(attributePaths = {"apuDetails", "apuDetails.resource"})
    List<ProjectItem> findByProjectIdOrderByItemOrderAsc(Long projectId);
    List<ProjectItem> findByPredecessorId(Long predecessorId);

    // Calcular el Presupuesto Total Planeado de un Proyecto
    @Query("SELECT COALESCE(SUM(pi.totalQuantity * pi.unitPrice), 0.0) " +
            "FROM ProjectItem pi " +
            "WHERE pi.project.id = :projectId")
    Double calculatePlannedValueByProjectId(@Param("projectId") Long projectId);
    List<ProjectItem> findByProjectId(Long projectId);
}
