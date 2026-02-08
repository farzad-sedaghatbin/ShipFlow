package com.github.farzadsedaghatbin.shipflow.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PitchStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.*;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.hibernate.envers.RelationTargetAuditMode;

@Entity
@Table(name = "pitches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Audited
public class Pitch {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String title;

  @Column(columnDefinition = "TEXT")
  private String description;

  @NotAudited
  @Column(nullable = false)
  private Integer appetiteDays;

  // Shape Up Methodology Fields
  @NotAudited
  @Column(columnDefinition = "TEXT")
  private String problemStatement;

  @NotAudited
  @Column(columnDefinition = "TEXT")
  private String solution;

  @NotAudited
  @Column(columnDefinition = "TEXT")
  private String rabbitHoles;

  @NotAudited
  @Column(columnDefinition = "TEXT")
  private String risks;

  @NotAudited
  @Column(columnDefinition = "TEXT")
  private String noGos;

  @NotAudited
  @Column(columnDefinition = "TEXT")
  private String wireframeLinks;

  @NotAudited
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cycle_id", nullable = false)
  @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "pitches", "teams", "retrospectives", "project"})
  private Cycle cycle;

  @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
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

  @NotAudited
  @Column
  private LocalDateTime circuitBreakerDate;

  @NotAudited
  @Column(nullable = false)
  private LocalDateTime createdAt;

  @NotAudited
  @Column(nullable = false)
  private LocalDateTime updatedAt;

  // Soft delete fields
  @NotAudited
  @Column
  private LocalDateTime deletedAt;

  @NotAudited
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "deleted_by_id")
  private User deletedBy;

  @NotAudited
  @OneToMany(mappedBy = "pitch", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<WorkLog> workLogs = new ArrayList<>();

  @NotAudited
  @OneToMany(mappedBy = "pitch", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<Meeting> meetings = new ArrayList<>();

  @NotAudited
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
