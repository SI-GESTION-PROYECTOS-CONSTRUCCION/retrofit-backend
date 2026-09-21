package com.retrofit.backend.service.impl;

import com.retrofit.backend.dto.NotificationEventDto;
import com.retrofit.backend.service.NotificationStreamService;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class NotificationStreamServiceImpl implements NotificationStreamService {
    private final Set<SseEmitter> emitters = ConcurrentHashMap.newKeySet();

    @Override
    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        return emitter;
    }

    @Override
    public void publish(NotificationEventDto event) {
        emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event().name("notification").data(event));
            } catch (IOException | IllegalStateException exception) {
                emitters.remove(emitter);
            }
        });
    }
}
