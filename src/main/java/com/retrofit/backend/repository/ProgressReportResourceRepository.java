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

    // Calcula el Costo Real (AC) acumulado de una partida específica
    @Query("SELECT COALESCE(SUM(prr.realQuantity * r.basePrice), 0.0) " +
            "FROM ProgressReportResource prr " +
            "JOIN prr.progressReport pr " +
            "JOIN prr.resource r " +
            "WHERE pr.projectItem.project.id = :projectId AND (pr.projectItem.code = :exactCode OR pr.projectItem.code LIKE :prefixCode)")
    Double calculateActualCostByProjectItemCode(@Param("projectId") Long projectId, @Param("exactCode") String exactCode, @Param("prefixCode") String prefixCode);

    // Calcula el Costo Real AGRUPADO POR TIPO DE RECURSO (Para la Dona)
    @Query("SELECT TYPE(r), COALESCE(SUM(prr.realQuantity * r.basePrice), 0.0) " +
            "FROM ProgressReportResource prr " +
            "JOIN prr.progressReport pr " +
            "JOIN pr.projectItem pi " +
            "JOIN prr.resource r " +
            "WHERE pi.project.id = :projectId AND (:exactCode IS NULL OR pi.code = :exactCode OR pi.code LIKE :prefixCode) " +
            "GROUP BY TYPE(r)")
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