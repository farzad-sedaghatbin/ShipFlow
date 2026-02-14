package com.github.farzadsedaghatbin.shipflow.dto;

import com.github.farzadsedaghatbin.shipflow.entity.enums.ReleaseRiskLevel;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ReleaseStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.*;

/**
 * DTO for Release entity.
 * Represents delivery milestones that can contain items from multiple epics.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReleaseDTO {
  private Long id;
  private String name;
  private String version;
  private String description;
  private ReleaseStatus status;
  private ReleaseRiskLevel riskLevel;
  private LocalDate targetDate;
  private LocalDate releaseDate;
  private String releaseNotes;
  private Integer sortOrder;

  // Project information
  private Long projectId;
  private String projectName;
  private String projectKey;

  // Timestamps
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  // Linked cycles
  private List<CycleSummaryDTO> cycles;

  // Progress metrics (calculated)
  private ReleaseProgressDTO progress;

  /**
   * Summary of a linked cycle.
   */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class CycleSummaryDTO {
    private Long id;
    private String name;
    private String phase;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean isActive;
  }
}
