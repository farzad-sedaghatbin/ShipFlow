package com.github.farzadsedaghatbin.shipflow.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PitchStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

@Entity
@Table(name = "pitches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pitch {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String title;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(nullable = false)
  private Integer appetiteDays;

  // Shape Up Methodology Fields
  @Column(columnDefinition = "TEXT")
  private String problemStatement;

  @Column(columnDefinition = "TEXT")
  private String solution;

  @Column(columnDefinition = "TEXT")
  private String rabbitHoles;

  @Column(columnDefinition = "TEXT")
  private String risks;

  @Column(columnDefinition = "TEXT")
  private String noGos;

  @Column(columnDefinition = "TEXT")
  private String wireframeLinks;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cycle_id", nullable = false)
  @JsonIgnoreProperties({
    "hibernateLazyInitializer",
    "handler",
    "pitches",
    "teams",
    "retrospectives",
    "project"
  })
  private Cycle cycle;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "team_id")
  @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "cycle", "members", "pitches"})
  private Team team;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PitchStatus status;

  // Circuit Breaker - Shape Up safety valve for overflow
  @Builder.Default
  @Column(nullable = false)
  private Boolean isCircuitBreakerTriggered = false;

  @Column(columnDefinition = "TEXT")
  private String circuitBreakerReason;

  @Column private LocalDateTime circuitBreakerDate;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  @Column(nullable = false)
  private LocalDateTime updatedAt;

  // Soft delete fields
  @Column
  private LocalDateTime deletedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "deleted_by_id")
  private User deletedBy;

  @OneToMany(mappedBy = "pitch", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<WorkLog> workLogs = new ArrayList<>();

  @OneToMany(mappedBy = "pitch", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<Meeting> meetings = new ArrayList<>();

  @OneToMany(mappedBy = "pitch", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<Evidence> evidences = new ArrayList<>();

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
