package com.retrofit.backend.service;

import com.retrofit.backend.dto.NotificationEventDto;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface NotificationStreamService {
    SseEmitter subscribe();
    void publish(NotificationEventDto event);
}
