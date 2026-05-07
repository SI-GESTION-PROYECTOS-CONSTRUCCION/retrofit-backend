package com.retrofit.backend.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class AdminDTO {
    private String email;
    private String username;
    private String password;
    private String role;
    private String name;
}

