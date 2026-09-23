package com.retrofit.backend.dto;

import com.retrofit.backend.model.DependencyType;
import lombok.Data;

@Data
public class GanttDependencyDto {
    private Long predecessorId;
    private DependencyType type = DependencyType.FINISH_TO_START;
}
