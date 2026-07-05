package com.retrofit.backend.repository;

import com.retrofit.backend.model.User;
import com.retrofit.backend.model.Worker;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkerRepository extends JpaRepository<Worker, Long> {
    Optional<Worker> findByDni(String dni);
    Optional<Worker> findByUser(User user);
    boolean existsByDni(String dni);
    boolean existsByPhone(String phone);
    @Query("SELECT w FROM Worker w WHERE " +
            "(:search = '' OR LOWER(w.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(w.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(w.dni) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
            "(:active IS NULL OR w.active = :active)")
    Page<Worker> findWithFilters(@Param("search") String search, @Param("active") Boolean active, Pageable pageable);
    @Query("SELECT w FROM Worker w WHERE w.id NOT IN " +
            "(SELECT pa.worker.id FROM ProjectAssignment pa WHERE pa.active = true) " +
            "AND w.active = true")
    List<Worker> findAvailableWorkers();
}
