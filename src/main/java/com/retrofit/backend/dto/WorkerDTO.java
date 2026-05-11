package com.retrofit.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WorkerDTO {
    private Long id;
    private String dni;
    private String position;
    private String phone;
    private boolean active;
    private String username;
    private String name;
    private String lastName;
    private String email;
    private String roleName;
    private boolean hasAccessAccount;
}
