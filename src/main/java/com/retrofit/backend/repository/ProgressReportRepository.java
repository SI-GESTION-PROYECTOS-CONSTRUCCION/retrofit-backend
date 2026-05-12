package com.retrofit.backend.repository;

import com.retrofit.backend.model.ProgressReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ProgressReportRepository extends JpaRepository<ProgressReport, UUID> {

    @Query("SELECT COALESCE(SUM(pr.executedQuantity), 0) FROM ProgressReport pr WHERE pr.projectItem.id = :itemId")
    Double sumExecutedQuantityByItemId(@Param("itemId") Long itemId);

    @Query("SELECT pr FROM ProgressReport pr WHERE pr.projectItem.project.id = :projectId " +
            "AND (CAST(:startDate AS date) IS NULL OR pr.reportDate >= :startDate) " +
            "AND (CAST(:endDate AS date) IS NULL OR pr.reportDate <= :endDate) " +
            "AND (:itemCode IS NULL OR pr.projectItem.code LIKE :itemCode) " +
            "ORDER BY pr.reportDate DESC")
    List<ProgressReport> findFilteredReports(
            @Param("projectId") Long projectId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("itemCode") String itemCode
    );
}