package com.retrofit.backend.service;

import com.retrofit.backend.model.RefreshToken;
import com.retrofit.backend.repository.RefreshTokenRepository;
import com.retrofit.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    
    // Configura la duración del refresh token a 7 días (604800000 milisegundos)
    private final Long refreshTokenDurationMs = 604800000L; 

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    @Transactional
    public RefreshToken createRefreshToken(String username) {
        var user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        // Buscar token existente o crear uno nuevo
        RefreshToken refreshToken = refreshTokenRepository.findByUser(user).orElse(new RefreshToken());

        refreshToken.setUser(user);
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));
        refreshToken.setToken(UUID.randomUUID().toString());

        refreshToken = refreshTokenRepository.save(refreshToken);
        return refreshToken;
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            throw new RuntimeException("El Refresh Token expiró. Inicie sesión de nuevo.");
        }
        return token;
    }

    @Transactional
    public int deleteByUsername(String username) {
        var user = userRepository.findByUsername(username).orElse(null);
        if (user != null) {
            var token = refreshTokenRepository.findByUser(user);
            if (token.isPresent()) {
                refreshTokenRepository.delete(token.get());
                return 1;
            }
        }
        return 0;
    }
}
