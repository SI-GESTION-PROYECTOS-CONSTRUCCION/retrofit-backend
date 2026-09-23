package com.retrofit.backend.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "project_item_dependencies", uniqueConstraints = @UniqueConstraint(columnNames = {"successor_id", "predecessor_id"}))
@Data
public class ProjectItemDependency {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "successor_id", nullable = false)
    private ProjectItem successor;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "predecessor_id", nullable = false)
    private ProjectItem predecessor;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private DependencyType type = DependencyType.FINISH_TO_START;
}
