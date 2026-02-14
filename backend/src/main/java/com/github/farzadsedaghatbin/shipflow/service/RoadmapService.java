package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.RoadmapTimelineDTO;
import com.github.farzadsedaghatbin.shipflow.dto.RoadmapTimelineDTO.*;
import com.github.farzadsedaghatbin.shipflow.entity.Epic;
import com.github.farzadsedaghatbin.shipflow.entity.Initiative;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.Release;
import com.github.farzadsedaghatbin.shipflow.entity.enums.EpicStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.InitiativeStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PitchStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ReleaseStatus;
import com.github.farzadsedaghatbin.shipflow.repository.EpicRepository;
import com.github.farzadsedaghatbin.shipflow.repository.InitiativeRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
import com.github.farzadsedaghatbin.shipflow.repository.ReleaseRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
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

  /**
   * Get the complete roadmap timeline for a project within a date range.
   *
   * @param projectId the project ID
   * @param startDate start of the timeline window
   * @param endDate end of the timeline window
   * @return timeline data for Gantt chart rendering
   */
  public RoadmapTimelineDTO getRoadmapTimeline(Long projectId, LocalDate startDate, LocalDate endDate) {
    List<Initiative> initiatives =
        initiativeRepository.findByProjectIdAndDateRangeNotDeleted(projectId, startDate, endDate);

    List<TimelineInitiative> timelineInitiatives = initiatives.stream()
        .map(this::toTimelineInitiative)
        .collect(Collectors.toList());

    // Get orphan epics (not linked to any initiative)
    List<Epic> orphanEpics = epicRepository.findOrphanEpicsByProjectIdNotDeleted(projectId);
    List<TimelineEpic> orphanTimelineEpics = orphanEpics.stream()
        .filter(e -> overlapsDateRange(e.getTargetStartDate(), e.getTargetEndDate(), startDate, endDate))
        .map(this::toTimelineEpic)
        .collect(Collectors.toList());

    // Get releases within the date range
    List<Release> releases = releaseRepository.findByProjectIdNotDeleted(projectId).stream()
        .filter(r -> r.getTargetDate() != null && 
            !r.getTargetDate().isBefore(startDate) && 
            !r.getTargetDate().isAfter(endDate))
        .collect(Collectors.toList());

    List<TimelineRelease> timelineReleases = releases.stream()
        .map(this::toTimelineRelease)
        .collect(Collectors.toList());

    // Calculate overall date bounds
    LocalDate minDate = calculateMinDate(timelineInitiatives, orphanTimelineEpics, timelineReleases, startDate);
    LocalDate maxDate = calculateMaxDate(timelineInitiatives, orphanTimelineEpics, timelineReleases, endDate);

    return RoadmapTimelineDTO.builder()
        .projectId(projectId)
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

  private TimelineInitiative toTimelineInitiative(Initiative initiative) {
    List<TimelineEpic> epics = initiative.getEpics().stream()
        .filter(e -> e.getDeletedAt() == null)
        .map(this::toTimelineEpic)
        .collect(Collectors.toList());

    double progress = calculateInitiativeProgress(initiative);

    return TimelineInitiative.builder()
        .id(initiative.getId())
        .name(initiative.getName())
        .description(initiative.getDescription())
        .status(initiative.getStatus().name())
        .color(initiative.getColor())
        .startDate(initiative.getTargetStartDate())
        .endDate(initiative.getTargetEndDate())
        .progress(progress)
        .epics(epics)
        .build();
  }

  private TimelineEpic toTimelineEpic(Epic epic) {
    List<TimelinePitch> pitches = epic.getPitches().stream()
        .filter(p -> p.getDeletedAt() == null)
        .map(this::toTimelinePitch)
        .collect(Collectors.toList());

    double progress = calculateEpicProgress(epic);

    return TimelineEpic.builder()
        .id(epic.getId())
        .name(epic.getName())
        .description(epic.getDescription())
        .status(epic.getStatus().name())
        .color(epic.getColor())
        .startDate(epic.getTargetStartDate())
        .endDate(epic.getTargetEndDate())
        .progress(progress)
        .pitches(pitches)
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
        .progressPercentage(progress)
        .epicId(pitch.getEpic() != null ? pitch.getEpic().getId() : null)
        .targetReleaseId(pitch.getTargetRelease() != null ? pitch.getTargetRelease().getId() : null)
        .build();
  }

  private TimelineRelease toTimelineRelease(Release release) {
    long totalPitches = pitchRepository.countByTargetReleaseIdNotDeleted(release.getId());
    long completedPitches = pitchRepository.countByTargetReleaseIdAndStatusNotDeleted(
        release.getId(), PitchStatus.DONE);

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
        .build();
  }

  private double calculateInitiativeProgress(Initiative initiative) {
    List<Epic> activeEpics = initiative.getEpics().stream()
        .filter(e -> e.getDeletedAt() == null)
        .collect(Collectors.toList());

    if (activeEpics.isEmpty()) {
      return 0.0;
    }

    double totalProgress = activeEpics.stream()
        .mapToDouble(this::calculateEpicProgress)
        .sum();

    return totalProgress / activeEpics.size();
  }

  private double calculateEpicProgress(Epic epic) {
    List<Pitch> activePitches = epic.getPitches().stream()
        .filter(p -> p.getDeletedAt() == null)
        .collect(Collectors.toList());

    if (activePitches.isEmpty()) {
      return switch (epic.getStatus()) {
        case COMPLETED -> 100.0;
        case CANCELLED -> 0.0;
        default -> 0.0;
      };
    }

    long completed = activePitches.stream()
        .filter(p -> p.getStatus() == PitchStatus.DONE)
        .count();

    return (double) completed / activePitches.size() * 100.0;
  }

  private boolean overlapsDateRange(LocalDate itemStart, LocalDate itemEnd, LocalDate rangeStart, LocalDate rangeEnd) {
    if (itemStart == null && itemEnd == null) {
      return true; // Items with no dates are always included
    }
    LocalDate start = itemStart != null ? itemStart : itemEnd;
    LocalDate end = itemEnd != null ? itemEnd : itemStart;
    return !start.isAfter(rangeEnd) && !end.isBefore(rangeStart);
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
