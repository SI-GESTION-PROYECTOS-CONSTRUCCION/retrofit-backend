package com.retrofit.backend.controller;


import com.retrofit.backend.dto.UserCreateDTO;
import com.retrofit.backend.dto.UserDTO;
import com.retrofit.backend.repository.UserRepository;

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

    @PostMapping
    public ResponseEntity<UserDTO> registerUser(@RequestBody UserCreateDTO request){
        return ResponseEntity.ok(userService.registerUser(request));
    }

    @GetMapping("/rol/{role}")
    public ResponseEntity<List<UserDTO>> getUserByRole(@PathVariable String role){
        return ResponseEntity.ok(userService.getUsersByRole(role));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id){
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> updateUser(@PathVariable Long id, @RequestBody UserCreateDTO dto){
        return ResponseEntity.ok(userService.updateUser(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id){
        userService.deleteUser(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<UserDTO>> getUsersByRole(
            @RequestParam(value = "roleName", defaultValue = "ALL", required = false) String roleName) {

        List<UserDTO> users = userService.getUsersByRole(roleName);
        return ResponseEntity.ok(users);
    }
}

