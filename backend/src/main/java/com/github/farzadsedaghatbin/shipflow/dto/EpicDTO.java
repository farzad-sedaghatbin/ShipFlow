package com.github.farzadsedaghatbin.shipflow.dto;

import com.github.farzadsedaghatbin.shipflow.entity.enums.BusinessValue;
import com.github.farzadsedaghatbin.shipflow.entity.enums.EpicStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.*;

/**
 * DTO for Epic entity.
 * Represents large features grouping related pitches.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EpicDTO {
  private Long id;
  private String name;
  private String description;
  private EpicStatus status;
  private String color;
  private BusinessValue priority;
  private LocalDate targetStartDate;
  private LocalDate targetEndDate;
  private Integer sortOrder;

  // Project information
  private Long projectId;
  private String projectName;
  private String projectKey;

  // Initiative information (optional parent)
  private Long initiativeId;
  private String initiativeName;

  // Owner information
  private Long ownerId;
  private String ownerName;

  // Timestamps
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  // Progress metrics (calculated)
  private Integer pitchCount;
  private Integer completedPitchCount;
  private Integer taskCount;
  private Integer completedTaskCount;
  private Double progressPercentage;

  // Child pitches (optional, for detail view)
  private List<PitchSummaryDTO> pitches;

  // Roadmap dependencies (informational)
  private List<EpicDependencyDTO> blockingEpics;
  private List<EpicDependencyDTO> blockedByEpics;

  /**
   * Summary of a pitch for list views.
   */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class PitchSummaryDTO {
    private Long id;
    private String title;
    private String status;
    private String cycleName;
    private Integer appetiteDays;
    private Double progressPercentage;
  }
}
