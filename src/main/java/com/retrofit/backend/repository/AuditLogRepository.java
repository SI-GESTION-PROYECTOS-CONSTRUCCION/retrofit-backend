package com.retrofit.backend.repository;

import com.retrofit.backend.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Page<AuditLog> findAllByOrderByActionDateDesc(Pageable pageable);
    long countByActionDateBetween(LocalDateTime startOfDay, LocalDateTime endOfDay);
    long countByAction(String action);

    @Query("SELECT a FROM AuditLog a WHERE " +
            "(:search IS NULL OR :search = '' OR LOWER(a.user.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(a.user.username) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
            "(:module IS NULL OR :module = '' OR a.affectedTable = :module) AND " +
            "(:action IS NULL OR :action = '' OR a.action = :action) AND " +
            "(cast(:startDate as timestamp) IS NULL OR a.actionDate >= :startDate) AND " +
            "(cast(:endDate as timestamp) IS NULL OR a.actionDate <= :endDate)")
    Page<AuditLog> findWithFilters(
            @Param("search") String search,
            @Param("module") String module,
            @Param("action") String action,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.action IN :acciones OR a.affectedTable = :modulo")
    long countAlerts(@Param("acciones") List<String> acciones, @Param("modulo") String modulo);
}
