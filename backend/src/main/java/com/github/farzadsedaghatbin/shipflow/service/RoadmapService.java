package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.RoadmapTimelineDTO;
import com.github.farzadsedaghatbin.shipflow.dto.RoadmapTimelineDTO.*;
import com.github.farzadsedaghatbin.shipflow.entity.Epic;
import com.github.farzadsedaghatbin.shipflow.entity.Initiative;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.entity.Release;
import com.github.farzadsedaghatbin.shipflow.entity.EpicDependency;
import com.github.farzadsedaghatbin.shipflow.entity.Task;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PitchStatus;
import com.github.farzadsedaghatbin.shipflow.repository.BugReportRepository;
import com.github.farzadsedaghatbin.shipflow.repository.EpicDependencyRepository;
import com.github.farzadsedaghatbin.shipflow.repository.EpicRepository;
import com.github.farzadsedaghatbin.shipflow.repository.ProjectRepository;
import com.github.farzadsedaghatbin.shipflow.repository.InitiativeRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
import com.github.farzadsedaghatbin.shipflow.repository.ReleaseRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TaskRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service for generating roadmap and timeline views. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoadmapService {

  private final InitiativeRepository initiativeRepository;
  private final EpicRepository epicRepository;
  private final PitchRepository pitchRepository;
  private final ReleaseRepository releaseRepository;
  private final TaskRepository taskRepository;
  private final BugReportRepository bugReportRepository;
  private final EpicDependencyRepository epicDependencyRepository;
  private final ProjectRepository projectRepository;

  /**
   * Get the complete roadmap timeline for a project within a date range.
   *
   * @param projectId the project ID
   * @param startDate start of the timeline window
   * @param endDate end of the timeline window
   * @return timeline data for Gantt chart rendering
   */
  @Cacheable(value = "roadmap", key = "#projectId + ':' + #startDate + ':' + #endDate")
  public RoadmapTimelineDTO getRoadmapTimeline(Long projectId, LocalDate startDate, LocalDate endDate) {
    List<Initiative> initiatives =
        initiativeRepository.findByProjectIdAndDateRangeNotDeleted(projectId, startDate, endDate);

    // Bulk-load root tasks for every epic of the project once (Kanban/Scrum), instead of N+1
    List<Epic> projectEpics = epicRepository.findByProjectIdNotDeleted(projectId);
    Map<Long, List<Task>> rootTasksByEpic = loadRootTasksByEpic(projectEpics);

    List<TimelineInitiative> timelineInitiatives = initiatives.stream()
        .map(i -> toTimelineInitiative(i, startDate, endDate, rootTasksByEpic))
        .collect(Collectors.toList());

    // Get orphan epics (not linked to any initiative) — only those with dates in range
    List<Epic> orphanEpics = epicRepository.findOrphanEpicsByProjectIdNotDeleted(projectId);
    List<TimelineEpic> orphanTimelineEpics = new ArrayList<>(orphanEpics.stream()
        .filter(e -> hasDatesInRange(e.getTargetStartDate(), e.getTargetEndDate(), startDate, endDate))
        .map(e -> toTimelineEpic(e, rootTasksByEpic))
        .toList());

    // Also include epics that belong to initiatives with no dates at all (those initiatives
    // are excluded from the date-filtered initiative query, but the epics may still have
    // their own dates in range and should appear as orphans on the timeline).
    List<Epic> epicsFromUndatedInitiatives =
        epicRepository.findEpicsWithDatesLinkedToUndatedInitiatives(projectId);
    epicsFromUndatedInitiatives.stream()
        .filter(e -> hasDatesInRange(e.getTargetStartDate(), e.getTargetEndDate(), startDate, endDate))
        .map(e -> toTimelineEpic(e, rootTasksByEpic))
        .forEach(orphanTimelineEpics::add);

    // Get releases within the date range
    List<Release> releases = releaseRepository.findByProjectIdNotDeleted(projectId).stream()
        .filter(r -> r.getTargetDate() != null &&
            !r.getTargetDate().isBefore(startDate) &&
            !r.getTargetDate().isAfter(endDate))
        .collect(Collectors.toList());

    // Bulk-fetch counts for all releases in 4 queries instead of 4N
    Map<Long, Long> pitchCounts;
    Map<Long, Long> donePitchCounts;
    Map<Long, Long> taskCounts;
    Map<Long, Long> bugCounts;
    if (releases.isEmpty()) {
      pitchCounts = Collections.emptyMap();
      donePitchCounts = Collections.emptyMap();
      taskCounts = Collections.emptyMap();
      bugCounts = Collections.emptyMap();
    } else {
      List<Long> releaseIds = releases.stream().map(Release::getId).collect(Collectors.toList());
      pitchCounts = toCountMap(pitchRepository.countByTargetReleaseIdsNotDeleted(releaseIds));
      donePitchCounts = toCountMap(pitchRepository.countByTargetReleaseIdsAndStatusNotDeleted(releaseIds, PitchStatus.DONE));
      taskCounts = toCountMap(taskRepository.countByTargetReleaseIdsNotDeleted(releaseIds));
      bugCounts = toCountMap(bugReportRepository.countByTargetReleaseIds(releaseIds));
    }

    List<TimelineRelease> timelineReleases = releases.stream()
        .map(r -> toTimelineRelease(r, pitchCounts, donePitchCounts, taskCounts, bugCounts))
        .collect(Collectors.toList());

    // Calculate overall date bounds
    LocalDate minDate = calculateMinDate(timelineInitiatives, orphanTimelineEpics, timelineReleases, startDate);
    LocalDate maxDate = calculateMaxDate(timelineInitiatives, orphanTimelineEpics, timelineReleases, endDate);

    Project project = projectRepository.findById(projectId).orElse(null);

    return RoadmapTimelineDTO.builder()
        .projectId(projectId)
        .projectName(project != null ? project.getName() : null)
        .projectType(project != null && project.getProjectType() != null ? project.getProjectType().name() : null)
        .dependencies(loadDependencies(projectEpics))
        .startDate(minDate)
        .endDate(maxDate)
        .initiatives(timelineInitiatives)
        .orphanEpics(orphanTimelineEpics)
        .releases(timelineReleases)
        .build();
  }

  /**
   * Get a simplified quarterly roadmap view.
   */
  public RoadmapTimelineDTO getQuarterlyRoadmap(Long projectId, int year, int quarter) {
    LocalDate startDate = LocalDate.of(year, (quarter - 1) * 3 + 1, 1);
    LocalDate endDate = startDate.plusMonths(3).minusDays(1);
    return getRoadmapTimeline(projectId, startDate, endDate);
  }

  /**
   * Get roadmap for the entire year.
   */
  public RoadmapTimelineDTO getYearlyRoadmap(Long projectId, int year) {
    LocalDate startDate = LocalDate.of(year, 1, 1);
    LocalDate endDate = LocalDate.of(year, 12, 31);
    return getRoadmapTimeline(projectId, startDate, endDate);
  }

  private Map<Long, List<Task>> loadRootTasksByEpic(List<Epic> epics) {
    if (epics.isEmpty()) {
      return Collections.emptyMap();
    }
    List<Long> epicIds = epics.stream().map(Epic::getId).collect(Collectors.toList());
    Map<Long, List<Task>> result = new HashMap<>();
    for (Task t : taskRepository.findByEpicIdInNotDeleted(epicIds)) {
      if (t.getParentTask() == null && t.getEpic() != null) {
        result.computeIfAbsent(t.getEpic().getId(), k -> new ArrayList<>()).add(t);
      }
    }
    return result;
  }

  private List<TimelineDependency> loadDependencies(List<Epic> projectEpics) {
    if (projectEpics.isEmpty()) {
      return new ArrayList<>();
    }
    Set<Long> ids = projectEpics.stream().map(Epic::getId).collect(Collectors.toCollection(HashSet::new));
    return epicDependencyRepository.findBySourceEpicIdInAndTargetEpicIdIn(ids, ids).stream()
        .map(this::toTimelineDependency)
        .collect(Collectors.toList());
  }

  private TimelineDependency toTimelineDependency(EpicDependency d) {
    return TimelineDependency.builder()
        .id(d.getId())
        .sourceEpicId(d.getSourceEpic().getId())
        .targetEpicId(d.getTargetEpic().getId())
        .dependencyType(d.getDependencyType().name())
        .build();
  }

  private TimelineInitiative toTimelineInitiative(
      Initiative initiative, LocalDate rangeStart, LocalDate rangeEnd, Map<Long, List<Task>> rootTasksByEpic) {
    List<TimelineEpic> epics = initiative.getEpics().stream()
        .filter(e -> e.getDeletedAt() == null)
        .filter(e -> hasDatesInRange(e.getTargetStartDate(), e.getTargetEndDate(), rangeStart, rangeEnd))
        .map(e -> toTimelineEpic(e, rootTasksByEpic))
        .collect(Collectors.toList());

    double progress = calculateInitiativeProgress(initiative, rootTasksByEpic);

    return TimelineInitiative.builder()
        .id(initiative.getId())
        .name(initiative.getName())
        .description(initiative.getDescription())
        .status(initiative.getStatus().name())
        .color(initiative.getColor())
        .startDate(initiative.getTargetStartDate())
        .endDate(initiative.getTargetEndDate())
        .progress(progress)
        .quarterLabel(computeQuarterLabel(initiative.getTargetStartDate(), initiative.getTargetEndDate()))
        .epics(epics)
        .build();
  }

  private TimelineEpic toTimelineEpic(Epic epic, Map<Long, List<Task>> rootTasksByEpic) {
    List<Task> rootTasks = rootTasksByEpic.getOrDefault(epic.getId(), Collections.emptyList());
    List<TimelineTask> timelineTasks =
        rootTasks.stream().map(this::toTimelineTask).collect(Collectors.toList());

    List<TimelinePitch> pitches = epic.getPitches().stream()
        .filter(p -> p.getDeletedAt() == null)
        .map(this::toTimelinePitch)
        .collect(Collectors.toList());

    double progress = calculateEpicProgress(epic, rootTasks);

    return TimelineEpic.builder()
        .id(epic.getId())
        .name(epic.getName())
        .description(epic.getDescription())
        .status(epic.getStatus().name())
        .color(epic.getColor())
        .startDate(epic.getTargetStartDate())
        .endDate(epic.getTargetEndDate())
        .progress(progress)
        .quarterLabel(computeQuarterLabel(epic.getTargetStartDate(), epic.getTargetEndDate()))
        .pitches(pitches)
        .tasks(timelineTasks)
        .build();
  }

  private TimelineTask toTimelineTask(Task task) {
    double progress = switch (task.getStatus()) {
      case DONE -> 100.0;
      case IN_REVIEW -> 80.0;
      case IN_PROGRESS -> 50.0;
      case BACKLOG, TODO, BLOCKED, CANCELLED -> 0.0;
    };
    return TimelineTask.builder()
        .id(task.getId())
        .title(task.getTitle())
        .status(task.getStatus().name())
        .priority(task.getPriority() != null ? task.getPriority().name() : null)
        .dueDate(task.getDueDate())
        .progress(progress)
        .epicId(task.getEpic() != null ? task.getEpic().getId() : null)
        .parentTaskId(task.getParentTask() != null ? task.getParentTask().getId() : null)
        .build();
  }

  private TimelinePitch toTimelinePitch(Pitch pitch) {
    // Calculate progress based on pitch status
    double progress = switch (pitch.getStatus()) {
      case IDEA, DRAFT, PENDING, SHAPED -> 0.0;
      case STARTED -> 10.0;
      case IN_PROGRESS -> 50.0;
      case TESTING -> 80.0;
      case DONE -> 100.0;
      case COOLDOWN -> 100.0;
      case CANCELLED, CIRCUIT_BREAKER -> 0.0;
    };

    return TimelinePitch.builder()
        .id(pitch.getId())
        .title(pitch.getTitle())
        .status(pitch.getStatus().name())
        .startDate(pitch.getCycle() != null ? pitch.getCycle().getStartDate() : null)
        .endDate(pitch.getCycle() != null ? pitch.getCycle().getEndDate() : null)
        .progress(progress)
        .epicId(pitch.getEpic() != null ? pitch.getEpic().getId() : null)
        .targetReleaseId(pitch.getTargetRelease() != null ? pitch.getTargetRelease().getId() : null)
        .build();
  }

  private TimelineRelease toTimelineRelease(
      Release release,
      Map<Long, Long> pitchCounts,
      Map<Long, Long> donePitchCounts,
      Map<Long, Long> taskCounts,
      Map<Long, Long> bugCounts) {
    long totalPitches = pitchCounts.getOrDefault(release.getId(), 0L);
    long completedPitches = donePitchCounts.getOrDefault(release.getId(), 0L);
    long totalTasks = taskCounts.getOrDefault(release.getId(), 0L);
    long totalBugs = bugCounts.getOrDefault(release.getId(), 0L);

    double progress = totalPitches > 0 ? (double) completedPitches / totalPitches * 100.0 : 0.0;

    return TimelineRelease.builder()
        .id(release.getId())
        .name(release.getName())
        .version(release.getVersion())
        .status(release.getStatus().name())
        .targetDate(release.getTargetDate())
        .releaseDate(release.getReleaseDate())
        .riskLevel(release.getRiskLevel().name())
        .progressPercentage(progress)
        .pitchCount(totalPitches)
        .taskCount(totalTasks)
        .bugCount(totalBugs)
        .build();
  }

  private Map<Long, Long> toCountMap(List<Object[]> rows) {
    return rows.stream().collect(Collectors.toMap(
        r -> (Long) r[0],
        r -> (Long) r[1]));
  }

  private double calculateInitiativeProgress(Initiative initiative, Map<Long, List<Task>> rootTasksByEpic) {
    List<Epic> activeEpics = initiative.getEpics().stream()
        .filter(e -> e.getDeletedAt() == null)
        .collect(Collectors.toList());

    if (activeEpics.isEmpty()) {
      return 0.0;
    }

    double totalProgress = activeEpics.stream()
        .mapToDouble(e -> calculateEpicProgress(e, rootTasksByEpic.getOrDefault(e.getId(), Collections.emptyList())))
        .sum();

    return totalProgress / activeEpics.size();
  }

  private double calculateEpicProgress(Epic epic, List<Task> rootTasks) {
    List<Pitch> activePitches = epic.getPitches().stream()
        .filter(p -> p.getDeletedAt() == null)
        .collect(Collectors.toList());

    if (activePitches.isEmpty() && rootTasks.isEmpty()) {
      return switch (epic.getStatus()) {
        case COMPLETED -> 100.0;
        case CANCELLED -> 0.0;
        default -> 0.0;
      };
    }

    long completed = activePitches.stream()
        .filter(p -> p.getStatus() == PitchStatus.DONE)
        .count();

    long completedTasks = rootTasks.stream().filter(t -> t.getStatus() == TaskStatus.DONE).count();
    int total = activePitches.size() + rootTasks.size();

    return (double) (completed + completedTasks) / total * 100.0;
  }

  private boolean hasDatesInRange(LocalDate itemStart, LocalDate itemEnd, LocalDate rangeStart, LocalDate rangeEnd) {
    if (itemStart == null) {
      return false;
    }
    // Treat a null endDate as open-ended — the item started and has no scheduled finish,
    // so include it as long as its startDate is not after the range end.
    if (itemEnd == null) {
      return !itemStart.isAfter(rangeEnd);
    }
    return !itemStart.isAfter(rangeEnd) && !itemEnd.isBefore(rangeStart);
  }

  private String computeQuarterLabel(LocalDate start, LocalDate end) {
    if (start == null || end == null) {
      return null;
    }
    int startQ = (start.getMonthValue() - 1) / 3 + 1;
    int endQ = (end.getMonthValue() - 1) / 3 + 1;
    int startY = start.getYear();
    int endY = end.getYear();
    if (startY == endY && startQ == endQ) {
      return "Q" + startQ + " " + startY;
    }
    if (startY == endY) {
      return "Q" + startQ + " – Q" + endQ + " " + startY;
    }
    return "Q" + startQ + " " + startY + " – Q" + endQ + " " + endY;
  }

  private LocalDate calculateMinDate(
      List<TimelineInitiative> initiatives,
      List<TimelineEpic> orphanEpics,
      List<TimelineRelease> releases,
      LocalDate defaultDate) {
    
    LocalDate min = defaultDate;

    for (TimelineInitiative init : initiatives) {
      if (init.getStartDate() != null && init.getStartDate().isBefore(min)) {
        min = init.getStartDate();
      }
    }

    for (TimelineEpic epic : orphanEpics) {
      if (epic.getStartDate() != null && epic.getStartDate().isBefore(min)) {
        min = epic.getStartDate();
      }
    }

    for (TimelineRelease release : releases) {
      if (release.getTargetDate() != null && release.getTargetDate().isBefore(min)) {
        min = release.getTargetDate();
      }
    }

    return min;
  }

  private LocalDate calculateMaxDate(
      List<TimelineInitiative> initiatives,
      List<TimelineEpic> orphanEpics,
      List<TimelineRelease> releases,
      LocalDate defaultDate) {
    
    LocalDate max = defaultDate;

    for (TimelineInitiative init : initiatives) {
      if (init.getEndDate() != null && init.getEndDate().isAfter(max)) {
        max = init.getEndDate();
      }
    }

    for (TimelineEpic epic : orphanEpics) {
      if (epic.getEndDate() != null && epic.getEndDate().isAfter(max)) {
        max = epic.getEndDate();
      }
    }

    for (TimelineRelease release : releases) {
      LocalDate releaseDate = release.getReleaseDate() != null ? release.getReleaseDate() : release.getTargetDate();
      if (releaseDate != null && releaseDate.isAfter(max)) {
        max = releaseDate;
      }
    }

    return max;
  }
}
