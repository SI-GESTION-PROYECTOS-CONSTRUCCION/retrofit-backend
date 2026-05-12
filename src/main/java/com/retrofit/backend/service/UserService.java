package com.retrofit.backend.service;


import com.retrofit.backend.dto.AdminDTO;
import com.retrofit.backend.dto.UserCreateDTO;
import com.retrofit.backend.dto.UserDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {
    UserDTO registerAdmin(AdminDTO admin);
    UserDTO registerUser(UserCreateDTO dto);
    UserDTO updateUser(long id, UserCreateDTO dto);
    void deleteUser(long id);
    Page<UserDTO> getAllUsers(String search, String roleName, Pageable pageable);
    UserDTO getUserById(long id);
}