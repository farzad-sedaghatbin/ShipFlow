package com.github.farzadsedaghatbin.shipflow.entity;

import com.github.farzadsedaghatbin.shipflow.entity.enums.TeamMemberRole;
import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.*;

@Entity
@Table(name = "team_assignments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamAssignment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "person_id", nullable = false)
  private Person person;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "team_id", nullable = false)
  private Team team;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TeamMemberRole role;

  @Column(nullable = false)
  private LocalDate startDate;

  @Column
  private LocalDate endDate;

  @Column(nullable = false)
  private Boolean isActive;

  @Column(columnDefinition = "TEXT")
  private String notes;

  // Capacity Configuration Override (finest-grained control per team assignment)
  // Useful for shared resources like tech leads with different allocations per team
  @Column(name = "hours_per_day_override", columnDefinition = "NUMERIC")
  private Double hoursPerDayOverride;

  @PrePersist
  protected void onCreate() {
    if (isActive == null) {
      isActive = true;
    }
    if (startDate == null) {
      startDate = LocalDate.now();
    }
  }
}
