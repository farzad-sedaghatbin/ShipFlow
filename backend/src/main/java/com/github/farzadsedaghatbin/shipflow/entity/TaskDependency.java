package com.github.farzadsedaghatbin.shipflow.entity;

import com.github.farzadsedaghatbin.shipflow.entity.enums.DependencyType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity representing a dependency between two tasks.
 * Supports lightweight dependency tracking to help teams identify blockers.
 */
@Entity
@Table(name = "task_dependencies", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"source_task_id", "target_task_id"}),
       indexes = {
           @Index(name = "idx_task_dep_source", columnList = "source_task_id"),
           @Index(name = "idx_task_dep_target", columnList = "target_task_id"),
           @Index(name = "idx_task_dep_type", columnList = "dependency_type")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskDependency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The task that has the dependency (the blocking task or the dependent task).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_task_id", nullable = false)
    private Task sourceTask;

    /**
     * The task being depended upon (the blocked task or the task being waited on).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_task_id", nullable = false)
    private Task targetTask;

    /**
     * Type of dependency relationship.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "dependency_type", nullable = false, length = 50)
    @Builder.Default
    private DependencyType dependencyType = DependencyType.BLOCKS;

    /**
     * When this dependency was created.
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
