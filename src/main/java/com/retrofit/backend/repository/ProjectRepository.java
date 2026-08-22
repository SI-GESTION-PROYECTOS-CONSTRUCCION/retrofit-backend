package com.retrofit.backend.repository;

import com.retrofit.backend.model.Project;
import com.retrofit.backend.enums.ProjectPriority;
import com.retrofit.backend.enums.ProjectStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {
        Optional<Project> findByCode(String code);

        boolean existsByCode(String code);

        @EntityGraph(attributePaths = {"manager"})
        @Query("SELECT p FROM Project p WHERE " +
                        "(:search = '' OR LOWER(p.code) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))) AND "
                        +
                        "(:priority IS NULL OR p.priority = :priority) AND " +
                        "(:status IS NULL OR p.status = :status)")
        Page<Project> findWithFilters(@Param("search") String search,
                        @Param("priority") ProjectPriority priority,
                        @Param("status") ProjectStatus status,
                        Pageable pageable);
}
