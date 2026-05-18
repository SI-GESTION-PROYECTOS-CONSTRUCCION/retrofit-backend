package com.retrofit.backend.repository;

import com.retrofit.backend.model.ProjectItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectItemRepository extends JpaRepository<ProjectItem, Long> {
    List<ProjectItem> findByProjectIdOrderByItemOrderAsc(Long projectId);
}
