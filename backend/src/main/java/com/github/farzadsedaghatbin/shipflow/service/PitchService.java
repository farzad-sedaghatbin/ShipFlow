package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.CreatePitchRequest;
import com.github.farzadsedaghatbin.shipflow.dto.PitchDependencyDTO;
import com.github.farzadsedaghatbin.shipflow.dto.PitchDTO;
import com.github.farzadsedaghatbin.shipflow.dto.ReorderRequest;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Epic;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.PitchDependency;
import com.github.farzadsedaghatbin.shipflow.entity.Release;
import com.github.farzadsedaghatbin.shipflow.entity.Team;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.UserRole;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PitchStatus;
import com.github.farzadsedaghatbin.shipflow.event.PitchStatusChangedEvent;
import com.github.farzadsedaghatbin.shipflow.exception.BadRequestException;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.repository.EpicRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PitchDependencyRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
import com.github.farzadsedaghatbin.shipflow.repository.ReleaseRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TeamRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import com.github.farzadsedaghatbin.shipflow.repository.WorkLogRepository;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PitchService {

  private final PitchRepository pitchRepository;
  private final CycleRepository cycleRepository;
  private final TeamRepository teamRepository;
  private final WorkLogRepository workLogRepository;
  private final UserRepository userRepository;
  private final EpicRepository epicRepository;
  private final ReleaseRepository releaseRepository;
  private final ApplicationEventPublisher eventPublisher;
  private final AICacheService cacheService;
  private final CapacityConfigService capacityConfigService;
  @org.springframework.beans.factory.annotation.Autowired(required = false)
  private PitchDependencyRepository pitchDependencyRepository;
  @org.springframework.beans.factory.annotation.Autowired(required = false)
  private PitchDependencyService pitchDependencyService;

  public List<PitchDTO> getAllPitches() {
    return pitchRepository.findAllNotDeleted().stream().map(this::toDTO).collect(Collectors.toList());
  }

  /**
   * Get pitches that the current user has access to. ADMINs can see all pitches,
   * other users see only pitches from accessible projects.
   */
  public List<PitchDTO> getAccessiblePitches() {
    User currentUser = getCurrentUser();
    if (currentUser == null) {
      throw new AccessDeniedException("User not authenticated");
    }

    // ADMINs can see all pitches
    if (currentUser.getRole() == UserRole.ADMIN) {
      return getAllPitches();
    }

    return pitchRepository.findAccessiblePitchesByUserId(currentUser.getId()).stream().map(this::toDTO)
        .collect(Collectors.toList());
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

  public List<PitchDTO> getPitchesByCycleId(Long cycleId) {
    // Includes pitches directly assigned to the cycle AND pitches assigned
    // to a betting slot that belongs to this cycle (handles cases where the
    // pitch's cycle_id was not set but the slot's cycle_id is set).
    return pitchRepository.findByCycleIdNotDeleted(cycleId).stream().map(this::toDTO).collect(Collectors.toList());
  }

  public List<PitchDTO> getPitchesByTeamId(Long teamId) {
    return pitchRepository.findByTeamIdNotDeleted(teamId).stream().map(this::toDTO).collect(Collectors.toList());
  }

  public List<PitchDTO> getPitchesByEpicId(Long epicId) {
    return pitchRepository.findByEpicIdNotDeleted(epicId).stream().map(this::toDTO).collect(Collectors.toList());
  }

  public List<PitchDTO> getPitchesByReleaseId(Long releaseId) {
    return pitchRepository.findByTargetReleaseIdNotDeleted(releaseId).stream().map(this::toDTO).collect(Collectors.toList());
  }

  @CacheEvict(value = "roadmap", allEntries = true)
  public PitchDTO linkToEpic(Long pitchId, Long epicId) {
    Pitch pitch = pitchRepository.findByIdNotDeleted(pitchId)
        .orElseThrow(() -> new ResourceNotFoundException("Pitch not found with id: " + pitchId));
    Epic epic = epicRepository.findByIdNotDeleted(epicId)
        .orElseThrow(() -> new ResourceNotFoundException("Epic not found with id: " + epicId));
    pitch.setEpic(epic);
    pitch.setUpdatedAt(LocalDateTime.now());
    return toDTO(pitchRepository.save(pitch));
  }

  @CacheEvict(value = "roadmap", allEntries = true)
  public PitchDTO unlinkFromEpic(Long pitchId) {
    Pitch pitch = pitchRepository.findByIdNotDeleted(pitchId)
        .orElseThrow(() -> new ResourceNotFoundException("Pitch not found with id: " + pitchId));
    pitch.setEpic(null);
    pitch.setUpdatedAt(LocalDateTime.now());
    return toDTO(pitchRepository.save(pitch));
  }

  @CacheEvict(value = "roadmap", allEntries = true)
  public PitchDTO setTargetRelease(Long pitchId, Long releaseId) {
    Pitch pitch = pitchRepository.findByIdNotDeleted(pitchId)
        .orElseThrow(() -> new ResourceNotFoundException("Pitch not found with id: " + pitchId));
    Release release = releaseRepository.findByIdNotDeleted(releaseId)
        .orElseThrow(() -> new ResourceNotFoundException("Release not found with id: " + releaseId));
    pitch.setTargetRelease(release);
    pitch.setUpdatedAt(LocalDateTime.now());
    return toDTO(pitchRepository.save(pitch));
  }

  @CacheEvict(value = "roadmap", allEntries = true)
  public PitchDTO clearTargetRelease(Long pitchId) {
    Pitch pitch = pitchRepository.findByIdNotDeleted(pitchId)
        .orElseThrow(() -> new ResourceNotFoundException("Pitch not found with id: " + pitchId));
    pitch.setTargetRelease(null);
    pitch.setUpdatedAt(LocalDateTime.now());
    return toDTO(pitchRepository.save(pitch));
  }

  public PitchDTO getPitchById(Long id) {
    Pitch pitch = pitchRepository.findByIdNotDeleted(id)
        .orElseThrow(() -> new IllegalArgumentException("Pitch not found with id: " + id));
    return toDTO(pitch);
  }

  /**
   * Create a lightweight idea (just title + optional description).
   * Ideas don't require cycle or appetite - they're raw concepts.
   */
  @CacheEvict(value = "roadmap", allEntries = true)
  public PitchDTO createIdea(String title, String description, Long epicId) {
    Pitch pitch = Pitch.builder()
        .title(title)
        .description(description)
        .status(PitchStatus.IDEA)
        .build();

    if (epicId != null) {
      Epic epic = epicRepository.findByIdNotDeleted(epicId)
          .orElseThrow(() -> new ResourceNotFoundException("Epic not found with id: " + epicId));
      pitch.setEpic(epic);
    }

    Pitch saved = pitchRepository.save(pitch);
    log.info("Created idea: {} (ID: {})", saved.getTitle(), saved.getId());

    // Publish event for knowledge ingestion
    eventPublisher.publishEvent(new KnowledgeEventListener.PitchKnowledgeEvent(saved.getId()));

    return toDTO(saved);
  }

  /**
   * Create a pitch with full flexibility - can be IDEA, DRAFT, SHAPED, or assigned to cycle.
   * Validates field requirements based on target status.
   */
  @CacheEvict(value = "roadmap", allEntries = true)
  public PitchDTO createPitch(CreatePitchRequest request) {
    // Determine final status - default to IDEA if not specified
    PitchStatus targetStatus = request.getStatus() != null ? request.getStatus() : PitchStatus.IDEA;

    // Validate based on target status
    validatePitchForStatus(request, targetStatus);

    // Build the pitch
    Pitch.PitchBuilder builder = Pitch.builder()
        .title(request.getTitle())
        .description(request.getDescription())
        .status(targetStatus)
        // Shape Up fields
        .problemStatement(request.getProblemStatement())
        .solution(request.getSolution())
        .rabbitHoles(request.getRabbitHoles())
        .risks(request.getRisks())
        .noGos(request.getNoGos())
        .wireframeLinks(request.getWireframeLinks());

    // Set appetite if provided (required for SHAPED+)
    if (request.getAppetiteDays() != null) {
      builder.appetiteDays(request.getAppetiteDays());
    }

    // Set cycle if provided (required for PENDING+)
    if (request.getCycleId() != null) {
      Cycle cycle = cycleRepository.findById(request.getCycleId())
          .orElseThrow(() -> new IllegalArgumentException("Cycle not found with id: " + request.getCycleId()));
      builder.cycle(cycle);
    }

    Pitch pitch = builder.build();

    if (request.getTeamId() != null) {
      Team team = teamRepository.findById(request.getTeamId())
          .orElseThrow(() -> new IllegalArgumentException("Team not found with id: " + request.getTeamId()));
      pitch.setTeam(team);
    }

    // Epic and Release linking
    if (request.getEpicId() != null) {
      Epic epic = epicRepository.findByIdNotDeleted(request.getEpicId())
          .orElseThrow(() -> new ResourceNotFoundException("Epic not found with id: " + request.getEpicId()));
      pitch.setEpic(epic);
    }
    if (request.getTargetReleaseId() != null) {
      Release release = releaseRepository.findByIdNotDeleted(request.getTargetReleaseId())
          .orElseThrow(() -> new ResourceNotFoundException("Release not found with id: " + request.getTargetReleaseId()));
      pitch.setTargetRelease(release);
    }

    // Priority and sort order
    if (request.getPriority() != null) {
      pitch.setPriority(request.getPriority());
    }
    if (request.getSortOrder() != null) {
      pitch.setSortOrder(request.getSortOrder());
    }

    Pitch saved = pitchRepository.save(pitch);

    // Publish event for knowledge ingestion
    eventPublisher.publishEvent(new KnowledgeEventListener.PitchKnowledgeEvent(saved.getId()));

    return toDTO(saved);
  }

  /**
   * Validates that a pitch has the required fields for a given status.
   */
  private void validatePitchForStatus(CreatePitchRequest request, PitchStatus targetStatus) {
    // IDEA: Only title required (already validated by @NotBlank)
    if (targetStatus == PitchStatus.IDEA) {
      return;
    }

    // DRAFT: Only title required
    if (targetStatus == PitchStatus.DRAFT) {
      return;
    }

    // SHAPED: Requires appetite
    if (targetStatus == PitchStatus.SHAPED) {
      if (request.getAppetiteDays() == null) {
        throw new IllegalArgumentException("Appetite days is required for SHAPED status");
      }
      return;
    }

    // PENDING and beyond: Requires cycle and appetite
    if (request.getCycleId() == null) {
      throw new IllegalArgumentException("Cycle ID is required for " + targetStatus + " status");
    }
    if (request.getAppetiteDays() == null) {
      throw new IllegalArgumentException("Appetite days is required for " + targetStatus + " status");
    }
  }

  // ===== Shape Up Workflow Transitions =====

  /**
   * Start shaping an idea - transitions IDEA → DRAFT.
   */
  @CacheEvict(value = "roadmap", allEntries = true)
  public PitchDTO startShaping(Long pitchId) {
    Pitch pitch = pitchRepository.findByIdNotDeleted(pitchId)
        .orElseThrow(() -> new ResourceNotFoundException("Pitch not found with id: " + pitchId));

    if (pitch.getStatus() != PitchStatus.IDEA) {
      throw new IllegalStateException("Can only start shaping from IDEA status. Current: " + pitch.getStatus());
    }

    pitch.setStatus(PitchStatus.DRAFT);
    Pitch saved = pitchRepository.save(pitch);
    log.info("Started shaping pitch: {} (IDEA → DRAFT)", saved.getTitle());

    eventPublisher.publishEvent(new KnowledgeEventListener.PitchKnowledgeEvent(saved.getId()));
    return toDTO(saved);
  }

  /**
   * Mark a draft as shaped and ready for betting - transitions DRAFT → SHAPED.
   * Requires: problemStatement OR solution, AND appetiteDays.
   */
  @CacheEvict(value = "roadmap", allEntries = true)
  public PitchDTO markAsShaped(Long pitchId) {
    Pitch pitch = pitchRepository.findByIdNotDeleted(pitchId)
        .orElseThrow(() -> new ResourceNotFoundException("Pitch not found with id: " + pitchId));

    if (pitch.getStatus() != PitchStatus.DRAFT && pitch.getStatus() != PitchStatus.IDEA) {
      throw new IllegalStateException("Can only mark as shaped from IDEA or DRAFT status. Current: " + pitch.getStatus());
    }

    // Validate shaping requirements
    if (pitch.getAppetiteDays() == null) {
      throw new IllegalArgumentException("Appetite days is required to mark as shaped");
    }
    boolean hasProblemOrSolution = (pitch.getProblemStatement() != null && !pitch.getProblemStatement().isBlank())
        || (pitch.getSolution() != null && !pitch.getSolution().isBlank());
    if (!hasProblemOrSolution) {
      throw new IllegalArgumentException("Problem statement or solution is required to mark as shaped");
    }

    pitch.setStatus(PitchStatus.SHAPED);
    // Shape Up workflow: Clear cycle/team assignment when marked as shaped
    // so it becomes available for betting in the next cycle
    pitch.setCycle(null);
    pitch.setTeam(null);
    
    Pitch saved = pitchRepository.save(pitch);
    log.info("Marked pitch as shaped: {} (→ SHAPED, ready for betting)", saved.getTitle());

    eventPublisher.publishEvent(new KnowledgeEventListener.PitchKnowledgeEvent(saved.getId()));
    return toDTO(saved);
  }

  /**
   * Assign a shaped pitch to a cycle (betting table decision) - transitions SHAPED → PENDING.
   */
  @CacheEvict(value = "roadmap", allEntries = true)
  public PitchDTO assignToCycle(Long pitchId, Long cycleId) {
    Pitch pitch = pitchRepository.findByIdNotDeleted(pitchId)
        .orElseThrow(() -> new ResourceNotFoundException("Pitch not found with id: " + pitchId));

    if (pitch.getStatus() != PitchStatus.SHAPED) {
      throw new IllegalStateException("Can only assign SHAPED pitches to cycle. Current: " + pitch.getStatus());
    }

    Cycle cycle = cycleRepository.findById(cycleId)
        .orElseThrow(() -> new ResourceNotFoundException("Cycle not found with id: " + cycleId));

    PitchStatus oldStatus = pitch.getStatus();
    pitch.setCycle(cycle);
    pitch.setStatus(PitchStatus.PENDING);
    Pitch saved = pitchRepository.save(pitch);

    log.info("Assigned pitch to cycle: {} → Cycle {} (SHAPED → PENDING)", saved.getTitle(), cycle.getName());

    // Invalidate cache and publish events
    invalidateCacheForPitch(saved);
    eventPublisher.publishEvent(new KnowledgeEventListener.PitchKnowledgeEvent(saved.getId()));
    eventPublisher.publishEvent(new PitchStatusChangedEvent(
        this, saved.getId(), cycle.getId(), saved.getTitle(), 
        oldStatus.name(), PitchStatus.PENDING.name()));

    return toDTO(saved);
  }

  /**
   * Unassign a pitch from its cycle (move back to betting candidates) - transitions PENDING → SHAPED.
   */
  @CacheEvict(value = "roadmap", allEntries = true)
  public PitchDTO unassignFromCycle(Long pitchId) {
    Pitch pitch = pitchRepository.findByIdNotDeleted(pitchId)
        .orElseThrow(() -> new ResourceNotFoundException("Pitch not found with id: " + pitchId));

    if (pitch.getStatus() != PitchStatus.PENDING) {
      throw new IllegalStateException("Can only unassign PENDING pitches. Current: " + pitch.getStatus());
    }

    Long oldCycleId = pitch.getCycle() != null ? pitch.getCycle().getId() : null;
    pitch.setCycle(null);
    pitch.setTeam(null); // Also clear team assignment
    pitch.setStatus(PitchStatus.SHAPED);
    Pitch saved = pitchRepository.save(pitch);

    log.info("Unassigned pitch from cycle: {} (PENDING → SHAPED)", saved.getTitle());

    // Invalidate cache
    if (oldCycleId != null) {
      cacheService.invalidateCycleRiskCache(oldCycleId);
    }
    eventPublisher.publishEvent(new KnowledgeEventListener.PitchKnowledgeEvent(saved.getId()));

    return toDTO(saved);
  }

  // ===== Pool Queries =====

  /** Get all ideas (raw concepts not yet being shaped). */
  public List<PitchDTO> getIdeas() {
    return pitchRepository.findAllIdeas().stream().map(this::toDTO).collect(Collectors.toList());
  }

  /** Get ideas for a specific project. */
  public List<PitchDTO> getIdeasByProjectId(Long projectId) {
    return pitchRepository.findIdeasByProjectId(projectId).stream().map(this::toDTO).collect(Collectors.toList());
  }

  /** Get ideas for a specific epic. */
  public List<PitchDTO> getIdeasByEpicId(Long epicId) {
    return pitchRepository.findIdeasByEpicId(epicId).stream().map(this::toDTO).collect(Collectors.toList());
  }

  /** Get all drafts (pitches being shaped). */
  public List<PitchDTO> getDrafts() {
    return pitchRepository.findAllDrafts().stream().map(this::toDTO).collect(Collectors.toList());
  }

  /** Get drafts for a specific project. */
  public List<PitchDTO> getDraftsByProjectId(Long projectId) {
    return pitchRepository.findDraftsByProjectId(projectId).stream().map(this::toDTO).collect(Collectors.toList());
  }

  /** Get all betting candidates (shaped pitches ready for cycle assignment). */
  public List<PitchDTO> getBettingCandidates() {
    return pitchRepository.findBettingCandidates().stream().map(this::toDTO).collect(Collectors.toList());
  }

  /** Get betting candidates for a specific project. */
  public List<PitchDTO> getBettingCandidatesByProjectId(Long projectId) {
    return pitchRepository.findBettingCandidatesByProjectId(projectId).stream().map(this::toDTO).collect(Collectors.toList());
  }

  /** Get all unassigned pitches (IDEA, DRAFT, SHAPED). */
  public List<PitchDTO> getUnassignedByEpicId(Long epicId) {
    return pitchRepository.findUnassignedByEpicId(epicId).stream().map(this::toDTO).collect(Collectors.toList());
  }

  /**
   * Batch-update sort order for pitches (used by drag-and-drop reordering).
   * Validates that no pitch dependency constraints are violated by the new order.
   */
  @CacheEvict(value = "roadmap", allEntries = true)
  public void reorder(ReorderRequest request) {
    List<Pitch> pitchesToSave = request.getItems().stream().map(item -> {
      Pitch pitch = pitchRepository.findByIdNotDeleted(item.getId())
          .orElseThrow(() -> new ResourceNotFoundException("Pitch not found with id: " + item.getId()));
      pitch.setSortOrder(item.getSortOrder());
      return pitch;
    }).collect(Collectors.toList());

    // Build proposed order map (pitchId -> sortOrder)
    Map<Long, Integer> proposedOrder = new HashMap<>();
    for (ReorderRequest.ReorderItem item : request.getItems()) {
      proposedOrder.put(item.getId(), item.getSortOrder());
    }

    // Validate dependency constraints (null-safe: repository may be absent in unit tests)
    if (pitchDependencyRepository != null) {
      for (ReorderRequest.ReorderItem item : request.getItems()) {
        List<PitchDependency> deps = pitchDependencyRepository
            .findBySourcePitchIdOrTargetPitchId(item.getId(), item.getId());
        for (PitchDependency dep : deps) {
          Long sourceId = dep.getSourcePitch().getId();
          Long targetId = dep.getTargetPitch().getId();
          Integer sourceOrder = proposedOrder.get(sourceId);
          Integer targetOrder = proposedOrder.get(targetId);
          if (sourceOrder == null || targetOrder == null) {
            continue;
          }
          com.github.farzadsedaghatbin.shipflow.entity.enums.DependencyType type = dep.getDependencyType();
          if (type == com.github.farzadsedaghatbin.shipflow.entity.enums.DependencyType.BLOCKS
              || type == com.github.farzadsedaghatbin.shipflow.entity.enums.DependencyType.DEPENDS_ON) {
            if (sourceOrder >= targetOrder) {
              String sourceTitle = dep.getSourcePitch().getTitle();
              String targetTitle = dep.getTargetPitch().getTitle();
              throw new BadRequestException(
                  "Cannot reorder: \"" + sourceTitle + "\" blocks \"" + targetTitle
                      + "\" — the blocking pitch must appear before the blocked pitch");
            }
          }
        }
      }
    }

    pitchRepository.saveAll(pitchesToSave);
    log.info("Reordered {} pitches", request.getItems().size());
  }

  @CacheEvict(value = "roadmap", allEntries = true)
  public PitchDTO updatePitch(Long id, CreatePitchRequest request) {
    Pitch pitch = pitchRepository.findByIdNotDeleted(id)
        .orElseThrow(() -> new IllegalArgumentException("Pitch not found with id: " + id));

    pitch.setTitle(request.getTitle());
    pitch.setDescription(request.getDescription());
    pitch.setAppetiteDays(request.getAppetiteDays());
    
    PitchStatus oldStatus = pitch.getStatus();
    pitch.setStatus(request.getStatus());
    
    // Shape Up workflow: When a pitch becomes SHAPED, clear cycle/team assignment
    // so it becomes available for betting in the next cycle
    if (request.getStatus() == PitchStatus.SHAPED && oldStatus != PitchStatus.SHAPED) {
      pitch.setCycle(null);
      pitch.setTeam(null);
    }

    // Shape Up fields
    pitch.setProblemStatement(request.getProblemStatement());
    pitch.setSolution(request.getSolution());
    pitch.setRabbitHoles(request.getRabbitHoles());
    pitch.setRisks(request.getRisks());
    pitch.setNoGos(request.getNoGos());
    pitch.setWireframeLinks(request.getWireframeLinks());

    if (request.getTeamId() != null) {
      Team team = teamRepository.findById(request.getTeamId())
          .orElseThrow(() -> new IllegalArgumentException("Team not found with id: " + request.getTeamId()));
      pitch.setTeam(team);
    }

    // Epic and Release linking (null clears the link)
    if (request.getEpicId() != null) {
      Epic epic = epicRepository.findByIdNotDeleted(request.getEpicId())
          .orElseThrow(() -> new ResourceNotFoundException("Epic not found with id: " + request.getEpicId()));
      pitch.setEpic(epic);
    } else {
      pitch.setEpic(null);
    }
    if (request.getTargetReleaseId() != null) {
      Release release = releaseRepository.findByIdNotDeleted(request.getTargetReleaseId())
          .orElseThrow(() -> new ResourceNotFoundException("Release not found with id: " + request.getTargetReleaseId()));
      pitch.setTargetRelease(release);
    } else {
      pitch.setTargetRelease(null);
    }

    // Priority and sort order
    pitch.setPriority(request.getPriority());
    if (request.getSortOrder() != null) {
      pitch.setSortOrder(request.getSortOrder());
    }

    Pitch saved = pitchRepository.save(pitch);

    // Publish event for knowledge ingestion
    eventPublisher.publishEvent(new KnowledgeEventListener.PitchKnowledgeEvent(saved.getId()));

    // Invalidate risk analysis cache since pitch data changed
    invalidateCacheForPitch(saved);

    return toDTO(saved);
  }

  @CacheEvict(value = "roadmap", allEntries = true)
  public PitchDTO updateStatus(Long id, PitchStatus status) {
    Pitch pitch = pitchRepository.findByIdNotDeleted(id)
        .orElseThrow(() -> new IllegalArgumentException("Pitch not found with id: " + id));

    PitchStatus oldStatus = pitch.getStatus();
    pitch.setStatus(status);
    
    // Shape Up workflow: When a pitch becomes SHAPED, clear cycle/team assignment
    // so it becomes available for betting in the next cycle
    if (status == PitchStatus.SHAPED && oldStatus != PitchStatus.SHAPED) {
      pitch.setCycle(null);
      pitch.setTeam(null);
    }
    
    Pitch saved = pitchRepository.save(pitch);

    // Invalidate risk analysis cache since status changed
    invalidateCacheForPitch(saved);

    // Publish event for narrative auto-regeneration if status changed significantly
    if (oldStatus != status && saved.getCycle() != null) {
      eventPublisher.publishEvent(new PitchStatusChangedEvent(
          this,
          saved.getId(),
          saved.getCycle().getId(),
          saved.getTitle(),
          oldStatus != null ? oldStatus.name() : null,
          status.name()
      ));
    }

    return toDTO(saved);
  }

  @CacheEvict(value = "roadmap", allEntries = true)
  public PitchDTO assignTeam(Long id, Long teamId) {
    Pitch pitch = pitchRepository.findByIdNotDeleted(id)
        .orElseThrow(() -> new IllegalArgumentException("Pitch not found with id: " + id));

    Team team = teamRepository.findById(teamId)
        .orElseThrow(() -> new IllegalArgumentException("Team not found with id: " + teamId));

    pitch.setTeam(team);
    Pitch saved = pitchRepository.save(pitch);

    // Invalidate risk analysis cache since team assignment changed
    invalidateCacheForPitch(saved);

    return toDTO(saved);
  }

  @CacheEvict(value = "roadmap", allEntries = true)
  public void deletePitch(Long id) {
    Pitch pitch = pitchRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Pitch not found with id: " + id));

    // Check if already deleted
    if (pitch.getDeletedAt() != null) {
      throw new IllegalStateException("Pitch is already deleted");
    }

    Long cycleId = pitch.getCycle() != null ? pitch.getCycle().getId() : null;
    User currentUser = getCurrentUser();

    // Perform soft delete
    pitch.setDeletedAt(LocalDateTime.now());
    pitch.setDeletedBy(currentUser);
    pitchRepository.save(pitch);

    // Invalidate cycle cache since pitch was removed
    if (cycleId != null) {
      cacheService.invalidateCycleRiskCache(cycleId);
    }
  }

  /** Invalidate cache for pitch and its cycle when pitch data changes. */
  private void invalidateCacheForPitch(Pitch pitch) {
    if (pitch != null) {
      cacheService.invalidatePitchRiskCache(pitch.getId());
      if (pitch.getCycle() != null) {
        cacheService.invalidateCycleRiskCache(pitch.getCycle().getId());
      }
    }
  }

  private PitchDTO toDTO(Pitch pitch) {
    // Load dependency data (null-safe: repositories may be absent in unit tests)
    List<PitchDependencyDTO> blockingPitches = (pitchDependencyRepository != null && pitchDependencyService != null)
        ? pitchDependencyRepository.findBySourcePitchId(pitch.getId())
            .stream().map(pitchDependencyService::toDTO).collect(Collectors.toList())
        : java.util.Collections.emptyList();
    List<PitchDependencyDTO> blockedByPitches = (pitchDependencyRepository != null && pitchDependencyService != null)
        ? pitchDependencyRepository.findByTargetPitchId(pitch.getId())
            .stream().map(pitchDependencyService::toDTO).collect(Collectors.toList())
        : java.util.Collections.emptyList();

    Double totalHours = workLogRepository.getTotalHoursByPitchId(pitch.getId());
    if (totalHours == null)
      totalHours = 0.0;

    double appetiteHours = capacityConfigService.calculatePitchAppetiteHours(pitch);
    double progress = appetiteHours > 0 ? (totalHours / appetiteHours) * 100 : 0;

    // Calculate team capacity and busiest person
    Integer teamMemberCount = null;
    Double totalBudgetPersonDays = null;
    Double budgetUtilizationPercent = null;
    PitchDTO.BusiestPersonDTO busiestPerson = null;
    
    if (pitch.getTeam() != null && pitch.getAppetiteDays() != null) {
      var team = pitch.getTeam();
      var teamBudget = capacityConfigService.calculateTeamBudget(team, pitch.getAppetiteDays());
      
      // Get hours spent per person from work logs
      Map<Long, Double> personHoursMap = workLogRepository.findByPitchId(pitch.getId()).stream()
          .filter(wl -> wl.getPerson() != null)
          .collect(java.util.stream.Collectors.groupingBy(
              wl -> wl.getPerson().getId(),
              java.util.stream.Collectors.summingDouble(wl -> wl.getHoursSpent() != null ? wl.getHoursSpent().doubleValue() : 0.0)
          ));
      
      // Calculate average hours per day for person-days conversion
      double avgHoursPerDay = teamBudget.getMemberBudgets().isEmpty() ? 
          capacityConfigService.getOrganizationDefaultHoursPerDay() :
          teamBudget.getTotalDailyCapacityHours() / teamBudget.getMemberCount();
      
      // Find busiest member (highest utilization)
      var busiestMember = teamBudget.getMemberBudgets().stream()
          .map(pb -> {
            double hoursSpent = personHoursMap.getOrDefault(pb.getPersonId(), 0.0);
            double utilization = pb.getTotalBudgetHours() > 0 ? 
                (hoursSpent / pb.getTotalBudgetHours()) * 100 : 0;
            return new Object() {
              final Long personId = pb.getPersonId();
              final String personName = pb.getPersonName();
              final String role = pb.getRole() != null ? pb.getRole().name() : null;
              final Double hoursPerDay = pb.getHoursPerDay();
              final String capacitySource = pb.getCapacitySource();
              final Double totalBudgetHours = pb.getTotalBudgetHours();
              final Double spent = hoursSpent;
              final Double util = Math.round(utilization * 10.0) / 10.0;
              final Boolean overBudget = hoursSpent > pb.getTotalBudgetHours();
            };
          })
          .max((a, b) -> Double.compare(a.util, b.util))
          .orElse(null);
      
      if (busiestMember != null) {
        busiestPerson = PitchDTO.BusiestPersonDTO.builder()
            .personId(busiestMember.personId)
            .personName(busiestMember.personName)
            .role(busiestMember.role)
            .hoursPerDay(busiestMember.hoursPerDay)
            .capacitySource(busiestMember.capacitySource)
            .totalBudgetHours(busiestMember.totalBudgetHours)
            .hoursSpent(busiestMember.spent)
            .utilizationPercent(busiestMember.util)
            .isOverBudget(busiestMember.overBudget)
            .build();
      }
      
      // Calculate budget metrics
      teamMemberCount = teamBudget.getMemberCount();
      totalBudgetPersonDays = avgHoursPerDay > 0 ? 
          Math.round((teamBudget.getTotalBudgetHours() / avgHoursPerDay) * 10.0) / 10.0 : 0;
      double totalPersonDaysSpent = avgHoursPerDay > 0 ? totalHours / avgHoursPerDay : 0;
      budgetUtilizationPercent = totalBudgetPersonDays > 0 ? 
          Math.round((totalPersonDaysSpent / totalBudgetPersonDays) * 1000.0) / 10.0 : 0;
    }

    // Handle null cycle for pre-cycle pitches (IDEA, DRAFT, SHAPED)
    Long cycleId = pitch.getCycle() != null ? pitch.getCycle().getId() : null;
    String cycleName = pitch.getCycle() != null ? pitch.getCycle().getName() : null;
    Long projectId = null;
    String projectName = null;
    String projectKey = null;
    
    if (pitch.getCycle() != null && pitch.getCycle().getProject() != null) {
      projectId = pitch.getCycle().getProject().getId();
      projectName = pitch.getCycle().getProject().getName();
      projectKey = pitch.getCycle().getProject().getProjectKey();
    }

    return PitchDTO.builder().id(pitch.getId()).title(pitch.getTitle()).description(pitch.getDescription())
        .appetiteDays(pitch.getAppetiteDays()).cycleId(cycleId)
        .cycleName(cycleName)
        .projectId(projectId)
        .projectName(projectName)
        .projectKey(projectKey)
        .teamId(pitch.getTeam() != null ? pitch.getTeam().getId() : null)
        .teamName(pitch.getTeam() != null ? pitch.getTeam().getName() : null).status(pitch.getStatus())
        .createdAt(pitch.getCreatedAt()).updatedAt(pitch.getUpdatedAt()).totalHoursSpent(totalHours)
        .appetiteHours(appetiteHours).progressPercentage(Math.min(progress, 100))
        // Team capacity and budget
        .teamMemberCount(teamMemberCount)
        .totalBudgetPersonDays(totalBudgetPersonDays)
        .budgetUtilizationPercent(budgetUtilizationPercent)
        .busiestPerson(busiestPerson)
        // Shape Up fields
        .problemStatement(pitch.getProblemStatement()).solution(pitch.getSolution())
        .rabbitHoles(pitch.getRabbitHoles()).risks(pitch.getRisks()).noGos(pitch.getNoGos())
        .wireframeLinks(pitch.getWireframeLinks())
        // Epic and release fields (roadmap)
        .epicId(pitch.getEpic() != null ? pitch.getEpic().getId() : null)
        .epicName(pitch.getEpic() != null ? pitch.getEpic().getName() : null)
        .targetReleaseId(pitch.getTargetRelease() != null ? pitch.getTargetRelease().getId() : null)
        .targetReleaseName(pitch.getTargetRelease() != null ? pitch.getTargetRelease().getName() : null)
        .targetReleaseVersion(pitch.getTargetRelease() != null ? pitch.getTargetRelease().getVersion() : null)
        // Priority and ordering
        .priority(pitch.getPriority())
        .sortOrder(pitch.getSortOrder())
        // Circuit breaker fields
        .isCircuitBreakerTriggered(pitch.getIsCircuitBreakerTriggered())
        .circuitBreakerReason(pitch.getCircuitBreakerReason()).circuitBreakerDate(pitch.getCircuitBreakerDate())
        // Roadmap dependencies
        .blockingPitches(blockingPitches)
        .blockedByPitches(blockedByPitches)
        .build();
  }
}
