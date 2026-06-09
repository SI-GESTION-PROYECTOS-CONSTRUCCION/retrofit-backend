package com.retrofit.backend.aspect;

import com.retrofit.backend.annotation.AuditChange;
import com.retrofit.backend.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {
    private final AuditService auditService;

    @AfterReturning(pointcut = "@annotation(auditarCambio)", returning = "resultado")
    public void registrarAuditoria(JoinPoint joinPoint, AuditChange auditarCambio, Object resultado) {
        try {
            Long id = extraerId(resultado);

            auditService.logAction(
                    auditarCambio.action(),
                    auditarCambio.module(),
                    id,
                    null,
                    resultado
            );

        } catch (Exception e) {
            System.err.println("Error silencioso en AOP de Auditoría: " + e.getMessage());
        }
    }

    private Long extraerId(Object objeto) {
        if (objeto == null) return null;
        try {
            Field idField = objeto.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            return (Long) idField.get(objeto);
        } catch (Exception e) {
            return null;
        }
    }
}
