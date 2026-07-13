package com.retrofit.backend.dto;

import jakarta.validation.Valid;
import lombok.Data;
import java.util.List;

@Data
public class BudgetSaveRequestDto {
    @jakarta.validation.constraints.PositiveOrZero(message = "El porcentaje de gastos generales no puede ser negativo")
    private Double generalExpensesPercentage;
    
    @jakarta.validation.constraints.PositiveOrZero(message = "El porcentaje de utilidad no puede ser negativo")
    private Double utilityPercentage;
    
    @Valid
    private List<ProjectItemRequestDto> items;
}
