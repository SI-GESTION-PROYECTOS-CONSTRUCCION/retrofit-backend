package com.retrofit.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "progress_reports")
@Data
public class ProgressReport {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_item_id", nullable = false)
    private ProjectItem projectItem;

    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;

    @Column(name = "executed_quantity", nullable = false)
    private Double executedQuantity; // metrado_ejecutado

    @Column(columnDefinition = "TEXT")
    private String observations;

    @OneToMany(mappedBy = "progressReport", cascade = CascadeType.ALL)
    private List<ProgressPhoto> photos;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}