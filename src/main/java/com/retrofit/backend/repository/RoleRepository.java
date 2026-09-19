package com.retrofit.backend.repository;

import com.retrofit.backend.model.RoleE;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<RoleE, Long> {
    @EntityGraph(attributePaths = {"permissions"})
    Optional<RoleE> findByName(String name);

    @Override
    @EntityGraph(attributePaths = {"permissions"})
    Optional<RoleE> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"permissions"})
    List<RoleE> findAll();
}
