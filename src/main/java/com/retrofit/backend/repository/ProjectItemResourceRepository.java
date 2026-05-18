package com.retrofit.backend.repository;

import com.retrofit.backend.model.ProjectItemResource;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProjectItemResourceRepository extends JpaRepository<ProjectItemResource, Long> {
    List<ProjectItemResource> findByProjectItemId(Long projectItemId);
}