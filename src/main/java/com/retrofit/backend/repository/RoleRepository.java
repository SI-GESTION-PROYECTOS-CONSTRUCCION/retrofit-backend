package com.retrofit.backend.repository;

import com.retrofit.backend.model.RoleE;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<RoleE, Long> {
    Optional<RoleE> findByName(String name);

    Optional<RoleE> findById(long id);
}
