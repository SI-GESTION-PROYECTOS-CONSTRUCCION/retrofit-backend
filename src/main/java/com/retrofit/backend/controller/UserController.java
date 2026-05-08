package com.retrofit.backend.controller;

import com.retrofit.backend.dto.UserDTO;
import com.retrofit.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<UserDTO>> getUsersByRole(
            @RequestParam(value = "roleName", defaultValue = "ALL", required = false) String roleName) {

        List<UserDTO> users = userService.getUsersByRole(roleName);
        return ResponseEntity.ok(users);
    }
}