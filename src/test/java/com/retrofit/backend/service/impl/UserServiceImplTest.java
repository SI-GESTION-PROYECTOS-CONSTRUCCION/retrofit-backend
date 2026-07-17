package com.retrofit.backend.service.impl;

import com.retrofit.backend.dto.AdminDTO;
import com.retrofit.backend.dto.UserDTO;
import com.retrofit.backend.model.Admin;
import com.retrofit.backend.model.RoleE;
import com.retrofit.backend.repository.AdminRepository;
import com.retrofit.backend.repository.RoleRepository;
import com.retrofit.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceImplTest {

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private AdminDTO adminDTO;
    private RoleE adminRole;

    @BeforeEach
    void setUp() {
        adminDTO = new AdminDTO();
        adminDTO.setEmail("admin@retrofit.com");
        adminDTO.setPassword("password123");
        adminDTO.setUsername("admin");
        adminDTO.setName("Admin Name");
        adminDTO.setLastName("Admin LastName");

        adminRole = new RoleE();
        adminRole.setId(1L);
        adminRole.setName("ADMIN");
    }

    @Test
    void testRegisterAdmin_Success() {
        when(adminRepository.findByEmail(adminDTO.getEmail())).thenReturn(Optional.empty());
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(adminRole));
        when(passwordEncoder.encode(adminDTO.getPassword())).thenReturn("encodedPassword");

        Admin savedAdmin = new Admin();
        savedAdmin.setId(1L);
        savedAdmin.setEmail(adminDTO.getEmail());
        savedAdmin.setUsername(adminDTO.getUsername());
        savedAdmin.setName(adminDTO.getName());
        savedAdmin.setRole(adminRole);
        savedAdmin.setActive(true);

        when(adminRepository.save(any(Admin.class))).thenReturn(savedAdmin);

        UserDTO result = userService.registerAdmin(adminDTO);

        assertNotNull(result);
        assertEquals("admin@retrofit.com", result.getEmail());
        assertEquals("admin", result.getUsername());
        assertTrue(result.isActive());
        verify(adminRepository, times(1)).save(any(Admin.class));
    }

    @Test
    void testRegisterAdmin_EmailAlreadyExists() {
        when(adminRepository.findByEmail(adminDTO.getEmail())).thenReturn(Optional.of(new Admin()));

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.registerAdmin(adminDTO);
        });

        assertEquals("User email already exists", exception.getMessage());
        verify(adminRepository, never()).save(any(Admin.class));
    }

    @Test
    void testRegisterAdmin_RoleNotFound() {
        when(adminRepository.findByEmail(adminDTO.getEmail())).thenReturn(Optional.empty());
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.empty());

        Exception exception = assertThrows(RuntimeException.class, () -> {
            userService.registerAdmin(adminDTO);
        });

        assertEquals("Rol ADMIN no existe en la base de datos", exception.getMessage());
        verify(adminRepository, never()).save(any(Admin.class));
    }
}
