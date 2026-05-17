package com.github.farzadsedaghatbin.shipflow.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CyclePhase;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

@Entity
@Table(name = "cycles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cycle {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "project_id")
  @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "cycles", "users"})
  private Project project;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private LocalDate startDate;

  @Column(nullable = false)
  private LocalDate endDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private CyclePhase phase;

  @Column(nullable = false)
  private Boolean isActive;

  /** Sprint goal text for Scrum mode (optional). */
  @Column(name = "sprint_goal", columnDefinition = "TEXT")
  private String sprintGoal;

  // velocity_actual column exists in DB (V100) but is not mapped here. Actual velocity is
  // computed dynamically by VelocityService from completed task story points and exposed via
  // VelocityPointDTO. The column is preserved for a future "close sprint" workflow.

  @OneToMany(mappedBy = "cycle", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<Pitch> pitches = new ArrayList<>();

  @OneToMany(mappedBy = "cycle", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<Retrospective> retrospectives = new ArrayList<>();
}
