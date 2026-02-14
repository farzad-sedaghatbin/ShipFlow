package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.CreateReleaseRequest;
import com.github.farzadsedaghatbin.shipflow.dto.ReleaseDTO;
import com.github.farzadsedaghatbin.shipflow.dto.ReleaseProgressDTO;
import com.github.farzadsedaghatbin.shipflow.entity.BugReport;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.entity.Release;
import com.github.farzadsedaghatbin.shipflow.entity.Task;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.enums.BugStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PitchStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ReleaseRiskLevel;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ReleaseStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskStatus;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import com.github.farzadsedaghatbin.shipflow.repository.BugReportRepository;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
import com.github.farzadsedaghatbin.shipflow.repository.ProjectRepository;
import com.github.farzadsedaghatbin.shipflow.repository.ReleaseRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TaskRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ReleaseService {

  private final ReleaseRepository releaseRepository;
  private final CycleRepository cycleRepository;
  private final PitchRepository pitchRepository;
  private final TaskRepository taskRepository;
  private final BugReportRepository bugReportRepository;
  private final ProjectRepository projectRepository;
  private final UserRepository userRepository;
  private final ApplicationEventPublisher eventPublisher;

  public List<ReleaseDTO> getAllReleases() {
    return releaseRepository.findAllNotDeleted().stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
  }

  public List<ReleaseDTO> getReleasesByProjectId(Long projectId) {
    return releaseRepository.findByProjectIdNotDeleted(projectId).stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
  }

  public Page<ReleaseDTO> getReleasesByProjectId(Long projectId, Pageable pageable) {
    return releaseRepository.findByProjectIdNotDeleted(projectId, pageable)
        .map(this::toDTO);
  }

  public List<ReleaseDTO> getUpcomingReleases(Long projectId) {
    return releaseRepository.findUpcomingByProjectIdNotDeleted(projectId, LocalDate.now()).stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
  }

  public List<ReleaseDTO> getReleasedReleases(Long projectId) {
    return releaseRepository.findReleasedByProjectIdNotDeleted(projectId).stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
  }

  public List<ReleaseDTO> getReleasesByProjectIdAndStatus(Long projectId, ReleaseStatus status) {
    return releaseRepository.findByProjectIdAndStatusNotDeleted(projectId, status).stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
  }

  public List<ReleaseDTO> getReleasesByDateRange(Long projectId, LocalDate startDate, LocalDate endDate) {
    return releaseRepository.findByProjectIdAndDateRangeNotDeleted(projectId, startDate, endDate).stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
  }

  public ReleaseDTO getReleaseById(Long id) {
    Release release = releaseRepository.findByIdNotDeleted(id)
        .orElseThrow(() -> new ResourceNotFoundException("Release not found with id: " + id));
    return toDTO(release);
  }

  public ReleaseDTO getReleaseWithCycles(Long id) {
    Release release = releaseRepository.findByIdWithCyclesNotDeleted(id)
        .orElseThrow(() -> new ResourceNotFoundException("Release not found with id: " + id));
    return toDTOWithCycles(release);
  }

  public ReleaseDTO createRelease(CreateReleaseRequest request) {
    Project project = projectRepository.findById(request.getProjectId())
        .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + request.getProjectId()));

    Release release = Release.builder()
        .name(request.getName())
        .version(request.getVersion())
        .description(request.getDescription())
        .status(request.getStatus() != null ? request.getStatus() : ReleaseStatus.PLANNING)
        .riskLevel(request.getRiskLevel() != null ? request.getRiskLevel() : ReleaseRiskLevel.LOW)
        .targetDate(request.getTargetDate())
        .releaseDate(request.getReleaseDate())
        .releaseNotes(request.getReleaseNotes())
        .project(project)
        .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
        .build();

    // Link cycles if provided
    if (request.getCycleIds() != null && !request.getCycleIds().isEmpty()) {
      for (Long cycleId : request.getCycleIds()) {
        Cycle cycle = cycleRepository.findById(cycleId)
            .orElseThrow(() -> new ResourceNotFoundException("Cycle not found with id: " + cycleId));
        release.addCycle(cycle);
      }
    }

    Release saved = releaseRepository.save(release);
    log.info("Created release: {} for project: {}", saved.getName(), project.getProjectKey());
    
    // Publish event for knowledge ingestion
    eventPublisher.publishEvent(new KnowledgeEventListener.ReleaseKnowledgeEvent(saved.getId()));
    
    return toDTO(saved);
  }

  public ReleaseDTO updateRelease(Long id, CreateReleaseRequest request) {
    Release release = releaseRepository.findByIdNotDeleted(id)
        .orElseThrow(() -> new ResourceNotFoundException("Release not found with id: " + id));

    release.setName(request.getName());
    release.setVersion(request.getVersion());
    release.setDescription(request.getDescription());
    if (request.getStatus() != null) {
      release.setStatus(request.getStatus());
    }
    if (request.getRiskLevel() != null) {
      release.setRiskLevel(request.getRiskLevel());
    }
    release.setTargetDate(request.getTargetDate());
    release.setReleaseDate(request.getReleaseDate());
    release.setReleaseNotes(request.getReleaseNotes());
    if (request.getSortOrder() != null) {
      release.setSortOrder(request.getSortOrder());
    }

    Release saved = releaseRepository.save(release);
    log.info("Updated release: {}", saved.getName());
    
    // Publish event for knowledge ingestion
    eventPublisher.publishEvent(new KnowledgeEventListener.ReleaseKnowledgeEvent(saved.getId()));
    
    return toDTO(saved);
  }

  public void deleteRelease(Long id) {
    Release release = releaseRepository.findByIdNotDeleted(id)
        .orElseThrow(() -> new ResourceNotFoundException("Release not found with id: " + id));

    User currentUser = getCurrentUser();
    release.setDeletedAt(LocalDateTime.now());
    release.setDeletedBy(currentUser);
    releaseRepository.save(release);
    log.info("Soft deleted release: {}", release.getName());
  }

  public ReleaseDTO updateStatus(Long id, ReleaseStatus status) {
    Release release = releaseRepository.findByIdNotDeleted(id)
        .orElseThrow(() -> new ResourceNotFoundException("Release not found with id: " + id));

    release.setStatus(status);
    if (status == ReleaseStatus.RELEASED && release.getReleaseDate() == null) {
      release.setReleaseDate(LocalDate.now());
    }
    Release saved = releaseRepository.save(release);
    log.info("Updated release status: {} to {}", saved.getName(), status);
    return toDTO(saved);
  }

  public ReleaseDTO linkCycle(Long releaseId, Long cycleId) {
    Release release = releaseRepository.findByIdWithCyclesNotDeleted(releaseId)
        .orElseThrow(() -> new ResourceNotFoundException("Release not found with id: " + releaseId));
    Cycle cycle = cycleRepository.findById(cycleId)
        .orElseThrow(() -> new ResourceNotFoundException("Cycle not found with id: " + cycleId));

    release.addCycle(cycle);
    Release saved = releaseRepository.save(release);
    log.info("Linked cycle {} to release {}", cycle.getName(), release.getName());
    return toDTOWithCycles(saved);
  }

  public ReleaseDTO unlinkCycle(Long releaseId, Long cycleId) {
    Release release = releaseRepository.findByIdWithCyclesNotDeleted(releaseId)
        .orElseThrow(() -> new ResourceNotFoundException("Release not found with id: " + releaseId));
    Cycle cycle = cycleRepository.findById(cycleId)
        .orElseThrow(() -> new ResourceNotFoundException("Cycle not found with id: " + cycleId));

    release.removeCycle(cycle);
    Release saved = releaseRepository.save(release);
    log.info("Unlinked cycle {} from release {}", cycle.getName(), release.getName());
    return toDTOWithCycles(saved);
  }

  /**
   * Calculate release progress from linked pitches, tasks, and bugs.
   */
  @Transactional(readOnly = true)
  public ReleaseProgressDTO calculateProgress(Long releaseId) {
    Release release = releaseRepository.findByIdNotDeleted(releaseId)
        .orElseThrow(() -> new ResourceNotFoundException("Release not found with id: " + releaseId));

    // Get all items linked to this release
    List<Pitch> pitches = pitchRepository.findByTargetReleaseIdNotDeleted(releaseId);
    List<Task> tasks = taskRepository.findByTargetReleaseIdNotDeleted(releaseId);
    List<BugReport> bugs = bugReportRepository.findByTargetReleaseId(releaseId);

    // Calculate pitch metrics
    int totalPitches = pitches.size();
    int completedPitches = (int) pitches.stream()
        .filter(p -> p.getStatus() == PitchStatus.DONE)
        .count();
    int inProgressPitches = (int) pitches.stream()
        .filter(p -> p.getStatus() == PitchStatus.IN_PROGRESS || p.getStatus() == PitchStatus.TESTING)
        .count();

    // Calculate task metrics
    int totalTasks = tasks.size();
    int completedTasks = (int) tasks.stream()
        .filter(t -> t.getStatus() == TaskStatus.DONE)
        .count();
    int inProgressTasks = (int) tasks.stream()
        .filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS || t.getStatus() == TaskStatus.IN_REVIEW)
        .count();
    int blockedTasks = (int) tasks.stream()
        .filter(t -> t.getStatus() == TaskStatus.BLOCKED)
        .count();

    // Calculate bug metrics
    int totalBugs = bugs.size();
    int resolvedBugs = (int) bugs.stream()
        .filter(b -> b.getStatus() == BugStatus.RESOLVED || b.getStatus() == BugStatus.VERIFIED || b.getStatus() == BugStatus.CLOSED)
        .count();
    int openBugs = (int) bugs.stream()
        .filter(b -> b.getStatus() == BugStatus.OPEN || b.getStatus() == BugStatus.REOPENED)
        .count();
    int criticalBugs = (int) bugs.stream()
        .filter(b -> b.getSeverity().name().equals("CRITICAL") || b.getSeverity().name().equals("BLOCKER"))
        .count();

    // Bug fix tracking (target vs actual release)
    int bugsOnTarget = 0;
    int bugsSlipped = 0;
    int bugsPulledForward = 0;

    for (BugReport bug : bugs) {
      if (bug.getFixedInRelease() != null) {
        if (bug.getFixedInRelease().getId().equals(releaseId)) {
          bugsOnTarget++;
        } else if (bug.getFixedInRelease().getTargetDate() != null && release.getTargetDate() != null) {
          if (bug.getFixedInRelease().getTargetDate().isAfter(release.getTargetDate())) {
            bugsSlipped++;
          } else {
            bugsPulledForward++;
          }
        }
      }
    }

    // Bugs by status
    Map<String, Integer> bugsByStatus = new HashMap<>();
    for (BugReport bug : bugs) {
      String status = bug.getStatus().name();
      bugsByStatus.merge(status, 1, Integer::sum);
    }

    // Calculate overall completion percentage (weighted: pitches 40%, tasks 40%, bugs 20%)
    double pitchCompletion = totalPitches > 0 ? (completedPitches * 100.0) / totalPitches : 100;
    double taskCompletion = totalTasks > 0 ? (completedTasks * 100.0) / totalTasks : 100;
    double bugCompletion = totalBugs > 0 ? (resolvedBugs * 100.0) / totalBugs : 100;

    double completionPercentage;
    if (totalPitches == 0 && totalTasks == 0 && totalBugs == 0) {
      completionPercentage = 0;
    } else {
      completionPercentage = (pitchCompletion * 0.4) + (taskCompletion * 0.4) + (bugCompletion * 0.2);
    }

    // Determine risk level and reason
    String riskLevel = release.getRiskLevel().name();
    String riskReason = null;

    if (criticalBugs > 0) {
      riskLevel = "CRITICAL";
      riskReason = criticalBugs + " critical/blocker bugs open";
    } else if (blockedTasks > 2) {
      riskLevel = "HIGH";
      riskReason = blockedTasks + " tasks blocked";
    } else if (completionPercentage < 50 && release.getTargetDate() != null 
        && release.getTargetDate().isBefore(LocalDate.now().plusWeeks(2))) {
      riskLevel = "HIGH";
      riskReason = "Low completion with release date approaching";
    }

    return ReleaseProgressDTO.builder()
        .completionPercentage(Math.round(completionPercentage * 10) / 10.0)
        .totalPitches(totalPitches)
        .completedPitches(completedPitches)
        .inProgressPitches(inProgressPitches)
        .totalTasks(totalTasks)
        .completedTasks(completedTasks)
        .inProgressTasks(inProgressTasks)
        .blockedTasks(blockedTasks)
        .totalBugs(totalBugs)
        .resolvedBugs(resolvedBugs)
        .openBugs(openBugs)
        .criticalBugs(criticalBugs)
        .bugsOnTarget(bugsOnTarget)
        .bugsSlipped(bugsSlipped)
        .bugsPulledForward(bugsPulledForward)
        .bugsByStatus(bugsByStatus)
        .riskLevel(riskLevel)
        .riskReason(riskReason)
        .build();
  }

  private User getCurrentUser() {
    try {
      String username = SecurityContextHolder.getContext().getAuthentication().getName();
      return userRepository.findByUsername(username).orElse(null);
    } catch (Exception e) {
      log.warn("Failed to get current user: {}", e.getMessage());
      return null;
    }
  }

  private ReleaseDTO toDTO(Release release) {
    ReleaseDTO dto = ReleaseDTO.builder()
        .id(release.getId())
        .name(release.getName())
        .version(release.getVersion())
        .description(release.getDescription())
        .status(release.getStatus())
        .riskLevel(release.getRiskLevel())
        .targetDate(release.getTargetDate())
        .releaseDate(release.getReleaseDate())
        .releaseNotes(release.getReleaseNotes())
        .sortOrder(release.getSortOrder())
        .projectId(release.getProject().getId())
        .projectName(release.getProject().getName())
        .projectKey(release.getProject().getProjectKey())
        .createdAt(release.getCreatedAt())
        .updatedAt(release.getUpdatedAt())
        .build();

    return dto;
  }

  private ReleaseDTO toDTOWithCycles(Release release) {
    ReleaseDTO dto = toDTO(release);

    List<ReleaseDTO.CycleSummaryDTO> cycleSummaries = release.getCycles().stream()
        .map(cycle -> ReleaseDTO.CycleSummaryDTO.builder()
            .id(cycle.getId())
            .name(cycle.getName())
            .phase(cycle.getPhase().name())
            .startDate(cycle.getStartDate())
            .endDate(cycle.getEndDate())
            .isActive(cycle.getIsActive())
            .build())
        .collect(Collectors.toList());

    dto.setCycles(cycleSummaries);
    return dto;
  }
}
