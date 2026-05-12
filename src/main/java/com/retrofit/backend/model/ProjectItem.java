package com.retrofit.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "project_items")
@Data
public class ProjectItem {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(length = 50)
    private String code;

    @Column(nullable = false, length = 150)
    private String description;

    @Column(length = 20)
    private String unit; // ej: m2, m3, und

    @Column(name = "total_quantity")
    private Double totalQuantity; // metrado_total

    @Column(name = "unit_price")
    private Double unitPrice;

    @Column(name = "indent_level")
    private Integer level;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
