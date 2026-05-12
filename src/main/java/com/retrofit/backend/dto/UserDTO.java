package com.retrofit.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {
    private long id;
    private String email;
    private String name;
    private String username;
    private String lastName;
    private String role;
    private boolean active;
    private Timestamp createdAt;
    private Timestamp updatedAt;
}