package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.qa.*;
import com.github.farzadsedaghatbin.shipflow.entity.*;
import com.github.farzadsedaghatbin.shipflow.entity.enums.*;
import com.github.farzadsedaghatbin.shipflow.repository.*;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service for managing bug reports. */
@Service
@RequiredArgsConstructor
@Slf4j
public class BugReportService {

  private final BugReportRepository bugReportRepository;
  private final TestRunRepository testRunRepository;
  private final PitchRepository pitchRepository;
  private final MessageService messageService;
  private final CycleRepository cycleRepository;
  private final TeamRepository teamRepository;
  private final UserRepository userRepository;
  private final HillChartPointRepository hillChartPointRepository;
  private final TaskRepository taskRepository;

  @Value("${app.qa.test-management.enabled:true}")
  private boolean testManagementEnabled;

  /** Create a new bug report. */
  @Transactional
  public BugReportDTO createBugReport(CreateBugReportRequest request, Long userId) {
    checkFeatureEnabled();

    User reporter =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

    BugReport bugReport =
        BugReport.builder()
            .bugKey(generateBugKey())
            .title(request.getTitle())
            .description(request.getDescription())
            .stepsToReproduce(request.getStepsToReproduce())
            .expectedBehavior(request.getExpectedBehavior())
            .actualBehavior(request.getActualBehavior())
            .environment(request.getEnvironment())
            .severity(request.getSeverity())
            .status(request.getStatus() != null ? request.getStatus() : BugStatus.OPEN)
            .tags(request.getTags() != null ? String.join(",", request.getTags()) : null)
            .attachments(request.getAttachments())
            .reporter(reporter)
            .build();

    // Set relationships
    if (request.getPitchId() != null) {
      Pitch pitch =
          pitchRepository
              .findById(request.getPitchId())
              .orElseThrow(
                  () -> new IllegalArgumentException("Pitch not found: " + request.getPitchId()));
      bugReport.setPitch(pitch);
      bugReport.setCycle(pitch.getCycle());
      bugReport.setTeam(pitch.getTeam());
    } else {
      if (request.getCycleId() != null) {
        Cycle cycle =
            cycleRepository
                .findById(request.getCycleId())
                .orElseThrow(
                    () -> new IllegalArgumentException("Cycle not found: " + request.getCycleId()));
        bugReport.setCycle(cycle);
      }
      if (request.getTeamId() != null) {
        Team team =
            teamRepository
                .findById(request.getTeamId())
                .orElseThrow(
                    () -> new IllegalArgumentException("Team not found: " + request.getTeamId()));
        bugReport.setTeam(team);
      }
    }

    if (request.getTestRunId() != null) {
      TestRun testRun =
          testRunRepository
              .findById(request.getTestRunId())
              .orElseThrow(
                  () ->
                      new IllegalArgumentException(
                          "Test run not found: " + request.getTestRunId()));
      bugReport.setTestRun(testRun);
    }

    // Set scope if provided
    if (request.getScopeId() != null) {
      HillChartPoint scope =
          hillChartPointRepository
              .findById(request.getScopeId())
              .orElseThrow(
                  () -> new IllegalArgumentException("Scope not found: " + request.getScopeId()));
      bugReport.setScope(scope);
    }

    // Set task if provided
    if (request.getTaskId() != null) {
      Task task =
          taskRepository
              .findById(request.getTaskId())
              .orElseThrow(
                  () -> new IllegalArgumentException("Task not found: " + request.getTaskId()));
      bugReport.setTask(task);
    }

    if (request.getAssigneeId() != null) {
      User assignee =
          userRepository
              .findById(request.getAssigneeId())
              .orElseThrow(
                  () ->
                      new IllegalArgumentException(
                          "Assignee not found: " + request.getAssigneeId()));
      bugReport.setAssignee(assignee);
    }

    bugReport = bugReportRepository.save(bugReport);
    log.info("Created bug report: {} by user: {}", bugReport.getBugKey(), userId);

    return toDTO(bugReport);
  }

  /** Update an existing bug report. */
  @Transactional
  public BugReportDTO updateBugReport(Long id, UpdateBugReportRequest request, Long userId) {
    checkFeatureEnabled();

    BugReport bugReport =
        bugReportRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Bug report not found: " + id));

    if (request.getTitle() != null) bugReport.setTitle(request.getTitle());
    if (request.getDescription() != null) bugReport.setDescription(request.getDescription());
    if (request.getStepsToReproduce() != null)
      bugReport.setStepsToReproduce(request.getStepsToReproduce());
    if (request.getExpectedBehavior() != null)
      bugReport.setExpectedBehavior(request.getExpectedBehavior());
    if (request.getActualBehavior() != null)
      bugReport.setActualBehavior(request.getActualBehavior());
    if (request.getEnvironment() != null) bugReport.setEnvironment(request.getEnvironment());
    if (request.getSeverity() != null) bugReport.setSeverity(request.getSeverity());
    if (request.getTags() != null) bugReport.setTags(String.join(",", request.getTags()));
    if (request.getAttachments() != null) bugReport.setAttachments(request.getAttachments());
    if (request.getResolution() != null) bugReport.setResolution(request.getResolution());

    // Handle status change
    if (request.getStatus() != null) {
      BugStatus oldStatus = bugReport.getStatus();
      bugReport.setStatus(request.getStatus());

      // Set resolved timestamp when transitioning to resolved status
      if (isResolvedStatus(request.getStatus()) && !isResolvedStatus(oldStatus)) {
        bugReport.setResolvedAt(LocalDateTime.now());
      }
    }

    // Update relationships
    if (request.getPitchId() != null) {
      Pitch pitch =
          pitchRepository
              .findById(request.getPitchId())
              .orElseThrow(
                  () -> new IllegalArgumentException("Pitch not found: " + request.getPitchId()));
      bugReport.setPitch(pitch);
    }
    if (request.getCycleId() != null) {
      Cycle cycle =
          cycleRepository
              .findById(request.getCycleId())
              .orElseThrow(
                  () -> new IllegalArgumentException("Cycle not found: " + request.getCycleId()));
      bugReport.setCycle(cycle);
    }
    if (request.getTeamId() != null) {
      Team team =
          teamRepository
              .findById(request.getTeamId())
              .orElseThrow(
                  () -> new IllegalArgumentException("Team not found: " + request.getTeamId()));
      bugReport.setTeam(team);
    }

    // Update scope if provided
    if (request.getScopeId() != null) {
      HillChartPoint scope =
          hillChartPointRepository
              .findById(request.getScopeId())
              .orElseThrow(
                  () -> new IllegalArgumentException("Scope not found: " + request.getScopeId()));
      bugReport.setScope(scope);
    }

    // Update task if provided
    if (request.getTaskId() != null) {
      Task task =
          taskRepository
              .findById(request.getTaskId())
              .orElseThrow(
                  () -> new IllegalArgumentException("Task not found: " + request.getTaskId()));
      bugReport.setTask(task);
    }

    if (request.getAssigneeId() != null) {
      User assignee =
          userRepository
              .findById(request.getAssigneeId())
              .orElseThrow(
                  () ->
                      new IllegalArgumentException(
                          "Assignee not found: " + request.getAssigneeId()));
      bugReport.setAssignee(assignee);
    }

    bugReport = bugReportRepository.save(bugReport);
    log.info("Updated bug report: {} by user: {}", bugReport.getBugKey(), userId);

    return toDTO(bugReport);
  }

  /** Delete a bug report. */
  @Transactional
  public void deleteBugReport(Long id) {
    checkFeatureEnabled();

    BugReport bugReport =
        bugReportRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Bug report not found: " + id));

    bugReportRepository.delete(bugReport);
    log.info("Deleted bug report: {}", bugReport.getBugKey());
  }

  /** Get a bug report by ID. */
  @Transactional(readOnly = true)
  public BugReportDTO getBugReportById(Long id) {
    checkFeatureEnabled();

    BugReport bugReport =
        bugReportRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Bug report not found: " + id));

    return toDTO(bugReport);
  }

  /** Get a bug report by key. */
  @Transactional(readOnly = true)
  public BugReportDTO getBugReportByKey(String key) {
    checkFeatureEnabled();

    BugReport bugReport =
        bugReportRepository
            .findByBugKey(key)
            .orElseThrow(() -> new IllegalArgumentException("Bug report not found: " + key));

    return toDTO(bugReport);
  }

  /** Get all bug reports with pagination. */
  @Transactional(readOnly = true)
  public Page<BugReportDTO> getAllBugReports(Pageable pageable) {
    checkFeatureEnabled();

    return bugReportRepository.findAll(pageable).map(this::toDTO);
  }

  /** Get all bug reports (non-pageable for backward compatibility). */
  @Transactional(readOnly = true)
  public List<BugReportDTO> getAllBugReports() {
    checkFeatureEnabled();

    return bugReportRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
  }

  /** Get bug reports by pitch. */
  @Transactional(readOnly = true)
  public List<BugReportDTO> getBugReportsByPitch(Long pitchId) {
    checkFeatureEnabled();

    return bugReportRepository.findByPitchId(pitchId).stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
  }

  /** Get bug reports by cycle. */
  @Transactional(readOnly = true)
  public List<BugReportDTO> getBugReportsByCycle(Long cycleId) {
    checkFeatureEnabled();

    return bugReportRepository.findByCycleId(cycleId).stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
  }

  /** Get open bug reports. */
  @Transactional(readOnly = true)
  public List<BugReportDTO> getOpenBugReports() {
    checkFeatureEnabled();

    return bugReportRepository.findAllOpen().stream().map(this::toDTO).collect(Collectors.toList());
  }

  /** Get open bug reports by pitch. */
  @Transactional(readOnly = true)
  public List<BugReportDTO> getOpenBugReportsByPitch(Long pitchId) {
    checkFeatureEnabled();

    return bugReportRepository.findOpenByPitchId(pitchId).stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
  }

  /** Get bug reports assigned to a user. */
  @Transactional(readOnly = true)
  public List<BugReportDTO> getBugReportsByAssignee(Long userId) {
    checkFeatureEnabled();

    return bugReportRepository.findByAssigneeId(userId).stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
  }

  /** Get bug reports reported by a user. */
  @Transactional(readOnly = true)
  public List<BugReportDTO> getBugReportsByReporter(Long userId) {
    checkFeatureEnabled();

    return bugReportRepository.findByReporterId(userId).stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
  }

  // ========== Multi-filter methods ==========

  /** Get bug reports with multi-selection filters. */
  @Transactional(readOnly = true)
  public Page<BugReportDTO> getBugReportsWithFilters(
      Long projectId,
      Long cycleId,
      Long pitchId,
      List<BugStatus> statuses,
      List<BugSeverity> severities,
      List<Long> assigneeIds,
      Boolean exclude,
      Pageable pageable) {
    checkFeatureEnabled();

    // Convert empty lists to null for the query
    List<BugStatus> statusList = (statuses != null && !statuses.isEmpty()) ? statuses : null;
    List<BugSeverity> severityList =
        (severities != null && !severities.isEmpty()) ? severities : null;
    List<Long> assigneeList = (assigneeIds != null && !assigneeIds.isEmpty()) ? assigneeIds : null;

    if (exclude != null && exclude) {
      return bugReportRepository
          .findWithExclusionFilters(
              projectId, cycleId, pitchId, statusList, severityList, assigneeList, pageable)
          .map(this::toDTO);
    } else {
      return bugReportRepository
          .findWithFilters(projectId, cycleId, pitchId, statusList, severityList, assigneeList, pageable)
          .map(this::toDTO);
    }
  }

  /** Generate unique bug key. */
  private String generateBugKey() {
    Integer maxNumber = bugReportRepository.findMaxBugKeyNumber();
    int nextNumber = (maxNumber != null ? maxNumber : 0) + 1;
    return String.format("BUG-%03d", nextNumber);
  }

  private boolean isResolvedStatus(BugStatus status) {
    return status == BugStatus.RESOLVED
        || status == BugStatus.VERIFIED
        || status == BugStatus.CLOSED
        || status == BugStatus.WONT_FIX
        || status == BugStatus.DUPLICATE;
  }

  private void checkFeatureEnabled() {
    if (!testManagementEnabled) {
      throw new IllegalArgumentException(
          messageService.getMessage("error.qa.test.management.disabled"));
    }
  }

  /** Convert entity to DTO. */
  public BugReportDTO toDTO(BugReport bugReport) {
    return BugReportDTO.builder()
        .id(bugReport.getId())
        .bugKey(bugReport.getBugKey())
        .title(bugReport.getTitle())
        .description(bugReport.getDescription())
        .stepsToReproduce(bugReport.getStepsToReproduce())
        .expectedBehavior(bugReport.getExpectedBehavior())
        .actualBehavior(bugReport.getActualBehavior())
        .environment(bugReport.getEnvironment())
        .pitchId(bugReport.getPitch() != null ? bugReport.getPitch().getId() : null)
        .pitchTitle(bugReport.getPitch() != null ? bugReport.getPitch().getTitle() : null)
        .cycleId(bugReport.getCycle() != null ? bugReport.getCycle().getId() : null)
        .cycleName(bugReport.getCycle() != null ? bugReport.getCycle().getName() : null)
        .teamId(bugReport.getTeam() != null ? bugReport.getTeam().getId() : null)
        .teamName(bugReport.getTeam() != null ? bugReport.getTeam().getName() : null)
        .testRunId(bugReport.getTestRun() != null ? bugReport.getTestRun().getId() : null)
        .scopeId(bugReport.getScope() != null ? bugReport.getScope().getId() : null)
        .scopeName(bugReport.getScope() != null ? bugReport.getScope().getScope() : null)
        .taskId(bugReport.getTask() != null ? bugReport.getTask().getId() : null)
        .taskTitle(bugReport.getTask() != null ? bugReport.getTask().getTitle() : null)
        .severity(bugReport.getSeverity())
        .status(bugReport.getStatus())
        .tags(bugReport.getTags())
        .tagList(bugReport.getTags() != null ? Arrays.asList(bugReport.getTags().split(",")) : null)
        .attachments(bugReport.getAttachments())
        .reporterId(bugReport.getReporter() != null ? bugReport.getReporter().getId() : null)
        .reporterName(
            bugReport.getReporter() != null ? bugReport.getReporter().getUsername() : null)
        .assigneeId(bugReport.getAssignee() != null ? bugReport.getAssignee().getId() : null)
        .assigneeName(
            bugReport.getAssignee() != null ? bugReport.getAssignee().getUsername() : null)
        .resolution(bugReport.getResolution())
        .resolvedAt(bugReport.getResolvedAt())
        .createdAt(bugReport.getCreatedAt())
        .updatedAt(bugReport.getUpdatedAt())
        .build();
  }
}
