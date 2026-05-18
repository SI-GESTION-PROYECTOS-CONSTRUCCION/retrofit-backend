package com.retrofit.backend.repository;

import com.retrofit.backend.model.ProgressReportResource;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgressReportResourceRepository extends JpaRepository<ProgressReportResource, Long> {
}