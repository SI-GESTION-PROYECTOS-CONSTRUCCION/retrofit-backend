package com.retrofit.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationEventDto {
    private String id;
    private String title;
    private String message;
    private LocalDateTime timestamp;
}
