package com.retrofit.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.sql.Timestamp;

@Entity
@Table(name="users")
@Inheritance(strategy = InheritanceType.JOINED)
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private String password;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private RoleE role;

    @Column(unique = false, columnDefinition = "TEXT")
    private String name;

    @Column(unique = false)
    private String lastName;

    @Column(unique = true, columnDefinition = "TEXT")
    private String username;

    @Column(unique = true)
    private String email;

    @Column(unique = false)
    private boolean active;

    @Column(unique = false, columnDefinition = "boolean default true")
    @Builder.Default
    private boolean requirePasswordChange = true;

    @Column(unique = false)
    private Timestamp createdAt;

    @Column(unique = false)
    private Timestamp updatedAt;
}
