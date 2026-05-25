package com.retrofit.backend.repository;

import com.retrofit.backend.model.ProjectItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProjectItemRepository extends JpaRepository<ProjectItem, Long> {
    List<ProjectItem> findByProjectIdOrderByItemOrderAsc(Long projectId);

    // Calcular el Presupuesto Total Planeado de un Proyecto
    @Query("SELECT COALESCE(SUM(pi.totalQuantity * pi.unitPrice), 0.0) " +
            "FROM ProjectItem pi " +
            "WHERE pi.project.id = :projectId")
    Double calculatePlannedValueByProjectId(@Param("projectId") Long projectId);

    // Trae las partidas de un proyecto para luego buscar las que están en rojo
    List<ProjectItem> findByProjectId(Long projectId);
}
