package com.retrofit.backend.repository;

import com.retrofit.backend.model.LaborCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LaborCategoryRepository extends JpaRepository<LaborCategory, Long> {}