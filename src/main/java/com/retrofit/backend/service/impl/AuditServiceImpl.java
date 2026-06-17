package com.retrofit.backend.service.impl;

import com.retrofit.backend.dto.AuditLogResponseDto;
import com.retrofit.backend.dto.AuditStatsDto;
import com.retrofit.backend.model.AuditLog;
import com.retrofit.backend.model.User;
import com.retrofit.backend.repository.AuditLogRepository;
import com.retrofit.backend.repository.UserRepository;
import com.retrofit.backend.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    // Leer
    @Override
    public Page<AuditLogResponseDto> getAuditLogs(int page, int size, String search, String module, String action, String date) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "actionDate"));

        // Lógica para fechas
        LocalDateTime startDate = null;
        LocalDateTime endDate = null;
        if (date != null && !date.isEmpty()) {
            startDate = LocalDate.parse(date).atStartOfDay();
            endDate = LocalDate.parse(date).atTime(23, 59, 59);
        }

        // Limpiar "Todos" o "Todas" para que pasen como null a la BD
        String finalModule = (module == null || module.equalsIgnoreCase("Todos")) ? null : module;
        String finalAction = (action == null || action.equalsIgnoreCase("Todas")) ? null : action;
        String finalSearch = (search == null || search.trim().isEmpty()) ? null : search.trim();

        Page<AuditLog> logs = auditLogRepository.findWithFilters(finalSearch, finalModule, finalAction, startDate, endDate, pageable);

        return logs.map(log -> AuditLogResponseDto.builder()
                .logId("LOG-" + log.getId())
                .timestamp(log.getActionDate())
                .userName(log.getUser().getName())
                .userRole(log.getUser().getRole().getName())
                .action(log.getAction())
                .module(log.getAffectedTable())
                .description("Acción realizada en el registro ID: " + log.getRegisterId())
                .ipAddress(log.getIpAddress())
                .userAgent(log.getUserAgent())
                .oldData(log.getOldData())
                .newData(log.getNewData())
                .build());
    }

    @Override
    public AuditStatsDto getAuditStats() {
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfDay = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);

        // Definimos las acciones críticas generales (En cualquier módulo)
        List<String> accionesCriticas = Arrays.asList("DELETE", "LOGIN_FAILED");

        // Definimos el módulo ultra-sensible (Cualquier acción aquí será alerta)
        String moduloCritico = "Roles";

        return AuditStatsDto.builder()
                .todayEvents(auditLogRepository.countByActionDateBetween(startOfDay, endOfDay))
                .modifications(auditLogRepository.countByAction("UPDATE"))
                // Usamos nuestro nuevo contador inteligente
                .alerts(auditLogRepository.countAlerts(accionesCriticas, moduloCritico))
                .build();
    }



    // Escribir
    @Override
    public void logAction(String action, String module, Long recordId, Object oldData, Object newData) {
        try {
            // Obtener usuario actual del Token
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User currentUser = userRepository.findByUsername(username).orElse(null);

            // Convertir objetos a texto JSON
            String oldJson = oldData != null ? objectMapper.writeValueAsString(oldData) : "null";
            String newJson = newData != null ? objectMapper.writeValueAsString(newData) : "null";

            // Peticion HTTP
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
            // Ip y navegador
            String ip = request.getRemoteAddr();
            String userAgent = request.getHeader("User-Agent");

            AuditLog log = new AuditLog();
            log.setUser(currentUser);
            log.setAction(action);
            log.setAffectedTable(module);
            log.setRegisterId(recordId);
            log.setOldData(oldJson);
            log.setNewData(newJson);
            log.setActionDate(LocalDateTime.now());
            log.setIpAddress(ip != null ? ip : "Desconocida");
            log.setUserAgent(userAgent != null ? userAgent : "Sistema Interno");

            // Guardar
            auditLogRepository.save(log);

        } catch (Exception e) {
            System.err.println("Error guardando auditoría: " + e.getMessage());
        }
    }
}
