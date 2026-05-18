package com.retrofit.backend.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "progress_report_resources")
@Data
public class ProgressReportResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    private ProgressReport progressReport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id", nullable = false)
    private Resource resource;

    @Column(name = "theoretical_quantity")
    private Double theoreticalQuantity;

    @Column(name = "real_quantity")
    private Double realQuantity;
}