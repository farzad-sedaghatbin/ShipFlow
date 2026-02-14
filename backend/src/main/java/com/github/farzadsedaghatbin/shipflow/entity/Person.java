package com.github.farzadsedaghatbin.shipflow.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

@Entity
@Table(name = "persons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Person {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  @Column(unique = true)
  private String email;

  @Column
  private String avatarUrl;

  @Column
  private String department;

  @Column(columnDefinition = "TEXT")
  private String skills;

  @Column(columnDefinition = "TEXT")
  private String bio;

  @Column(nullable = false)
  private Boolean isActive;

  // Capacity Configuration (null = inherit from organization/team)
  @Column(name = "hours_per_day_override", columnDefinition = "NUMERIC")
  private Double hoursPerDayOverride;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  @Column
  private LocalDateTime updatedAt;

  @OneToMany(mappedBy = "person", cascade = CascadeType.ALL)
  @Builder.Default
  private List<TeamAssignment> teamAssignments = new ArrayList<>();

  @OneToMany(mappedBy = "person", cascade = CascadeType.ALL)
  @Builder.Default
  private List<WorkLog> workLogs = new ArrayList<>();

  @OneToMany(mappedBy = "person", cascade = CascadeType.ALL)
  @Builder.Default
  private List<Evidence> evidences = new ArrayList<>();

  @OneToOne(mappedBy = "person", fetch = FetchType.LAZY)
  private User user;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    if (isActive == null) {
      isActive = true;
    }
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
