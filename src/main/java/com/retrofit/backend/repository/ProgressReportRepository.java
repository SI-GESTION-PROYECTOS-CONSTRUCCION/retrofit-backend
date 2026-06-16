package com.retrofit.backend.repository;

import com.retrofit.backend.model.ProgressReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ProgressReportRepository extends JpaRepository<ProgressReport, Long> {

        @Query("SELECT COALESCE(SUM(pr.executedQuantity), 0) FROM ProgressReport pr WHERE pr.projectItem.id = :itemId")
        Double sumExecutedQuantityByItemId(@Param("itemId") Long itemId);

        @Query("SELECT pr.projectItem.id, COALESCE(SUM(pr.executedQuantity), 0) FROM ProgressReport pr WHERE pr.projectItem.project.id = :projectId GROUP BY pr.projectItem.id")
        List<Object[]> sumExecutedQuantityByProjectIdGroupedByItemId(@Param("projectId") Long projectId);

        @Query("SELECT pr FROM ProgressReport pr WHERE pr.projectItem.project.id = :projectId " +
                        "AND (CAST(:startDate AS date) IS NULL OR pr.reportDate >= :startDate) " +
                        "AND (CAST(:endDate AS date) IS NULL OR pr.reportDate <= :endDate) " +
                        "AND (:itemCode IS NULL OR pr.projectItem.code LIKE :itemCode) " +
                        "ORDER BY pr.reportDate DESC")
        List<ProgressReport> findFilteredReports(
                        @Param("projectId") Long projectId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("itemCode") String itemCode);

        // Calcula el Valor Ganado (EV) Total de un proyecto
        @Query("SELECT COALESCE(SUM(pr.executedQuantity * pi.unitPrice), 0.0) " +
                        "FROM ProgressReport pr " +
                        "JOIN pr.projectItem pi " +
                        "WHERE pi.project.id = :projectId")
        Double calculateEarnedValueByProjectId(@Param("projectId") Long projectId);

        // Trae todos los reportes de progreso de un proyecto ordenados por fecha (Para
        // la Curva S)
        @Query("SELECT pr FROM ProgressReport pr " +
                        "JOIN pr.projectItem pi " +
                        "WHERE pi.project.id = :projectId " +
                        "ORDER BY pr.reportDate ASC")
        List<ProgressReport> findAllByProjectIdOrderByDateAsc(@Param("projectId") Long projectId);

        // Calcula el Valor Ganado (EV) de una partida específica (Para saber si está en
        // rojo)
        @Query("SELECT COALESCE(SUM(pr.executedQuantity * pi.unitPrice), 0.0) " +
                        "FROM ProgressReport pr " +
                        "JOIN pr.projectItem pi " +
                        "WHERE pi.id = :projectItemId")
        Double calculateEarnedValueByProjectItemId(@Param("projectItemId") Long projectItemId);

        // Calcula el Valor Ganado por fecha (Para la Curva S)
        @Query("SELECT pr.reportDate, COALESCE(SUM(pr.executedQuantity * pi.unitPrice), 0.0) " +
                        "FROM ProgressReport pr " +
                        "JOIN pr.projectItem pi " +
                        "WHERE pi.project.id = :projectId " +
                        "GROUP BY pr.reportDate " +
                        "ORDER BY pr.reportDate ASC")
        List<Object[]> getEarnedValueByDate(@Param("projectId") Long projectId);
}