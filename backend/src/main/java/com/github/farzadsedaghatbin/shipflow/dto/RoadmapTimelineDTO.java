package com.github.farzadsedaghatbin.shipflow.dto;

import java.time.LocalDate;
import java.util.List;
import lombok.*;

/**
 * DTO for roadmap timeline data.
 * Used to render Gantt/timeline views showing initiatives, epics, and releases.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoadmapTimelineDTO {
  private Long projectId;
  private String projectName;
  private LocalDate startDate;
  private LocalDate endDate;

  // Timeline items
  private List<TimelineInitiative> initiatives;
  private List<TimelineEpic> orphanEpics; // Epics not linked to any initiative
  private List<TimelineRelease> releases;

  /**
   * Initiative representation for timeline rendering.
   */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class TimelineInitiative {
    private Long id;
    private String name;
    private String description;
    private String status;
    private String color;
    private LocalDate startDate;
    private LocalDate endDate;
    private Double progress;
    private Integer sortOrder;

    // Child epics
    private List<TimelineEpic> epics;
  }

  /**
   * Epic representation for timeline rendering.
   */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class TimelineEpic {
    private Long id;
    private String name;
    private String description;
    private String status;
    private String color;
    private LocalDate startDate;
    private LocalDate endDate;
    private Double progress;
    private Integer sortOrder;
    private Long initiativeId;

    // Child pitches
    private List<TimelinePitch> pitches;
  }

  /**
   * Pitch representation for timeline rendering.
   */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class TimelinePitch {
    private Long id;
    private String title;
    private String status;
    private LocalDate startDate; // Cycle start date
    private LocalDate endDate;   // Cycle end date
    private Double progressPercentage;
    private Long epicId;
    private Long targetReleaseId;
  }

  /**
   * Release milestone for timeline rendering.
   */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class TimelineRelease {
    private Long id;
    private String name;
    private String version;
    private String status;
    private String riskLevel;
    private LocalDate targetDate;
    private LocalDate releaseDate;
    private Double progressPercentage;
    private Integer sortOrder;
  }
}
