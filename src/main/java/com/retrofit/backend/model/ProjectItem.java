package com.retrofit.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "project_items")
@Data
public class ProjectItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    @Column(name = "labor_yield")
    private Double laborYield = 0.0;

    @Column(name = "equipment_yield")
    private Double equipmentYield = 0.0;

    @OneToMany(mappedBy = "projectItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProjectItemResource> apuDetails;

    @Column(name = "item_order")
    private Integer itemOrder = 0;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "predecessor_id")
    private Long predecessorId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
