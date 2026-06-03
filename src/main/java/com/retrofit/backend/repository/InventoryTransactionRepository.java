package com.retrofit.backend.repository;

import com.retrofit.backend.dto.StockSummaryDTO;
import com.retrofit.backend.model.InventoryTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {

    // 1. Historial completo de un material en un proyecto (Para el Kardex de la vista operativa)
    List<InventoryTransaction> findByProjectIdAndResourceIdOrderByTransactionDateDesc(Long projectId, Long resourceId);

    // 2. Cálculo del Stock Físico Actual (Stock = Suma de INBOUND - Suma de OUTBOUND)
    @Query("SELECT COALESCE(SUM(CASE WHEN t.transactionType = 'INBOUND' THEN t.quantity ELSE -t.quantity END), 0) " +
            "FROM InventoryTransaction t " +
            "WHERE t.project.id = :projectId AND t.resource.id = :resourceId")
    BigDecimal calculateCurrentStock(@Param("projectId") Long projectId, @Param("resourceId") Long resourceId);

    // 3. Gasto Real Integrado (Suma todas las salidas asociadas a una partida y recurso)
    @Query("SELECT COALESCE(SUM(t.quantity), 0) FROM InventoryTransaction t " +
            "WHERE t.projectItem.id = :projectItemId " +
            "AND t.resource.id = :resourceId " +
            "AND t.transactionType = 'OUTBOUND'")
    BigDecimal calculateConsumedQuantityByProjectItem(@Param("projectItemId") Long projectItemId, @Param("resourceId") Long resourceId);

    // 4. Obtener todas las transacciones de un proyecto (Para reportes)
    List<InventoryTransaction> findByProjectIdOrderByTransactionDateDesc(Long projectId);

    @Query("SELECT new com.retrofit.backend.dto.StockSummaryDTO(" +
            "t.project.id, " +
            "r.id, " +
            "r.name, " +
            "r.unit, " +
            "SUM(CASE WHEN t.transactionType = 'INBOUND' THEN t.quantity ELSE -t.quantity END)) " +
            "FROM InventoryTransaction t " +
            "JOIN t.resource r " +
            "WHERE t.project.id = :projectId " +
            "GROUP BY t.project.id, r.id, r.name, r.unit")
    List<StockSummaryDTO> getProjectStockSummary(@Param("projectId") Long projectId);
}
