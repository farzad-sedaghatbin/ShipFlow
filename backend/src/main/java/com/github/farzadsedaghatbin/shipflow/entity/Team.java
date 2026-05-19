package com.github.farzadsedaghatbin.shipflow.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

@Entity
@Table(name = "teams")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Team {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  // Capacity Configuration Overrides (null = inherit from organization)
  @Column(name = "hours_per_day_override", columnDefinition = "NUMERIC")
  private Double hoursPerDayOverride;

  @Column(name = "working_days_per_week_override")
  private Integer workingDaysPerWeekOverride;

  @Column(name = "is_archived", nullable = false)
  @Builder.Default
  private Boolean isArchived = false;

  @ManyToMany(mappedBy = "teams")
  @Builder.Default
  private List<Cycle> cycles = new ArrayList<>();

  @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<TeamAssignment> assignments = new ArrayList<>();

  @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<Pitch> pitches = new ArrayList<>();
}
