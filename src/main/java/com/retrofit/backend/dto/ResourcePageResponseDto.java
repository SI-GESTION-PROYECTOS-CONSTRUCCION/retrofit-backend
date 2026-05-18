package com.retrofit.backend.dto;
import java.util.List;
import lombok.Data;

@Data
public class ResourcePageResponseDto {

    private List<ProjectItemResourceResponseDto> content;

    private int totalPages;

    private long totalElements;

    private int currentPage;
}
