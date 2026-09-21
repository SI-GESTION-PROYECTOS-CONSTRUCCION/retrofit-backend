package com.retrofit.backend.repository;

import com.retrofit.backend.model.ProgressReportResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProgressReportResourceRepository extends JpaRepository<ProgressReportResource, Long> {
    // Calcula el Costo Real (AC) Total de un proyecto
    @Query("SELECT COALESCE(SUM(prr.realQuantity * r.basePrice), 0.0) " +
            "FROM ProgressReportResource prr " +
            "JOIN prr.progressReport pr " +
            "JOIN pr.projectItem pi " +
            "JOIN prr.resource r " +
            "WHERE pi.project.id = :projectId AND (:exactCode IS NULL OR pi.code = :exactCode OR pi.code LIKE :prefixCode)")
    Double calculateActualCostByProjectId(@Param("projectId") Long projectId, @Param("exactCode") String exactCode, @Param("prefixCode") String prefixCode);

    // Calcula el Costo Real (AC) agrupado por partida para todo el proyecto en 1 sola consulta
    @Query("SELECT pr.projectItem.id, COALESCE(SUM(prr.realQuantity * r.basePrice), 0.0) " +
            "FROM ProgressReportResource prr " +
            "JOIN prr.progressReport pr " +
            "JOIN prr.resource r " +
            "WHERE pr.projectItem.project.id = :projectId " +
            "GROUP BY pr.projectItem.id")
    List<Object[]> sumActualCostByProjectIdGroupedByItemId(@Param("projectId") Long projectId);

    // Calcula el Costo Real (AC) acumulado de una partida específica
    @Query("SELECT COALESCE(SUM(prr.realQuantity * r.basePrice), 0.0) " +
            "FROM ProgressReportResource prr " +
            "JOIN prr.progressReport pr " +
            "JOIN prr.resource r " +
            "WHERE pr.projectItem.project.id = :projectId AND (pr.projectItem.code = :exactCode OR pr.projectItem.code LIKE :prefixCode)")
    Double calculateActualCostByProjectItemCode(@Param("projectId") Long projectId, @Param("exactCode") String exactCode, @Param("prefixCode") String prefixCode);

    // Calcula el Costo Real AGRUPADO POR TIPO DE RECURSO (Para la Dona)
    @Query(value = "SELECT CASE WHEN e.id IS NOT NULL THEN 'EQUIPMENT' WHEN l.id IS NOT NULL THEN 'LABOR' " +
            "WHEN m.id IS NOT NULL THEN 'MATERIAL' ELSE 'OTHER' END, COALESCE(SUM(prr.real_quantity * r.base_price), 0) " +
            "FROM progress_report_resources prr JOIN progress_reports pr ON pr.id = prr.report_id " +
            "JOIN project_items pi ON pi.id = pr.project_item_id JOIN resources r ON r.id = prr.resource_id " +
            "LEFT JOIN equipments e ON e.id = r.id LEFT JOIN labor_categories l ON l.id = r.id LEFT JOIN materials m ON m.id = r.id " +
            "WHERE pi.project_id = :projectId AND (:exactCode IS NULL OR pi.code = :exactCode OR pi.code LIKE :prefixCode) GROUP BY 1", nativeQuery = true)
    List<Object[]> calculateActualCostByResourceType(@Param("projectId") Long projectId, @Param("exactCode") String exactCode, @Param("prefixCode") String prefixCode);

    // Calcula el Costo Real por fecha (Para la Curva S)
    @Query("SELECT pr.reportDate, COALESCE(SUM(prr.realQuantity * r.basePrice), 0.0) " +
            "FROM ProgressReportResource prr " +
            "JOIN prr.progressReport pr " +
            "JOIN pr.projectItem pi " +
            "JOIN prr.resource r " +
            "WHERE pi.project.id = :projectId AND (:exactCode IS NULL OR pi.code = :exactCode OR pi.code LIKE :prefixCode) " +
            "GROUP BY pr.reportDate " +
            "ORDER BY pr.reportDate ASC")
    List<Object[]> getActualCostByDate(@Param("projectId") Long projectId, @Param("exactCode") String exactCode, @Param("prefixCode") String prefixCode);
}
