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
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final UserService userService;
    private final UserRepository userRepository;

    @PostMapping("/registerAdmin")
    public ResponseEntity<UserDTO> register(@RequestBody AdminDTO request){
        return ResponseEntity.ok(userService.registerAdmin(request));
    }

    @GetMapping("/profile")
    public ResponseEntity<UserProfileDTO> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails.getUsername();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        UserProfileDTO dto = UserProfileDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .lastName(user.getLastName())
                .role(user.getRole().getName())
                .build();

        return ResponseEntity.ok(dto);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request){
        final String jwt = authService.login(request);
        return ResponseEntity.ok(new AuthResponse(jwt));
    }

}
