package com.retrofit.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CriticalItemDto {
    private Long itemId;
    private String itemCode;
    private String description;
    private Double earnedValue; // Lo que debio costar segun el avance
    private Double actualCost; // Lo que realmente se gasto
    private Double lossAmount; // Cuanta plata se esta perdiendo en esta partida
}
