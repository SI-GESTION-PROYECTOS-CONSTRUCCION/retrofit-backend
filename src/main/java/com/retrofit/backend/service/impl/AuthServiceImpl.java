package com.retrofit.backend.service.impl;

import com.retrofit.backend.auth.AuthRequest;
import com.retrofit.backend.auth.jwt.JwtUtil;
import com.retrofit.backend.model.User;
import com.retrofit.backend.repository.UserRepository;
import com.retrofit.backend.service.AuditService;
import com.retrofit.backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {
    private final AuthenticationManager authenticationManager;
    private final UserDetailsServiceImpl userDetailsService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final AuditService auditService;

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCK_TIME_DURATION_MS = 15 * 60 * 1000L; // 15 minutos

    public String login(AuthRequest request) {
        String username = request.getUsername() != null ? request.getUsername().trim() : "";
        User user = userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElse(null);

        // 1. Verificar si la cuenta está bloqueada temporalmente
        if (user != null && user.getLockTime() != null) {
            long lockTimeMillis = user.getLockTime().getTime();
            long currentTimeMillis = System.currentTimeMillis();
            if (lockTimeMillis + LOCK_TIME_DURATION_MS > currentTimeMillis) {
                long remainingMinutes = ((lockTimeMillis + LOCK_TIME_DURATION_MS - currentTimeMillis) / 60000) + 1;
                auditService.logAuthenticationEvent("LOGIN_FAILED", username, false,
                        "Acceso bloqueado: cuenta bloqueada temporalmente. Minutos restantes: " + remainingMinutes);
                throw new LockedException(
                        "Cuenta bloqueada temporalmente por seguridad tras 5 intentos fallidos. Intente nuevamente en "
                                + remainingMinutes + " minuto(s).");
            } else {
                // El bloqueo expiró, restablecer contador
                user.setLockTime(null);
                user.setFailedAttempts(0);
                userRepository.save(user);
            }
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user != null ? user.getUsername() : username,
                            request.getPassword()));
        } catch (BadCredentialsException ex) {
            if (user != null) {
                int newAttempts = user.getFailedAttempts() + 1;
                user.setFailedAttempts(newAttempts);

                if (newAttempts >= MAX_FAILED_ATTEMPTS) {
                    user.setLockTime(new Timestamp(System.currentTimeMillis()));
                    userRepository.save(user);
                    auditService.logAuthenticationEvent("LOGIN_FAILED", username, false,
                            "Cuenta bloqueada por 15 minutos tras 5 intentos fallidos consecutivos");
                    throw new LockedException(
                            "Su cuenta ha sido bloqueada temporalmente por 15 minutos debido a 5 intentos fallidos consecutivos.");
                } else {
                    userRepository.save(user);
                    auditService.logAuthenticationEvent("LOGIN_FAILED", username, false,
                            "Intento fallido " + newAttempts + " de " + MAX_FAILED_ATTEMPTS);
                    throw new BadCredentialsException(
                            "Usuario o contraseña incorrectos");
                }
            } else {
                auditService.logAuthenticationEvent("LOGIN_FAILED", username, false, "Usuario no encontrado");
                throw new BadCredentialsException("Usuario o contraseña incorrectos");
            }
        } catch (Exception ex) {
            auditService.logAuthenticationEvent("LOGIN_FAILED", username, false,
                    "Fallo en autenticación: " + ex.getMessage());
            throw ex;
        }

        // Si la autenticación fue exitosa, restablecer contadores de intento fallido
        if (user != null && (user.getFailedAttempts() > 0 || user.getLockTime() != null)) {
            user.setFailedAttempts(0);
            user.setLockTime(null);
            userRepository.save(user);
        }

        final UserDetails userDetails = userDetailsService
                .loadUserByUsername(user != null ? user.getUsername() : username);
        final String jwt = jwtUtil.generateToken(userDetails);

        // Registrar LOGIN exitoso en auditoría
        auditService.logAuthenticationEvent("LOGIN", userDetails.getUsername(), true,
                "Inicio de sesión seguro completado");

        return jwt;
    }
}
