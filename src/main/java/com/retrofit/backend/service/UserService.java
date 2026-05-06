package com.retrofit.backend.service;


import com.retrofit.backend.dto.AdminDTO;
import com.retrofit.backend.dto.UserCreateDTO;
import com.retrofit.backend.dto.UserDTO;

import java.util.List;

public interface UserService {
    UserDTO registerAdmin(AdminDTO admin);
    UserDTO registerUser(UserCreateDTO dto);
    UserDTO updateUser(long id, UserCreateDTO dto);
    void deleteUser(long id);
    List<UserDTO> getUsersByRole(String roleName);
    UserDTO getUserById(long id);
}
