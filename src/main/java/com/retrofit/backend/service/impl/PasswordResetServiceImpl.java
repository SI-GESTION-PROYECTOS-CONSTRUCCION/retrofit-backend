package com.retrofit.backend.service.impl;

import com.retrofit.backend.dto.ForgotPasswordRequestDto;
import com.retrofit.backend.dto.ResetPasswordRequestDto;
import com.retrofit.backend.model.PasswordResetToken;
import com.retrofit.backend.model.User;
import com.retrofit.backend.repository.PasswordResetTokenRepository;
import com.retrofit.backend.repository.UserRepository;
import com.retrofit.backend.service.EmailService;
import com.retrofit.backend.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetServiceImpl implements PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    @Override
    @Transactional
    public void processForgotPassword(ForgotPasswordRequestDto request) {
        String cleanEmail = request.getEmail().trim().toLowerCase();
        Optional<User> optionalUser = userRepository.findByEmail(cleanEmail);

        if (optionalUser.isEmpty()) {
            log.warn("Solicitud de recuperación para correo inexistente: {}", cleanEmail);
            // Retorno silencioso para prevenir enumeración de usuarios
            return;
        }

        User user = optionalUser.get();
        if (!user.isActive()) {
            log.warn("Solicitud de recuperación para usuario inactivo: {}", cleanEmail);
            return;
        }

        tokenRepository.invalidateAllActiveTokensForUser(user);

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiryDate(Instant.now().plus(15, ChronoUnit.MINUTES))
                .createdAt(Instant.now())
                .used(false)
                .build();

        tokenRepository.save(resetToken);

        String resetUrl = frontendUrl + "/login?token=" + token;
        emailService.sendPasswordResetEmail(user.getEmail(), user.getName(), resetUrl);
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequestDto request) {
        PasswordResetToken resetToken = tokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new IllegalArgumentException("El enlace de restablecimiento es inválido o no existe."));

        if (resetToken.isUsed()) {
            throw new IllegalArgumentException("Este enlace ya ha sido utilizado. Solicita uno nuevo si lo necesitas.");
        }

        if (resetToken.isExpired()) {
            throw new IllegalArgumentException("El enlace ha expirado. Por seguridad, solicita uno nuevo.");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setRequirePasswordChange(false);
        user.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        userRepository.save(user);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);
        log.info("Contraseña restablecida exitosamente para el usuario: {}", user.getUsername());
    }
}
