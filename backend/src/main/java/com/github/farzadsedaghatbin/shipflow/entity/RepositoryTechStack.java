package com.github.farzadsedaghatbin.shipflow.entity;

import com.github.farzadsedaghatbin.shipflow.dto.wisearchitecture.TechStackType;
import com.github.farzadsedaghatbin.shipflow.entity.github.GitHubRepository;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/**
 * Entity to cache detected tech stacks for repositories.
 * Tech stacks are typically stable and don't change frequently,
 * so we cache them to avoid repeated detection.
 */
@Entity
@Table(name = "repository_tech_stacks", 
    uniqueConstraints = @UniqueConstraint(columnNames = {"repository_id", "stack_type"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RepositoryTechStack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id", nullable = false)
    private GitHubRepository repository;

    @Enumerated(EnumType.STRING)
    @Column(name = "stack_type", nullable = false, length = 50)
    private TechStackType stackType;

    @Column(name = "confidence_score")
    private Integer confidenceScore;

    @Column(name = "detected_at", nullable = false)
    private LocalDateTime detectedAt;

    @Column(name = "detected_by_files", columnDefinition = "TEXT")
    private String detectedByFiles; // Comma-separated list of key files that identified this stack

    @Column(name = "framework", length = 100)
    private String framework; // Detected framework (e.g., "Spring Boot", "React")

    @Column(name = "primary_language", length = 50)
    private String primaryLanguage; // Primary programming language (e.g., "Java", "TypeScript")

    @PrePersist
    protected void onCreate() {
        detectedAt = LocalDateTime.now();
    }
}
