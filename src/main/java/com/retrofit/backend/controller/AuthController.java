package com.retrofit.backend.controller;

import com.retrofit.backend.auth.AuthRequest;
import com.retrofit.backend.auth.AuthResponse;
import com.retrofit.backend.dto.AdminDTO;
import com.retrofit.backend.dto.UserCreateDTO;
import com.retrofit.backend.dto.UserDTO;
import com.retrofit.backend.dto.UserProfileDTO;
import com.retrofit.backend.model.User;
import com.retrofit.backend.repository.UserRepository;
import com.retrofit.backend.service.AuthService;
import com.retrofit.backend.service.UserService;
import com.retrofit.backend.auth.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetailsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;


import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final UserService userService;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @PostMapping("/registerAdmin")
    public ResponseEntity<UserDTO> register(@RequestBody AdminDTO request){
        return ResponseEntity.ok(userService.registerAdmin(request));
    }

    @GetMapping("/profile")
    public ResponseEntity<UserProfileDTO> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails.getUsername();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));


        List<String> permisos = user.getRole().getPermissions().stream()
                .map(permission -> permission.getName())
                .collect(Collectors.toList());

        UserProfileDTO dto = UserProfileDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .lastName(user.getLastName())
                .role(user.getRole().getName())
                .permissions(permisos)
                .build();

        return ResponseEntity.ok(dto);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request){
        final String jwt = authService.login(request);
        return ResponseEntity.ok(new AuthResponse(jwt));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String jwt = authHeader.substring(7);
            try {
                String username = jwtUtil.extractUsernameFromExpiredToken(jwt);
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                String newJwt = jwtUtil.generateToken(userDetails);
                return ResponseEntity.ok(new AuthResponse(newJwt));
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token inválido o firmas incorrectas");
            }
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token no proporcionado");
    }
}
