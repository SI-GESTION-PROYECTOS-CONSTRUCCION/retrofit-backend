package com.retrofit.backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "materials")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class Material extends Resource {
    @Override
    @Transient
    public String fetchResourceType() {
        return "MATERIAL";
    }
}