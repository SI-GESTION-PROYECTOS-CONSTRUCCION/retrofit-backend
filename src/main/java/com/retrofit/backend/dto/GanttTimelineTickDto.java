package com.retrofit.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GanttTimelineTickDto {
    private String label;
    private String positionStyle;
    private String labelStyle;
}
