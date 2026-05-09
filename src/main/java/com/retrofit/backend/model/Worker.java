package com.retrofit.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Entity
@Table (name = "workers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Worker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    @JoinColumn(name = "usuario_id", referencedColumnName = "id")
    private User user;

    private String position;

    @Column(unique = true, length = 8)
    private String dni;

    @Column(unique = true)
    private String phone;

    private boolean active;

    private Timestamp createdAt;
    private Timestamp updatedAt;

    @Column(name = "project_id")
    private Long projectId;
}
