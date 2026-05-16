package com.retrofit.backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "labor_categories")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class LaborCategory extends Resource {
}