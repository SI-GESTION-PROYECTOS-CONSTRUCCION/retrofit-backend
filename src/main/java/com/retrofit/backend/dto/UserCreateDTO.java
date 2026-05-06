package com.retrofit.backend.dto;

import lombok.Data;

@Data
public class UserCreateDTO {
    private String email;
    private String name;
    private String username;
    private String lastName;
    private String sex;
    private String password;
    private String role;
}
