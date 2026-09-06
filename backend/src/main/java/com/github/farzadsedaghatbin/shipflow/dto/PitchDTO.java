package com.github.farzadsedaghatbin.shipflow.dto;

import com.github.farzadsedaghatbin.shipflow.entity.enums.BusinessValue;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PitchStatus;
import java.time.LocalDateTime;
import java.util.List;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PitchDTO {
  // Basic fields
  private Long id;
  private String title;
  private String description;
  private Integer appetiteDays;

  // Cycle information
  private Long cycleId;
  private String cycleName;

  // Project information
  private Long projectId;
  private String projectName;
  private String projectKey;

  // Team information
  private Long teamId;
  private String teamName;

  // Status and timestamps
  private PitchStatus status;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  // Progress metrics
  private Double totalHoursSpent;
  private Double appetiteHours;
  private Double progressPercentage;
  
  // Team Capacity and Budget
  private Integer teamMemberCount;
  private Double totalBudgetPersonDays;
  private Double budgetUtilizationPercent;
  private BusiestPersonDTO busiestPerson;

  // Circuit Breaker - Shape Up safety valve
  private Boolean isCircuitBreakerTriggered;
  private String circuitBreakerReason;
  private LocalDateTime circuitBreakerDate;

  // Shape Up Methodology Fields
  private String problemStatement;
  private String solution;
  private String rabbitHoles;
  private String risks;
  private String noGos;
  private String wireframeLinks;

  // Epic relationship (roadmap)
  private Long epicId;
  private String epicName;

  // Target release
  private Long targetReleaseId;
  private String targetReleaseName;
  private String targetReleaseVersion;

  // Priority and ordering
  private BusinessValue priority;
  private Integer sortOrder;

  // Roadmap dependencies
  private List<PitchDependencyDTO> blockingPitches;
  private List<PitchDependencyDTO> blockedByPitches;

  /** Optimistic-lock version (v1.13.0 S64) — echo back as {@code expectedVersion} on the next update. */
  private Long version;

  /**
   * Summary of the busiest team member (closest to exhausting their budget).
   */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class BusiestPersonDTO {
    private Long personId;
    private String personName;
    private String role;
    private Double hoursPerDay;
    private String capacitySource;
    private Double totalBudgetHours;
    private Double hoursSpent;
    private Double utilizationPercent;
    private Boolean isOverBudget;
  }
}
