package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.CreateCycleRequest;
import com.github.farzadsedaghatbin.shipflow.dto.CycleDTO;
import com.github.farzadsedaghatbin.shipflow.dto.CycleRetroStatusDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.UserRole;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CyclePhase;
import com.github.farzadsedaghatbin.shipflow.entity.enums.RetroStatus;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
import com.github.farzadsedaghatbin.shipflow.repository.ProjectRepository;
import com.github.farzadsedaghatbin.shipflow.repository.RetroRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CycleService {

  private final CycleRepository cycleRepository;
  private final ProjectRepository projectRepository;
  private final PitchRepository pitchRepository;
  private final RetroRepository retroRepository;
  private final UserRepository userRepository;
  private final OrganizationSettingsService organizationSettingsService;
  private final MessageService messageService;

  @Autowired(required = false)
  private KnowledgeIngestionService knowledgeIngestionService;

  @Autowired(required = false)
  private RiskAnalysisService riskAnalysisService;

  @Cacheable(value = "cycles", key = "'all'")
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

    // ADMINs can see all active SHAPE_UP cycles (exclude KANBAN projects)
    if (currentUser.getRole() == UserRole.ADMIN) {
      return cycleRepository
          .findByIsActiveTrueAndProjectProjectTypeNot(com.github.farzadsedaghatbin.shipflow.entity.enums.ProjectType.KANBAN)
          .stream().map(this::toDTO).collect(Collectors.toList());
    }

    return cycleRepository.findAccessibleActiveCyclesByUserId(currentUser.getId()).stream().map(this::toDTO)
        .collect(Collectors.toList());
  }

  @Cacheable(value = "cycles", key = "'byProject:' + #projectId")
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

  @Cacheable(value = "cycles", key = "'detail:' + #id")
  public CycleDTO getCycleById(Long id) {
    Cycle cycle = cycleRepository.findByIdWithProject(id)
        .orElseThrow(() -> new ResourceNotFoundException("Cycle not found with id: " + id));
    return toDTO(cycle);
  }

  @CacheEvict(value = {"cycles", "roadmap"}, allEntries = true)
  public CycleDTO createCycle(CreateCycleRequest request) {
    Project project = projectRepository.findById(request.getProjectId()).orElseThrow(
        () -> new ResourceNotFoundException("Project not found with id: " + request.getProjectId()));

    // Calculate or validate end date
    LocalDate endDate = calculateOrValidateEndDate(request.getStartDate(), request.getEndDate());

    Cycle cycle = Cycle.builder().project(project).name(request.getName()).startDate(request.getStartDate())
        .endDate(endDate).phase(request.getPhase() != null ? request.getPhase() : CyclePhase.SHAPING_BUILDING)
        .isActive(true).sprintGoal(request.getSprintGoal()).velocityActual(request.getVelocityActual()).build();

    Cycle saved = cycleRepository.save(cycle);
    // Ingest into knowledge base for QA
    if (knowledgeIngestionService != null) {
      try {
        knowledgeIngestionService.ingestCycle(saved.getId());
      } catch (Exception e) {
        log.warn("Failed to ingest cycle to knowledge base: {}", e.getMessage());
      }
    }
    return toDTO(saved);
  }

  @CacheEvict(value = {"cycles", "roadmap"}, allEntries = true)
  public CycleDTO updateCycle(Long id, CreateCycleRequest request) {
    Cycle cycle = cycleRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Cycle not found with id: " + id));

    // Track whether dates changed for cache invalidation
    boolean datesChanged = !cycle.getStartDate().equals(request.getStartDate())
        || !cycle.getEndDate().equals(calculateOrValidateEndDate(request.getStartDate(), request.getEndDate()));

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
    if (request.getSprintGoal() != null) {
      cycle.setSprintGoal(request.getSprintGoal());
    }
    if (request.getVelocityActual() != null) {
      cycle.setVelocityActual(request.getVelocityActual());
    }

    Cycle saved = cycleRepository.save(cycle);

    // Invalidate risk analysis caches when cycle dates change
    if (datesChanged && riskAnalysisService != null) {
      try {
        riskAnalysisService.invalidateCycleCache(saved.getId());
        // Also invalidate pitch-level caches since pitch risk data includes cycle dates
        pitchRepository.findByCycleId(saved.getId()).forEach(pitch -> {
          try {
            riskAnalysisService.invalidatePitchCache(pitch.getId());
          } catch (Exception e) {
            log.warn("Failed to invalidate pitch risk cache for pitch {}: {}", pitch.getId(), e.getMessage());
          }
        });
        log.info("Invalidated risk caches for cycle {} and its pitches due to date change", saved.getId());
      } catch (Exception e) {
        log.warn("Failed to invalidate risk caches for cycle {}: {}", saved.getId(), e.getMessage());
      }
    }

    // Re-ingest into knowledge base for QA
    if (knowledgeIngestionService != null) {
      try {
        knowledgeIngestionService.ingestCycle(saved.getId());
      } catch (Exception e) {
        log.warn("Failed to re-ingest cycle to knowledge base: {}", e.getMessage());
      }
    }
    return toDTO(saved);
  }


  @CacheEvict(value = {"cycles", "roadmap"}, allEntries = true)
  public CycleDTO toggleActive(Long id) {
    Cycle cycle = cycleRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Cycle not found with id: " + id));

    cycle.setIsActive(!cycle.getIsActive());
    Cycle saved = cycleRepository.save(cycle);
    return toDTO(saved);
  }

  @CacheEvict(value = {"cycles", "roadmap"}, allEntries = true)
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

  @CacheEvict(value = {"cycles", "roadmap"}, allEntries = true)
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
        .teamCount((int) (cycle.getPitches() != null
            ? cycle.getPitches().stream().map(p -> p.getTeam()).filter(t -> t != null).distinct().count()
            : 0));

    if (cycle.getProject() != null) {
      builder.projectId(cycle.getProject().getId()).projectName(cycle.getProject().getName())
          .projectKey(cycle.getProject().getProjectKey())
          .projectType(cycle.getProject().getProjectType());
    }

    builder.sprintGoal(cycle.getSprintGoal()).velocityActual(cycle.getVelocityActual());

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
