package com.retrofit.backend.repository;

import com.retrofit.backend.model.ProjectItemDependency;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProjectItemDependencyRepository extends JpaRepository<ProjectItemDependency, Long> {
    List<ProjectItemDependency> findBySuccessorProjectId(Long projectId);
    List<ProjectItemDependency> findBySuccessorId(Long successorId);
    void deleteBySuccessorId(Long successorId);
}
