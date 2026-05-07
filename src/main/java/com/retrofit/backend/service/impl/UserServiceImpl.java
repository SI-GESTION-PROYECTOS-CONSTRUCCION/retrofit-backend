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
            throw new EntityExistsException("User with this dni already exists");
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
        if(userRepository.findByUsername(dto.getUsername()).isPresent()){
            throw new EntityExistsException("El username ya existe");
        }
        RoleE roleEntity = roleRepository.findByName(dto.getRole())
                .orElseThrow(() -> new RuntimeException("Rol no encontrado: " + dto.getRole()));
        User userToSave;
        switch (dto.getRole()) {
            case "ADMIN":
                userToSave = Admin.builder().build();
                break;

            default:
                throw new IllegalArgumentException("Tipo de rol no soportado para registro");
        }

        userToSave.setEmail(dto.getEmail());
        userToSave.setName(dto.getName());
        userToSave.setLastName(dto.getLastName());
        userToSave.setSex(dto.getSex());
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

        if (dto.getUsername() != null && !dto.getUsername().isEmpty()) {
            if (!userFound.getUsername().equals(dto.getUsername())) {
                if (userRepository.findByUsername(dto.getUsername()).isPresent()) {
                    throw new EntityExistsException("Ya existe otro usuario con el usuario: " + dto.getUsername());
                }
                userFound.setEmail(dto.getEmail());
            }
        }


        if (dto.getName() != null) userFound.setName(dto.getName());
        if (dto.getLastName() != null) userFound.setLastName(dto.getLastName());
        if (dto.getSex() != null) userFound.setSex(dto.getSex());


        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            userFound.setPassword(passwordEncoder.encode(dto.getPassword()));
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
                .sex(user.getSex())
                .role(user.getRole().getName())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}

