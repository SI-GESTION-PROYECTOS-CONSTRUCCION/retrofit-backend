package com.retrofit.backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name="admin")
@Data
@NoArgsConstructor
@SuperBuilder
public class Admin extends User{
}
