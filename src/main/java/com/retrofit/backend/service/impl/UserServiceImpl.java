package com.retrofit.backend.service.impl;

import com.retrofit.backend.dto.AdminDTO;
import com.retrofit.backend.dto.UserCreateDTO;
import com.retrofit.backend.dto.UserDTO;
import com.retrofit.backend.model.Admin;
import com.retrofit.backend.model.RoleE;
import com.retrofit.backend.model.User;
import com.retrofit.backend.repository.AdminRepository;
import com.retrofit.backend.repository.RoleRepository;
import com.retrofit.backend.repository.UserRepository;
import com.retrofit.backend.service.UserService;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final AdminRepository adminRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    @Override
    public UserDTO registerAdmin(AdminDTO admin) {
        if(adminRepository.findByEmail(admin.getEmail()).isPresent()){
            throw new EntityExistsException("El email ya esta registrado");
        };

        RoleE adminRole = roleRepository.findByName("ADMIN")
                .orElseThrow(() -> new RuntimeException("Rol ADMIN no existe en la base de datos"));


        Admin user = Admin.builder()
                .email(admin.getEmail())
                .password(passwordEncoder.encode(admin.getPassword()))
                .role(adminRole)
                .username(admin.getUsername())
                .name(admin.getName())
                .createdAt(Timestamp.valueOf(LocalDateTime.now()))
                .build();

        System.out.println(user.getRole());
        return mapToDTO(adminRepository.save(user));
    }


    @Override
    public UserDTO registerUser(UserCreateDTO dto) {

        RoleE roleEntity = roleRepository.findByName(dto.getRole())
                .orElseThrow(() -> new RuntimeException("Rol no encontrado: " + dto.getRole()));

        if(userRepository.findByUsername(dto.getUsername()).isPresent()){
            throw new EntityExistsException("El username ya esta registrado");
        }

        if(userRepository.findByEmail(dto.getEmail()).isPresent()){
            throw new EntityExistsException("El email ya esta registrado");
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
        userToSave.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));

        User savedUser = userRepository.save(userToSave);

        return mapToDTO(savedUser);
    }


    @Override
    public List<UserDTO> getUsersByRole(String roleName) {

        if (roleName == null || roleName.equals("ALL")) {
            return userRepository.findAll().stream()
                    .map(this::mapToDTO)
                    .collect(Collectors.toList());
        }

        return userRepository.findByRole_Name(roleName).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public UserDTO updateUser(long id, UserCreateDTO dto) {
        User userFound = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado con ID: " + id));

        if (dto.getUsername() != null && !dto.getUsername().isBlank()) {
            if (!userFound.getUsername().equals(dto.getUsername())) {
                if (userRepository.findByUsername(dto.getUsername()).isPresent()) {
                    throw new EntityExistsException("El nombre de usuario '" + dto.getUsername() + "' ya está en uso.");
                }
                userFound.setUsername(dto.getUsername());
            }
        }

        if (dto.getEmail() != null && !dto.getEmail().isBlank()) userFound.setEmail(dto.getEmail());
        if (dto.getName() != null && !dto.getName().isBlank()) userFound.setName(dto.getName());
        if (dto.getLastName() != null && !dto.getLastName().isBlank()) userFound.setLastName(dto.getLastName());
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) userFound.setPassword(passwordEncoder.encode(dto.getPassword()));
        // Solo los usuarios ADMIN pueden cambiar a otro usuario a ADMIN
        if (dto.getRole() != null && !dto.getRole().isBlank()) {
            RoleE newRole = roleRepository.findByName(dto.getRole())
                    .orElseThrow(() -> new EntityNotFoundException("El rol '" + dto.getRole() + "' no existe."));

            if (newRole.getName().equals("ADMIN")) {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                boolean isAdmin = auth.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ADMIN"));

                if (!isAdmin) {
                    throw new AccessDeniedException("No tienes permisos para asignar el rol de ADMINISTRADOR.");
                }
            }

            userFound.setRole(newRole);
        }

        userFound.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));

        return mapToDTO(userRepository.save(userFound));
    }

    @Override
    public void deleteUser(long id) {
        if (!userRepository.existsById(id)) {
            throw new EntityNotFoundException("Usuario no encontrado con ID: " + id);
        }
        userRepository.deleteById(id);
    }

    @Override
    public UserDTO getUserById(long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado con el ID: " + id));

        return mapToDTO(user);
    }

    private UserDTO mapToDTO(User user){
        return UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .username(user.getUsername())
                .lastName(user.getLastName())
                .role(user.getRole().getName())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}

