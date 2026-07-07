package com.retrofit.backend.repository;

import com.retrofit.backend.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
        Optional<User> findByUsername(String username);

        Optional<User> findByEmail(String email);

        List<User> findByRole_Name(String roleName);

        @Query("SELECT u FROM User u WHERE " +
                        "(:search = '' OR LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                        "LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
                        "(:roleName = 'ALL' OR u.role.name = :roleName) AND " +
                        "(:active IS NULL OR u.active = :active)")
        Page<User> findWithFilters(@Param("search") String search,
                        @Param("roleName") String roleName,
                        @Param("active") Boolean active,
                        Pageable pageable);
}