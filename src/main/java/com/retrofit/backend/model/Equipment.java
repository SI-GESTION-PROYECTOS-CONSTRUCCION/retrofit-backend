package com.retrofit.backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "equipments")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class Equipment extends Resource {
}