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
  /** SHAPE_UP, KANBAN or SCRUM. Shape Up epics hold pitches; Kanban/Scrum epics hold tasks. */
  private String projectType;
  private LocalDate startDate;
  private LocalDate endDate;

  // Timeline items
  private List<TimelineInitiative> initiatives;
  private List<TimelineEpic> orphanEpics; // Epics not linked to any initiative
  private List<TimelineRelease> releases;

  /**
   * Epic-to-epic dependencies where both epics belong to this project. Direction: the
   * {@code sourceEpicId} epic BLOCKS the {@code targetEpicId} epic (the target is waiting on the
   * source) when {@code dependencyType} is BLOCKS, which is the default.
   */
  private List<TimelineDependency> dependencies;

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
    private String quarterLabel;

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
    private String quarterLabel;

    // Child pitches (Shape Up)
    private List<TimelinePitch> pitches;

    // Child root tasks (Kanban/Scrum)
    private List<TimelineTask> tasks;
  }

  /** Root task (no parent) linked to an epic, for Kanban/Scrum roadmaps. */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class TimelineTask {
    private Long id;
    private String title;
    private String status;
    private String priority;
    private LocalDate dueDate;
    private Double progress;
    private Long epicId;
    private Long parentTaskId;
  }

  /**
   * Dependency between two epics. {@code sourceEpicId} BLOCKS {@code targetEpicId} for type
   * BLOCKS (target waits on source); DEPENDS_ON is the inverse reading; RELATED_TO is symmetric.
   */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class TimelineDependency {
    private Long id;
    private Long sourceEpicId;
    private Long targetEpicId;
    private String dependencyType;
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
    private Double progress;
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
    private Long pitchCount;
    private Long taskCount;
    private Long bugCount;
  }
}
