package com.retrofit.backend.repository;

import com.retrofit.backend.model.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


@Repository
public interface ResourceRepository extends JpaRepository<Resource, Long> {
    @Query("SELECT r FROM Resource r WHERE " +
            "(:resourceType = 'ALL' " +
            " OR (:resourceType = 'LABOR' AND TYPE(r) = LaborCategory) " +
            " OR (:resourceType = 'MATERIAL' AND TYPE(r) = Material) " +
            " OR (:resourceType = 'EQUIPMENT' AND TYPE(r) = Equipment)) " +
            "AND (:search = '' OR LOWER(r.name) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Resource> findFilteredResources(
            @Param("resourceType") String resourceType,
            @Param("search") String search,
            Pageable pageable);

    @Query(value = "SELECT " +
            "CASE " +
            "  WHEN EXISTS (SELECT 1 FROM labor_categories WHERE id = :resourceId) THEN 'LABOR' " +
            "  WHEN EXISTS (SELECT 1 FROM equipments WHERE id = :resourceId) THEN 'EQUIPMENT' " +
            "  ELSE 'MATERIAL' " +
            "END", nativeQuery = true)
    String findResourceTypeNative(@Param("resourceId") Long resourceId);
}
