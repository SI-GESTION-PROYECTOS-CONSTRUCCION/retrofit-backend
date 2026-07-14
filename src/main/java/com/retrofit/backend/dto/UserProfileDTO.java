package com.retrofit.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserProfileDTO {
    private Long id;
    private String email;
    private String username;
    private String name;
    private String lastName;
    private String role;
    private List<String> permissions;
    private boolean requirePasswordChange;
}
