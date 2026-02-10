package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.CreateCycleRequest;
import com.github.farzadsedaghatbin.shipflow.dto.CycleDTO;
import com.github.farzadsedaghatbin.shipflow.dto.CycleRetroStatusDTO;
import com.github.farzadsedaghatbin.shipflow.dto.cycle.PhaseTransitionResultDTO;
import com.github.farzadsedaghatbin.shipflow.dto.cycle.PhaseTransitionResultDTO.PhaseTransitionWarning;
import com.github.farzadsedaghatbin.shipflow.dto.cycle.PhaseTransitionResultDTO.WarningCategory;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.UserRole;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CyclePhase;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PitchStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.RetroStatus;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import com.github.farzadsedaghatbin.shipflow.repository.BettingDecisionRepository;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
import com.github.farzadsedaghatbin.shipflow.repository.ProjectRepository;
import com.github.farzadsedaghatbin.shipflow.repository.RetroRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.github.farzadsedaghatbin.shipflow.event.CycleStatusChangedEvent;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CycleService {

  private final CycleRepository cycleRepository;
  private final ProjectRepository projectRepository;
  private final PitchRepository pitchRepository;
  private final RetroRepository retroRepository;
  private final BettingDecisionRepository bettingDecisionRepository;
  private final DashboardNotificationService notificationService;
  private final UserRepository userRepository;
  private final OrganizationSettingsService organizationSettingsService;
  private final MessageService messageService;
  private final ApplicationEventPublisher eventPublisher;

  public List<CycleDTO> getAllCycles() {
    return cycleRepository.findAllByOrderByStartDateDesc().stream().map(this::toDTO).collect(Collectors.toList());
  }

  /**
   * Get cycles that the current user has access to. ADMINs can see all cycles,
   * other users see only cycles from accessible projects.
   */
  public List<CycleDTO> getAccessibleCycles() {
    User currentUser = getCurrentUser();
    if (currentUser == null) {
      throw new AccessDeniedException(messageService.getMessage("error.user.not.authenticated"));
    }

    // ADMINs can see all cycles
    if (currentUser.getRole() == UserRole.ADMIN) {
      return getAllCycles();
    }

    return cycleRepository.findAccessibleCyclesByUserId(currentUser.getId()).stream().map(this::toDTO)
        .collect(Collectors.toList());
  }

  /** Get active cycles that the current user has access to. */
  public List<CycleDTO> getAccessibleActiveCycles() {
    User currentUser = getCurrentUser();
    if (currentUser == null) {
      throw new AccessDeniedException(messageService.getMessage("error.user.not.authenticated"));
    }

    // ADMINs can see all active cycles
    if (currentUser.getRole() == UserRole.ADMIN) {
      return getActiveCycles();
    }

    return cycleRepository.findAccessibleActiveCyclesByUserId(currentUser.getId()).stream().map(this::toDTO)
        .collect(Collectors.toList());
  }

  public List<CycleDTO> getCyclesByProject(Long projectId) {
    return cycleRepository.findByProjectIdOrderByStartDateDesc(projectId).stream().map(this::toDTO)
        .collect(Collectors.toList());
  }

  public List<CycleDTO> getActiveCyclesByProject(Long projectId) {
    return cycleRepository.findByProjectIdAndIsActiveTrue(projectId).stream().map(this::toDTO)
        .collect(Collectors.toList());
  }

  public List<CycleDTO> getActiveCycles() {
    return cycleRepository.findByIsActiveTrue().stream().map(this::toDTO).collect(Collectors.toList());
  }

  public CycleDTO getCycleById(Long id) {
    Cycle cycle = cycleRepository.findByIdWithProject(id)
        .orElseThrow(() -> new ResourceNotFoundException("Cycle not found with id: " + id));
    return toDTO(cycle);
  }

  public CycleDTO createCycle(CreateCycleRequest request) {
    Project project = projectRepository.findById(request.getProjectId()).orElseThrow(
        () -> new ResourceNotFoundException("Project not found with id: " + request.getProjectId()));

    // Calculate or validate end date
    LocalDate endDate = calculateOrValidateEndDate(request.getStartDate(), request.getEndDate());

    Cycle cycle = Cycle.builder().project(project).name(request.getName()).startDate(request.getStartDate())
        .endDate(endDate).phase(request.getPhase() != null ? request.getPhase() : CyclePhase.BUILD)
        .isActive(true).build();

    Cycle saved = cycleRepository.save(cycle);
    return toDTO(saved);
  }

  public CycleDTO updateCycle(Long id, CreateCycleRequest request) {
    Cycle cycle = cycleRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Cycle not found with id: " + id));

    // Allow changing project
    if (request.getProjectId() != null
        && (cycle.getProject() == null || !cycle.getProject().getId().equals(request.getProjectId()))) {
      Project project = projectRepository.findById(request.getProjectId()).orElseThrow(
          () -> new ResourceNotFoundException("Project not found with id: " + request.getProjectId()));
      cycle.setProject(project);
    }

    // Calculate or validate end date
    LocalDate endDate = calculateOrValidateEndDate(request.getStartDate(), request.getEndDate());

    cycle.setName(request.getName());
    cycle.setStartDate(request.getStartDate());
    cycle.setEndDate(endDate);
    cycle.setPhase(request.getPhase());

    Cycle saved = cycleRepository.save(cycle);
    return toDTO(saved);
  }

  public CycleDTO updatePhase(Long id, CyclePhase phase) {
    Cycle cycle = cycleRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Cycle not found with id: " + id));

    CyclePhase oldPhase = cycle.getPhase();
    cycle.setPhase(phase);
    Cycle saved = cycleRepository.save(cycle);

    // Send notifications if phase actually changed
    if (oldPhase != phase) {
      notificationService.notifyCyclePhaseChange(saved, oldPhase, phase);
      
      // Publish event for narrative auto-regeneration
      eventPublisher.publishEvent(new CycleStatusChangedEvent(
          this, 
          saved.getId(), 
          saved.getName(),
          oldPhase != null ? oldPhase.name() : null, 
          phase.name()
      ));
    }

    return toDTO(saved);
  }

  /**
   * Transition to a new phase with validation and warnings.
   * Shape Up phases follow: SHAPING → BETTING → BUILD → COOLDOWN
   * 
   * Transitions proceed with warnings (not blocked) if prerequisites aren't met:
   * - SHAPING → BETTING: warn if no shaped pitches
   * - BETTING → BUILD: warn if shaped pitches have no betting decision
   * - BUILD → COOLDOWN: warn if pitches aren't complete, no retrospective
   * - COOLDOWN → SHAPING: warn if retrospective not closed
   *
   * @param id cycle ID
   * @param targetPhase the phase to transition to
   * @return result with the transition outcome and any warnings
   */
  public PhaseTransitionResultDTO transitionPhase(Long id, CyclePhase targetPhase) {
    Cycle cycle = cycleRepository.findByIdWithProject(id)
        .orElseThrow(() -> new ResourceNotFoundException("Cycle not found with id: " + id));

    CyclePhase currentPhase = cycle.getPhase();
    List<PhaseTransitionWarning> warnings = new ArrayList<>();

    // Check for skipped phases (non-sequential transitions)
    if (isPhaseSkipped(currentPhase, targetPhase)) {
      warnings.add(PhaseTransitionWarning.builder()
          .code("SKIPPED_PHASE")
          .message(String.format("Skipping from %s directly to %s (normal flow: SHAPING → BETTING → BUILD → COOLDOWN)", 
              currentPhase, targetPhase))
          .category(WarningCategory.SKIPPED_PHASE)
          .build());
      log.warn("Cycle {} skipping phase: {} → {}", id, currentPhase, targetPhase);
    }

    // Validate based on current phase being left
    switch (currentPhase) {
      case SHAPING:
        warnings.addAll(validateLeavingShaping(cycle));
        break;
      case BETTING:
        warnings.addAll(validateLeavingBetting(cycle));
        break;
      case BUILD:
        warnings.addAll(validateLeavingBuild(cycle));
        break;
      case COOLDOWN:
        warnings.addAll(validateLeavingCooldown(cycle));
        break;
    }

    // Log warnings but proceed with transition
    if (!warnings.isEmpty()) {
      log.warn("Phase transition for cycle {} ({} → {}) proceeding with {} warnings: {}",
          id, currentPhase, targetPhase, warnings.size(), 
          warnings.stream().map(PhaseTransitionWarning::getCode).collect(Collectors.joining(", ")));
    }

    // Perform the transition
    cycle.setPhase(targetPhase);
    Cycle saved = cycleRepository.save(cycle);

    // Send notifications
    if (currentPhase != targetPhase) {
      notificationService.notifyCyclePhaseChange(saved, currentPhase, targetPhase);
    }

    return PhaseTransitionResultDTO.successWithWarnings(
        saved.getId(), saved.getName(), currentPhase, targetPhase, warnings);
  }

  private boolean isPhaseSkipped(CyclePhase from, CyclePhase to) {
    // Define normal phase order
    int fromOrdinal = from.ordinal();
    int toOrdinal = to.ordinal();
    
    // Normal progression is +1, wrapping from COOLDOWN to SHAPING
    if (from == CyclePhase.COOLDOWN && to == CyclePhase.SHAPING) {
      return false; // This is the normal wrap-around
    }
    
    // Going backwards is always a skip (except COOLDOWN → SHAPING)
    if (toOrdinal < fromOrdinal) {
      return true;
    }
    
    // Jumping more than one step forward is a skip
    return (toOrdinal - fromOrdinal) > 1;
  }

  private List<PhaseTransitionWarning> validateLeavingShaping(Cycle cycle) {
    List<PhaseTransitionWarning> warnings = new ArrayList<>();
    
    // Check if any pitches have been shaped
    long shapedCount = pitchRepository.countByCycleIdAndStatus(cycle.getId(), PitchStatus.SHAPED);
    if (shapedCount == 0) {
      warnings.add(PhaseTransitionWarning.builder()
          .code("NO_SHAPED_PITCHES")
          .message("No pitches have been shaped for this cycle")
          .category(WarningCategory.EMPTY_CYCLE)
          .affectedCount(0)
          .build());
    }
    
    return warnings;
  }

  private List<PhaseTransitionWarning> validateLeavingBetting(Cycle cycle) {
    List<PhaseTransitionWarning> warnings = new ArrayList<>();
    
    // Check for shaped pitches without betting decisions
    long shapedCount = pitchRepository.countByCycleIdAndStatus(cycle.getId(), PitchStatus.SHAPED);
    long decisionsCount = bettingDecisionRepository.countByCycleId(cycle.getId());
    
    if (shapedCount > decisionsCount) {
      int undecided = (int) (shapedCount - decisionsCount);
      warnings.add(PhaseTransitionWarning.builder()
          .code("PITCHES_WITHOUT_DECISIONS")
          .message(String.format("%d shaped pitch(es) have no betting decision recorded", undecided))
          .category(WarningCategory.BETTING_INCOMPLETE)
          .affectedCount(undecided)
          .build());
    }
    
    // Check if any pitches were committed (at least one should be for a productive build phase)
    long committedCount = bettingDecisionRepository.countCommittedByCycleId(cycle.getId());
    if (committedCount == 0 && shapedCount > 0) {
      warnings.add(PhaseTransitionWarning.builder()
          .code("NO_COMMITTED_PITCHES")
          .message("No pitches were committed for this cycle - build phase will have no work")
          .category(WarningCategory.UNCOMMITTED_PITCHES)
          .affectedCount((int) shapedCount)
          .build());
    }
    
    return warnings;
  }

  private List<PhaseTransitionWarning> validateLeavingBuild(Cycle cycle) {
    List<PhaseTransitionWarning> warnings = new ArrayList<>();
    
    // Check for incomplete pitches (STARTED or IN_PROGRESS)
    long incompleteCount = pitchRepository.countByCycleIdAndStatusIn(
        cycle.getId(), 
        List.of(PitchStatus.STARTED, PitchStatus.IN_PROGRESS, PitchStatus.TESTING));
    
    if (incompleteCount > 0) {
      warnings.add(PhaseTransitionWarning.builder()
          .code("INCOMPLETE_PITCHES")
          .message(String.format("%d pitch(es) are still in progress", incompleteCount))
          .category(WarningCategory.PITCHES_NOT_COMPLETE)
          .affectedCount((int) incompleteCount)
          .build());
    }
    
    // Check for retrospective
    boolean retroEnabled = cycle.getProject() != null 
        && Boolean.TRUE.equals(cycle.getProject().getEnableRetrospectives());
    if (retroEnabled) {
      long retroCount = retroRepository.countByCycleId(cycle.getId());
      if (retroCount == 0) {
        warnings.add(PhaseTransitionWarning.builder()
            .code("NO_RETROSPECTIVE")
            .message("No retrospective has been created for this cycle")
            .category(WarningCategory.NO_RETROSPECTIVE)
            .affectedCount(0)
            .build());
      }
    }
    
    return warnings;
  }

  private List<PhaseTransitionWarning> validateLeavingCooldown(Cycle cycle) {
    List<PhaseTransitionWarning> warnings = new ArrayList<>();
    
    // Check if retrospective was completed
    boolean retroEnabled = cycle.getProject() != null 
        && Boolean.TRUE.equals(cycle.getProject().getEnableRetrospectives());
    if (retroEnabled) {
      long closedRetros = retroRepository.countByCycleIdAndStatus(cycle.getId(), RetroStatus.CLOSED);
      if (closedRetros == 0) {
        long openRetros = retroRepository.countByCycleId(cycle.getId());
        if (openRetros > 0) {
          warnings.add(PhaseTransitionWarning.builder()
              .code("RETROSPECTIVE_NOT_CLOSED")
              .message("Retrospective exists but has not been closed")
              .category(WarningCategory.NO_RETROSPECTIVE)
              .affectedCount(1)
              .build());
        }
      }
    }
    
    return warnings;
  }

  public CycleDTO toggleActive(Long id) {
    Cycle cycle = cycleRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Cycle not found with id: " + id));

    cycle.setIsActive(!cycle.getIsActive());
    Cycle saved = cycleRepository.save(cycle);
    return toDTO(saved);
  }

  /**
   * Close/Deactivate a cycle with retro enforcement. Requires at least one closed
   * retrospective if retrospectives are enabled for the project.
   */
  public CycleDTO closeCycle(Long id) {
    Cycle cycle = cycleRepository.findByIdWithProject(id)
        .orElseThrow(() -> new ResourceNotFoundException("Cycle not found with id: " + id));

    // Check retrospective requirement
    CycleRetroStatusDTO retroStatus = getCycleRetroStatus(id);
    if (!retroStatus.getCanCloseCycle()) {
      throw new IllegalStateException(retroStatus.getMessage());
    }

    cycle.setIsActive(false);
    Cycle saved = cycleRepository.save(cycle);
    return toDTO(saved);
  }

  /** Get the retrospective completion status for a cycle. */
  public CycleRetroStatusDTO getCycleRetroStatus(Long cycleId) {
    Cycle cycle = cycleRepository.findByIdWithProject(cycleId)
        .orElseThrow(() -> new ResourceNotFoundException("Cycle not found with id: " + cycleId));

    boolean retroEnabled = cycle.getProject() != null
        && Boolean.TRUE.equals(cycle.getProject().getEnableRetrospectives());

    long totalRetros = retroRepository.countByCycleId(cycleId);
    long closedRetros = retroRepository.countByCycleIdAndStatus(cycleId, RetroStatus.CLOSED);

    boolean canClose;
    String message;

    if (!retroEnabled) {
      canClose = true;
      message = "Retrospectives are disabled for this project";
    } else if (closedRetros >= 1) {
      canClose = true;
      message = "Cycle can be closed - retrospective completed";
    } else {
      canClose = false;
      message = "You can't close this cycle yet — create and close at least one Retro for this cycle.";
    }

    return CycleRetroStatusDTO.builder().cycleId(cycleId).cycleName(cycle.getName()).totalRetros((int) totalRetros)
        .closedRetros((int) closedRetros).canCloseCycle(canClose).message(message).build();
  }

  public void deleteCycle(Long id) {
    if (!cycleRepository.existsById(id)) {
      throw new ResourceNotFoundException("Cycle not found with id: " + id);
    }
    cycleRepository.deleteById(id);
  }

  private CycleDTO toDTO(Cycle cycle) {
    CycleDTO.CycleDTOBuilder builder = CycleDTO.builder().id(cycle.getId()).name(cycle.getName())
        .startDate(cycle.getStartDate()).endDate(cycle.getEndDate()).phase(cycle.getPhase())
        .isActive(cycle.getIsActive()).pitchCount((int) pitchRepository.countByCycleIdNotDeleted(cycle.getId()))
        .teamCount(cycle.getTeams() != null ? cycle.getTeams().size() : 0);

    if (cycle.getProject() != null) {
      builder.projectId(cycle.getProject().getId()).projectName(cycle.getProject().getName())
          .projectKey(cycle.getProject().getProjectKey());
    }

    return builder.build();
  }

  /**
   * Calculate or validate cycle end date based on configuration and user
   * privileges. - If no end date provided: auto-calculate from start date +
   * organization's default cycle length - If end date provided: only
   * ADMIN/PROJECT_MANAGER can override, otherwise throw exception
   */
  private LocalDate calculateOrValidateEndDate(LocalDate startDate, LocalDate providedEndDate) {
    if (providedEndDate == null) {
      // Auto-calculate from organization settings
      return calculateEndDateFromConfiguration(startDate);
    }

    // User wants to override - check if they have privilege
    if (!currentUserCanOverrideCycleDates()) {
      throw new AccessDeniedException(
          "Only users with ADMIN or PROJECT_MANAGER role can set custom cycle end dates. "
              + "The system will automatically calculate the end date based on the configured cycle length.");
    }

    // User has privilege, use their custom date
    return providedEndDate;
  }

  /**
   * Calculate end date from start date using organization's default cycle length
   */
  private LocalDate calculateEndDateFromConfiguration(LocalDate startDate) {
    var settings = organizationSettingsService.getSettings();
    Integer cycleLengthWeeks = settings.getDefaultCycleLengthWeeks();

    if (cycleLengthWeeks == null || cycleLengthWeeks <= 0) {
      log.warn(
          "Invalid or missing default cycle length configuration (value: {}). Falling back to Shape Up standard of 6 weeks.",
          cycleLengthWeeks);
      cycleLengthWeeks = 6; // Fallback to Shape Up standard
    }

    return startDate.plusWeeks(cycleLengthWeeks);
  }

  /**
   * Check if current user has privilege to override cycle dates. Only ADMIN and
   * PROJECT_MANAGER roles can set custom dates.
   */
  private boolean currentUserCanOverrideCycleDates() {
    try {
      String username = SecurityContextHolder.getContext().getAuthentication().getName();
      User user = userRepository.findByUsername(username).orElse(null);

      if (user == null) {
        return false;
      }

      UserRole role = user.getRole();
      return role == UserRole.ADMIN || role == UserRole.MANAGER;
    } catch (Exception e) {
      // Throw exception to surface authentication/authorization issues
      log.error("Failed to determine current user's role when checking cycle date override privileges", e);
      throw new RuntimeException(messageService.getMessage("error.cycle.permissions.verification.failed"), e);
    }
  }

  /** Get the currently authenticated user. */
  private User getCurrentUser() {
    try {
      String username = SecurityContextHolder.getContext().getAuthentication().getName();
      return userRepository.findByUsername(username).orElse(null);
    } catch (Exception e) {
      log.warn("Failed to get current user: {}", e.getMessage());
      return null;
    }
  }
}
