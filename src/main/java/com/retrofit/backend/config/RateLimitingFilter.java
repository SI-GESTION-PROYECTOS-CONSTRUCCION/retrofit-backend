package com.retrofit.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimiterService rateLimiterService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Permitir preflight CORS sin consumir tokens
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String servletPath = request.getServletPath();

        // Omitir recursos públicos estáticos y documentación Swagger
        if (servletPath.startsWith("/public/") ||
            servletPath.startsWith("/swagger-ui") ||
            servletPath.startsWith("/v3/api-docs")) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = extractClientIp(request);
        boolean isAuthEndpoint = isSensitiveAuthEndpoint(servletPath);

        ConsumptionProbe probe = isAuthEndpoint
                ? rateLimiterService.tryConsumeAuth(clientIp)
                : rateLimiterService.tryConsumeApi(clientIp);

        if (probe.isConsumed()) {
            response.setHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            filterChain.doFilter(request, response);
        } else {
            long retryAfterSeconds = Math.max(1, (probe.getNanosToWaitForRefill() + 999_999_999L) / 1_000_000_000L);
            log.warn("Rate limit excedido para IP [{}] en ruta [{}] (Auth: {}). Reintentar en {}s",
                    clientIp, servletPath, isAuthEndpoint, retryAfterSeconds);

            response.setStatus(429); // HTTP 429 Too Many Requests
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
            response.setHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(retryAfterSeconds));
            response.setHeader("X-Rate-Limit-Remaining", "0");

            Map<String, Object> errorBody = new LinkedHashMap<>();
            errorBody.put("timestamp", LocalDateTime.now().toString());
            errorBody.put("status", 429);
            errorBody.put("error", "Too Many Requests");
            errorBody.put("message", isAuthEndpoint
                    ? "Demasiados intentos de acceso desde esta dirección IP. Por favor espere " + retryAfterSeconds + " segundos antes de reintentar."
                    : "Límite de solicitudes de la API excedido. Por favor espere " + retryAfterSeconds + " segundos antes de continuar.");
            errorBody.put("retryAfterSeconds", retryAfterSeconds);
            errorBody.put("path", request.getRequestURI());

            response.getWriter().write(objectMapper.writeValueAsString(errorBody));
        }
    }

    private boolean isSensitiveAuthEndpoint(String path) {
        return path.equals("/auth/login") ||
               path.equals("/auth/forgot-password") ||
               path.equals("/auth/reset-password");
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }
        return request.getRemoteAddr();
    }
}
