package com.github.farzadsedaghatbin.shipflow.dto;

import com.github.farzadsedaghatbin.shipflow.entity.enums.InitiativeStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.*;

/**
 * DTO for Initiative entity.
 * Represents strategic themes spanning multiple quarters.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InitiativeDTO {
  private Long id;
  private String name;
  private String description;
  private InitiativeStatus status;
  private String color;
  private LocalDate targetStartDate;
  private LocalDate targetEndDate;
  private Integer sortOrder;

  // Project information
  private Long projectId;
  private String projectName;
  private String projectKey;

  // Owner information
  private Long ownerId;
  private String ownerName;

  // Timestamps
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  // Progress metrics (calculated)
  private Integer epicCount;
  private Integer completedEpicCount;
  private Integer pitchCount;
  private Integer completedPitchCount;
  private Double progressPercentage;

  // Child epics (optional, for detail view)
  private List<EpicSummaryDTO> epics;

  /**
   * Summary of an epic for list views.
   */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class EpicSummaryDTO {
    private Long id;
    private String name;
    private String status;
    private LocalDate targetStartDate;
    private LocalDate targetEndDate;
    private Integer pitchCount;
    private Double progressPercentage;
  }
}
