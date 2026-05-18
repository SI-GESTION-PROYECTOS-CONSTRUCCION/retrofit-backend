package com.retrofit.backend.dto;

import lombok.Data;

@Data
public class ProjectItemResourceResponseDto {
    private Long id;
    private Long resourceId;
    private String resourceName;
    private String resourceUnit;
    private Double resourceBasePrice;

    // Necesitamos saber qué tipo es para agruparlos en Angular (Mano de Obra, Material, Equipo)
    private String resourceType;

    private Double squad;
    private Double quantity;
    private Double partialPrice;
}