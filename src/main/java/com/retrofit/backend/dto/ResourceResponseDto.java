package com.retrofit.backend.dto;

import lombok.Data;

@Data
public class ResourceResponseDto {
    private Long id;
    private String name;
    private String unit;
    private Double basePrice;
}