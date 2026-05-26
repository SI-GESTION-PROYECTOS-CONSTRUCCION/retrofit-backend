package com.retrofit.backend.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class GanttUpdateDto {
    private LocalDate startDate;
    private LocalDate endDate;
    private Long predecessorId;

}
