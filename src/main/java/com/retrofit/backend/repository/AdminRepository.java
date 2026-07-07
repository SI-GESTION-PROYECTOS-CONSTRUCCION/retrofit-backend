package com.retrofit.backend.repository;

import com.retrofit.backend.model.Admin;
import com.retrofit.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Long> {
    Optional<User> findByEmail(String email);

    @Query("SELECT c FROM Admin c WHERE c.role.name = :roleName")
    List<Admin> findAdminByRoleName(@Param("roleName") String role);

    @Query("SELECT c FROM Admin c WHERE c.id = :id AND c.role.name = 'ADMIN'")
    Optional<Admin> findAdminById(@Param("id") long id);
}