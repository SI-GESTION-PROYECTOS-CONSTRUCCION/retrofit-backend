package com.retrofit.backend.service.impl;

import com.retrofit.backend.annotation.AuditChange;
import com.retrofit.backend.dto.AdminDTO;
import com.retrofit.backend.dto.UserCreateDTO;
import com.retrofit.backend.dto.UserDTO;
import com.retrofit.backend.model.Admin;
import com.retrofit.backend.model.RoleE;
import com.retrofit.backend.model.User;
import com.retrofit.backend.repository.AdminRepository;
import com.retrofit.backend.repository.RoleRepository;
import com.retrofit.backend.repository.UserRepository;
import com.retrofit.backend.repository.WorkerRepository;
import com.retrofit.backend.service.AuditService;
import com.retrofit.backend.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final AdminRepository adminRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final WorkerRepository workerRepository;
    private final AuditService auditService;

    @Override
    public UserDTO registerAdmin(AdminDTO admin) {
        if (adminRepository.findByEmail(admin.getEmail()).isPresent()) {
            throw new IllegalArgumentException("User email already exists");
        }

        RoleE adminRole = roleRepository.findByName("ADMIN")
                .orElseThrow(() -> new RuntimeException("Rol ADMIN no existe en la base de datos"));

        Admin user = Admin.builder()
                .email(admin.getEmail())
                .password(passwordEncoder.encode(admin.getPassword()))
                .role(adminRole)
                .username(admin.getUsername())
                .name(admin.getName())
                .lastName(admin.getLastName())
                .active(true)
                .requirePasswordChange(true)
                .createdAt(Timestamp.valueOf(LocalDateTime.now()))
                .build();
        return mapToDTO(adminRepository.save(user));
    }

    @Override
    @AuditChange(action = "CREATE", module = "Usuarios")
    public UserDTO registerUser(UserCreateDTO dto) {

        RoleE roleEntity = roleRepository.findByName(dto.getRole())
                .orElseThrow(() -> new RuntimeException("Rol no encontrado: " + dto.getRole()));

        if (userRepository.findByUsername(dto.getUsername()).isPresent()) {
            throw new IllegalArgumentException("User username already exists");
        }

        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new IllegalArgumentException("User email already exists");
        }

        User userToSave;
        switch (dto.getRole()) {
            case "ADMIN":
                userToSave = Admin.builder().build();
                break;

            case "INGENIERO_RESIDENTE":
                userToSave = new User();
                break;

            case "ALMACENERO":
                userToSave = new User();
                break;

            default:
                throw new IllegalArgumentException("El rol " + dto.getRole() + " no tiene una entidad asociada.");
        }

        userToSave.setEmail(dto.getEmail());
        userToSave.setName(dto.getName());
        userToSave.setUsername(dto.getUsername());
        userToSave.setLastName(dto.getLastName());
        userToSave.setPassword(passwordEncoder.encode(dto.getPassword()));
        userToSave.setRole(roleEntity);
        userToSave.setActive(true);
        userToSave.setRequirePasswordChange(true);
        userToSave.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));

        User savedUser = userRepository.save(userToSave);

        return mapToDTO(savedUser);
    }

    @Override
    public Page<UserDTO> getAllUsers(String search, String roleName, Boolean active, Pageable pageable) {
        String finalSearch = (search == null) ? "" : search.trim();
        return userRepository.findWithFilters(finalSearch, roleName, active, pageable)
                .map(this::mapToDTO);
    }

    @Override
    public UserDTO updateUser(long id, UserCreateDTO dto) {
        User userFound = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado con ID: " + id));

        UserDTO estadoAnterior = mapToDTO(userFound);

        if (dto.getUsername() != null && !dto.getUsername().isBlank()) {
            if (!userFound.getUsername().equals(dto.getUsername())) {
                if (userRepository.findByUsername(dto.getUsername()).isPresent()) {
                    throw new IllegalArgumentException("User username already exists");
                }
                userFound.setUsername(dto.getUsername());
            }
        }

        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            if (!userFound.getEmail().equals(dto.getEmail())) {
                if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
                    throw new IllegalArgumentException("User email already exists");
                }
                userFound.setEmail(dto.getEmail());
            }
        }

        if (dto.getEmail() != null && !dto.getEmail().isBlank())
            userFound.setEmail(dto.getEmail());
        if (dto.getName() != null && !dto.getName().isBlank())
            userFound.setName(dto.getName());
        if (dto.getLastName() != null && !dto.getLastName().isBlank())
            userFound.setLastName(dto.getLastName());
        if (dto.getPassword() != null && !dto.getPassword().isBlank())
            userFound.setPassword(passwordEncoder.encode(dto.getPassword()));
        // Solo los usuarios ADMIN pueden cambiar a otro usuario a ADMIN
        if (dto.getRole() != null && !dto.getRole().isBlank()) {
            RoleE newRole = roleRepository.findByName(dto.getRole())
                    .orElseThrow(() -> new EntityNotFoundException("El rol '" + dto.getRole() + "' no existe."));

            if (newRole.getName().equals("ADMIN")) {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                String currentUsername = auth.getName();
                User currentUser = userRepository.findByUsername(currentUsername)
                        .orElseThrow(() -> new AccessDeniedException("Usuario actual no encontrado"));

                if (!currentUser.getRole().getName().equals("ADMIN")) {
                    throw new AccessDeniedException("No tienes permisos para asignar el rol de ADMINISTRADOR.");
                }
            }

            userFound.setRole(newRole);
        }

        workerRepository.findByUser(userFound).ifPresent(worker -> {
            // Si el nombre o apellido cambiaron en el User, se los pasamos al Worker
            if (dto.getName() != null && !dto.getName().isBlank())
                worker.setName(dto.getName());
            if (dto.getLastName() != null && !dto.getLastName().isBlank())
                worker.setLastName(dto.getLastName());
            workerRepository.save(worker);
        });

        userFound.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));

        UserDTO estadoNuevo = mapToDTO(userFound);
        auditService.logAction("UPDATE", "Usuarios", userFound.getId(), estadoAnterior, estadoNuevo);
        return mapToDTO(userRepository.save(userFound));
    }

    @Override
    @AuditChange(action = "DELETE", module = "Usuarios")
    public void deleteUser(long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));

        user.setActive(false);
        userRepository.save(user);
    }

    @Override
    public UserDTO getUserById(long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado con el ID: " + id));

        return mapToDTO(user);
    }

    @Override
    public void changePassword(String username, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado con el username: " + username));
        
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setRequirePasswordChange(false);
        user.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
        userRepository.save(user);
    }

    private UserDTO mapToDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .username(user.getUsername())
                .lastName(user.getLastName())
                .role(user.getRole().getName())
                .active(user.isActive())
                .requirePasswordChange(user.isRequirePasswordChange())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
