package com.retrofit.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "progress_photos")
@Data
public class ProgressPhoto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "progress_report_id", nullable = false)
    private ProgressReport progressReport;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_url", nullable = false)
    private String fileUrl;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime uploadedAt;
}