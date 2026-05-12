package com.retrofit.backend.repository;

import com.retrofit.backend.model.ProgressPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgressPhotoRepository extends JpaRepository<ProgressPhoto, Long> {
}
