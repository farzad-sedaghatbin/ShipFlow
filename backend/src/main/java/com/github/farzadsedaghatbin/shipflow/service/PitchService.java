package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.CreatePitchRequest;
import com.github.farzadsedaghatbin.shipflow.dto.PitchDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.Team;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.UserRole;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PitchStatus;
import com.github.farzadsedaghatbin.shipflow.event.PitchStatusChangedEvent;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TeamRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import com.github.farzadsedaghatbin.shipflow.repository.WorkLogRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
  private final ApplicationEventPublisher eventPublisher;
  private final AICacheService cacheService;
  private final CapacityConfigService capacityConfigService;

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
    return pitchRepository.findByCycleIdNotDeleted(cycleId).stream().map(this::toDTO).collect(Collectors.toList());
  }

  public List<PitchDTO> getPitchesByTeamId(Long teamId) {
    return pitchRepository.findByTeamIdNotDeleted(teamId).stream().map(this::toDTO).collect(Collectors.toList());
  }

  public PitchDTO getPitchById(Long id) {
    Pitch pitch = pitchRepository.findByIdNotDeleted(id)
        .orElseThrow(() -> new IllegalArgumentException("Pitch not found with id: " + id));
    return toDTO(pitch);
  }

  public PitchDTO createPitch(CreatePitchRequest request) {
    Cycle cycle = cycleRepository.findById(request.getCycleId())
        .orElseThrow(() -> new IllegalArgumentException("Cycle not found with id: " + request.getCycleId()));

    Pitch pitch = Pitch.builder().title(request.getTitle()).description(request.getDescription())
        .appetiteDays(request.getAppetiteDays()).cycle(cycle)
        .status(request.getStatus() != null ? request.getStatus() : PitchStatus.PENDING)
        // Shape Up fields
        .problemStatement(request.getProblemStatement()).solution(request.getSolution())
        .rabbitHoles(request.getRabbitHoles()).risks(request.getRisks()).noGos(request.getNoGos())
        .wireframeLinks(request.getWireframeLinks()).build();

    if (request.getTeamId() != null) {
      Team team = teamRepository.findById(request.getTeamId())
          .orElseThrow(() -> new IllegalArgumentException("Team not found with id: " + request.getTeamId()));
      pitch.setTeam(team);
    }

    Pitch saved = pitchRepository.save(pitch);

    // Publish event for knowledge ingestion
    eventPublisher.publishEvent(new KnowledgeEventListener.PitchKnowledgeEvent(saved.getId()));

    return toDTO(saved);
  }

  public PitchDTO updatePitch(Long id, CreatePitchRequest request) {
    Pitch pitch = pitchRepository.findByIdNotDeleted(id)
        .orElseThrow(() -> new IllegalArgumentException("Pitch not found with id: " + id));

    pitch.setTitle(request.getTitle());
    pitch.setDescription(request.getDescription());
    pitch.setAppetiteDays(request.getAppetiteDays());
    pitch.setStatus(request.getStatus());

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

    Pitch saved = pitchRepository.save(pitch);

    // Publish event for knowledge ingestion
    eventPublisher.publishEvent(new KnowledgeEventListener.PitchKnowledgeEvent(saved.getId()));

    // Invalidate risk analysis cache since pitch data changed
    invalidateCacheForPitch(saved);

    return toDTO(saved);
  }

  public PitchDTO updateStatus(Long id, PitchStatus status) {
    Pitch pitch = pitchRepository.findByIdNotDeleted(id)
        .orElseThrow(() -> new IllegalArgumentException("Pitch not found with id: " + id));

    PitchStatus oldStatus = pitch.getStatus();
    pitch.setStatus(status);
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

    return PitchDTO.builder().id(pitch.getId()).title(pitch.getTitle()).description(pitch.getDescription())
        .appetiteDays(pitch.getAppetiteDays()).cycleId(pitch.getCycle().getId())
        .cycleName(pitch.getCycle().getName())
        .projectId(pitch.getCycle().getProject() != null ? pitch.getCycle().getProject().getId() : null)
        .projectName(pitch.getCycle().getProject() != null ? pitch.getCycle().getProject().getName() : null)
        .projectKey(
            pitch.getCycle().getProject() != null ? pitch.getCycle().getProject().getProjectKey() : null)
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
        // Circuit breaker fields
        .isCircuitBreakerTriggered(pitch.getIsCircuitBreakerTriggered())
        .circuitBreakerReason(pitch.getCircuitBreakerReason()).circuitBreakerDate(pitch.getCircuitBreakerDate())
        .build();
  }
}
