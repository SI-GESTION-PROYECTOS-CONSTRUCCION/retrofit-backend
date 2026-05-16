package com.retrofit.backend.repository;

import com.retrofit.backend.model.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {}
