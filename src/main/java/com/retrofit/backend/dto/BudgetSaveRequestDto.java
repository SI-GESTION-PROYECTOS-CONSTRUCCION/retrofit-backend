package com.retrofit.backend.dto;

import jakarta.validation.Valid;
import lombok.Data;
import java.util.List;

@Data
public class BudgetSaveRequestDto {
    private Double generalExpensesPercentage;
    private Double utilityPercentage;
    
    @Valid
    private List<ProjectItemRequestDto> items;
}
