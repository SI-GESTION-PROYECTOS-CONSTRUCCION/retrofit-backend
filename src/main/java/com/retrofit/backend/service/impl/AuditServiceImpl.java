package com.retrofit.backend.service.impl;

import com.retrofit.backend.dto.AuditLogResponseDto;
import com.retrofit.backend.dto.AuditStatsDto;
import com.retrofit.backend.dto.NotificationEventDto;
import com.retrofit.backend.model.AuditLog;
import com.retrofit.backend.model.User;
import com.retrofit.backend.repository.AuditLogRepository;
import com.retrofit.backend.repository.UserRepository;
import com.retrofit.backend.service.AuditService;
import com.retrofit.backend.service.NotificationStreamService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final NotificationStreamService notificationStreamService;

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
            // Obtener usuario actual del Token de manera segura
            String username = null;
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                username = SecurityContextHolder.getContext().getAuthentication().getName();
            }
            User currentUser = null;
            if (username != null && !username.isBlank() && !username.equals("anonymousUser")) {
                currentUser = userRepository.findByUsername(username).orElse(null);
            }

            // Convertir objetos a texto JSON
            String oldJson = oldData != null ? objectMapper.writeValueAsString(oldData) : "null";
            String newJson = newData != null ? objectMapper.writeValueAsString(newData) : "null";

            String ip = extractClientIp();
            String userAgent = extractUserAgent();

            AuditLog log = new AuditLog();
            log.setUser(currentUser);
            log.setAction(action);
            log.setAffectedTable(module);
            log.setRegisterId(recordId);
            log.setOldData(oldJson);
            log.setNewData(newJson);
            log.setActionDate(LocalDateTime.now());
            log.setIpAddress(ip);
            log.setUserAgent(userAgent);

            auditLogRepository.save(log);
            notificationStreamService.publish(NotificationEventDto.builder()
                    .id("LOG-" + log.getId())
                    .title(notificationTitle(action))
                    .message(currentUser != null ? currentUser.getName() + ": " + module : module)
                    .timestamp(log.getActionDate())
                    .build());

        } catch (Exception e) {
            System.err.println("Error guardando auditoría: " + e.getMessage());
        }
    }

    @Override
    public void logAuthenticationEvent(String action, String username, boolean success, String details) {
        try {
            User currentUser = null;
            if (username != null && !username.isBlank()) {
                currentUser = userRepository.findByUsername(username)
                        .or(() -> userRepository.findByEmail(username))
                        .orElse(null);
            }

            String ip = extractClientIp();
            String userAgent = extractUserAgent();

            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("identificador", username);
            dataMap.put("resultado", success ? "AUTORIZADO / EXITOSO" : "BLOQUEADO / DENEGADO");
            dataMap.put("detalle", details);
            String newJson = objectMapper.writeValueAsString(dataMap);

            AuditLog log = new AuditLog();
            log.setUser(currentUser);
            log.setAction(action);
            log.setAffectedTable("Autenticación");
            log.setRegisterId(currentUser != null ? currentUser.getId() : null);
            log.setOldData(null);
            log.setNewData(newJson);
            log.setActionDate(LocalDateTime.now());
            log.setIpAddress(ip);
            log.setUserAgent(userAgent);

            auditLogRepository.save(log);

            notificationStreamService.publish(NotificationEventDto.builder()
                    .id("LOG-" + log.getId())
                    .title(success ? "Acceso al Sistema" : "Alerta de Seguridad (Auth)")
                    .message(username + ": " + details)
                    .timestamp(log.getActionDate())
                    .build());
        } catch (Exception e) {
            System.err.println("Error guardando auditoría de autenticación: " + e.getMessage());
        }
    }

    @Override
    public List<AuditLog> getAuditLogsForReport(
            String search,
            String module,
            String action,
            String date,
            LocalDate startDate,
            LocalDate endDate) {

        LocalDateTime startDateTime = null;
        LocalDateTime endDateTime = null;

        if (date != null && !date.isBlank()) {
            LocalDate parsedDate = LocalDate.parse(date.trim());
            startDateTime = parsedDate.atStartOfDay();
            endDateTime = parsedDate.atTime(23, 59, 59);
        } else {
            if (startDate != null) startDateTime = startDate.atStartOfDay();
            if (endDate != null) endDateTime = endDate.atTime(23, 59, 59);
        }

        String finalModule = (module == null || module.equalsIgnoreCase("Todos")) ? null : module.trim();
        String finalAction = (action == null || action.equalsIgnoreCase("Todas")) ? null : action.trim();
        String finalSearch = (search == null || search.trim().isBlank()) ? null : search.trim();

        return auditLogRepository.findListWithFilters(
                finalSearch, finalModule, finalAction, startDateTime, endDateTime);
    }

    private String extractClientIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String remoteAddr = request.getHeader("X-Forwarded-For");
                if (remoteAddr == null || remoteAddr.isBlank()) {
                    remoteAddr = request.getRemoteAddr();
                } else {
                    remoteAddr = remoteAddr.split(",")[0].trim();
                }
                if (remoteAddr != null && !remoteAddr.isBlank()) return remoteAddr;
            }
        } catch (Exception ignored) {}
        return "Desconocida";
    }

    private String extractUserAgent() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String ua = request.getHeader("User-Agent");
                if (ua != null && !ua.isBlank()) return ua;
            }
        } catch (Exception ignored) {}
        return "Sistema Interno";
    }

    private String notificationTitle(String action) {
        return switch (action) {
            case "CREATE" -> "Nuevo registro";
            case "UPDATE" -> "Información actualizada";
            case "DELETE" -> "Registro eliminado";
            case "EXPORT" -> "Reporte generado";
            case "LOGIN" -> "Inicio de sesión seguro";
            case "LOGIN_FAILED" -> "Intento no autorizado bloqueado";
            default -> "Actividad registrada";
        };
    }
}
