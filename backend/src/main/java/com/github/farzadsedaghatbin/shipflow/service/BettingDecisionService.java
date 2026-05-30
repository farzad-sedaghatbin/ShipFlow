package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.betting.*;
import com.github.farzadsedaghatbin.shipflow.entity.*;
import com.github.farzadsedaghatbin.shipflow.entity.enums.BettingDecisionType;
import com.github.farzadsedaghatbin.shipflow.repository.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing betting decisions - tracking commit/reject decisions
 * with reasoning for Shape Up methodology.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class BettingDecisionService {

  private final BettingDecisionRepository bettingDecisionRepository;
  private final PitchRepository pitchRepository;
  private final CycleRepository cycleRepository;
  private final TeamRepository teamRepository;
  private final UserRepository userRepository;
  private final BettingSlotRepository bettingSlotRepository;
  private final MessageService messageService;

  /**
   * Record a new betting decision for a pitch in a cycle.
   * If a decision already exists for this pitch+cycle, it will be updated instead of creating a duplicate.
   * Uses database unique constraint (pitch_id, cycle_id) to ensure data integrity.
   *
   * @param request the decision details
   * @param decidedByUserId the user making the decision
   * @return the recorded decision
   */
  @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
  public BettingDecisionDTO recordDecision(RecordBettingDecisionRequest request, Long decidedByUserId) {
    log.info("Recording betting decision for pitch {} in cycle {} by user {}",
        request.getPitchId(), request.getCycleId(), decidedByUserId);

    // Validate entities exist
    Pitch pitch = pitchRepository.findById(request.getPitchId())
        .orElseThrow(() -> new IllegalArgumentException(
            messageService.getMessage("error.pitch.not.found", request.getPitchId())));

    // The requested_appetite_days column is NOT NULL and is copied from the pitch's appetite.
    // Reject the decision with a clear message rather than letting the insert fail with a
    // database constraint violation (HTTP 500).
    if (pitch.getAppetiteDays() == null) {
      throw new IllegalArgumentException(
          messageService.getMessage("error.betting.pitch.no.appetite", pitch.getTitle()));
    }

    Cycle cycle = cycleRepository.findById(request.getCycleId())
        .orElseThrow(() -> new IllegalArgumentException(
            messageService.getMessage("error.cycle.not.found", request.getCycleId())));

    User decidedBy = userRepository.findById(decidedByUserId)
        .orElseThrow(() -> new IllegalArgumentException(
            messageService.getMessage("error.user.not.found", decidedByUserId)));

    Team consideredTeam = null;
    if (request.getConsideredTeamId() != null) {
      consideredTeam = teamRepository.findById(request.getConsideredTeamId())
          .orElseThrow(() -> new IllegalArgumentException(
              messageService.getMessage("error.team.not.found", request.getConsideredTeamId())));
    }

    // Check if a decision already exists for this pitch+cycle combination
    // Using the optimized method that expects at most one result due to unique constraint
    Optional<BettingDecision> existingDecision = bettingDecisionRepository
        .findByPitchIdAndCycleId(request.getPitchId(), request.getCycleId());
    
    BettingDecision decision;
    if (existingDecision.isPresent()) {
      // Update the existing decision instead of creating a new one
      decision = existingDecision.get();
      log.info("Updating existing betting decision {} for pitch {}", decision.getId(), pitch.getTitle());
      
      decision.setDecision(request.getDecision());
      decision.setReason(request.getReason());
      decision.setDecidedBy(decidedBy);
      decision.setDecidedAt(LocalDateTime.now());
      decision.setRequestedAppetiteDays(pitch.getAppetiteDays());
      decision.setAvailableCapacityDays(request.getAvailableCapacityDays());
      decision.setAppetiteComparisonNotes(request.getAppetiteComparisonNotes());
      decision.setConsideredTeam(consideredTeam);
      decision.setPriorityScore(request.getPriorityScore());
      decision.setStrategicAlignmentNotes(request.getStrategicAlignmentNotes());
    } else {
      // Build a new decision entity
      decision = BettingDecision.builder()
          .pitch(pitch)
          .cycle(cycle)
          .decision(request.getDecision())
          .reason(request.getReason())
          .decidedBy(decidedBy)
          .decidedAt(LocalDateTime.now())
          .requestedAppetiteDays(pitch.getAppetiteDays())
          .availableCapacityDays(request.getAvailableCapacityDays())
          .appetiteComparisonNotes(request.getAppetiteComparisonNotes())
          .consideredTeam(consideredTeam)
          .priorityScore(request.getPriorityScore())
          .strategicAlignmentNotes(request.getStrategicAlignmentNotes())
          .build();
    }

    BettingDecision saved = bettingDecisionRepository.save(decision);
    log.info("Recorded betting decision {} for pitch {}: {}",
        saved.getId(), pitch.getTitle(), request.getDecision());

    return toDTO(saved);
  }

  /**
   * Get the decision history for a specific pitch across all cycles.
   */
  @Transactional(readOnly = true)
  public List<BettingDecisionDTO> getPitchDecisionHistory(Long pitchId) {
    return bettingDecisionRepository.findByPitchIdOrderByDecidedAtDesc(pitchId)
        .stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
  }

  /**
   * Get all decisions for a specific cycle.
   */
  @Transactional(readOnly = true)
  public List<BettingDecisionDTO> getCycleDecisions(Long cycleId) {
    return bettingDecisionRepository.findByCycleIdOrderByDecidedAtDesc(cycleId)
        .stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
  }

  /**
   * Get comprehensive decision history and statistics for a cycle.
   */
  @Transactional(readOnly = true)
  public BettingDecisionHistoryDTO getCycleDecisionHistory(Long cycleId) {
    Cycle cycle = cycleRepository.findById(cycleId)
        .orElseThrow(() -> new IllegalArgumentException(
            messageService.getMessage("error.cycle.not.found", cycleId)));

    List<BettingDecision> decisions = bettingDecisionRepository.findByCycleIdOrderByDecidedAtDesc(cycleId);
    List<BettingDecisionDTO> decisionDTOs = decisions.stream().map(this::toDTO).collect(Collectors.toList());

    // Calculate counts by type
    Map<BettingDecisionType, Long> decisionCounts = decisions.stream()
        .collect(Collectors.groupingBy(BettingDecision::getDecision, Collectors.counting()));

    // Get repeatedly rejected pitches
    List<Object[]> repeatedlyRejected = bettingDecisionRepository.findRepeatedlyRejectedPitches(2);
    List<BettingDecisionHistoryDTO.RepeatedlyRejectedPitchDTO> rejectedPitches = repeatedlyRejected.stream()
        .map(row -> {
          Long pitchId = (Long) row[0];
          Long count = (Long) row[1];
          Pitch pitch = pitchRepository.findById(pitchId).orElse(null);
          List<String> reasons = bettingDecisionRepository
              .findByCycleIdAndDecisionOrderByDecidedAtDesc(cycleId, BettingDecisionType.REJECTED)
              .stream()
              .filter(d -> d.getPitch().getId().equals(pitchId))
              .map(BettingDecision::getReason)
              .collect(Collectors.toList());

          return BettingDecisionHistoryDTO.RepeatedlyRejectedPitchDTO.builder()
              .pitchId(pitchId)
              .pitchTitle(pitch != null ? pitch.getTitle() : "Unknown")
              .rejectionCount(count.intValue())
              .rejectionReasons(reasons)
              .build();
        })
        .collect(Collectors.toList());

    // Calculate appetite statistics
    List<BettingDecision> decisionsWithCapacity = decisions.stream()
        .filter(d -> d.getAvailableCapacityDays() != null)
        .collect(Collectors.toList());

    int overCapacityCount = (int) decisionsWithCapacity.stream()
        .filter(d -> !d.appetiteFitsCapacity())
        .count();

    double avgAppetite = decisions.stream()
        .mapToInt(BettingDecision::getRequestedAppetiteDays)
        .average()
        .orElse(0);

    double avgCapacity = decisionsWithCapacity.stream()
        .mapToInt(BettingDecision::getAvailableCapacityDays)
        .average()
        .orElse(0);

    return BettingDecisionHistoryDTO.builder()
        .cycleId(cycle.getId())
        .cycleName(cycle.getName())
        .decisions(decisionDTOs)
        .decisionCounts(decisionCounts)
        .totalDecisions(decisions.size())
        .committedCount(decisionCounts.getOrDefault(BettingDecisionType.COMMITTED, 0L).intValue())
        .rejectedCount(decisionCounts.getOrDefault(BettingDecisionType.REJECTED, 0L).intValue())
        .deferredCount(decisionCounts.getOrDefault(BettingDecisionType.DEFERRED, 0L).intValue())
        .needsShapingCount(decisionCounts.getOrDefault(BettingDecisionType.NEEDS_SHAPING, 0L).intValue())
        .repeatedlyRejectedPitches(rejectedPitches)
        .decisionsOverCapacity(overCapacityCount)
        .averageAppetiteDays(avgAppetite)
        .averageAvailableCapacityDays(avgCapacity)
        .build();
  }

  /**
   * Get the most recent decision for a pitch.
   */
  @Transactional(readOnly = true)
  public Optional<BettingDecisionDTO> getMostRecentDecision(Long pitchId) {
    return bettingDecisionRepository.findFirstByPitchIdOrderByDecidedAtDesc(pitchId)
        .map(this::toDTO);
  }

  /**
   * Compare appetite vs capacity for a pitch against a specific team.
   */
  @Transactional(readOnly = true)
  public AppetiteComparisonDTO compareAppetite(Long pitchId, Long teamId, Long cycleId) {
    Pitch pitch = pitchRepository.findById(pitchId)
        .orElseThrow(() -> new IllegalArgumentException(
            messageService.getMessage("error.pitch.not.found", pitchId)));

    Team team = teamRepository.findById(teamId)
        .orElseThrow(() -> new IllegalArgumentException(
            messageService.getMessage("error.team.not.found", teamId)));

    Cycle cycle = cycleRepository.findById(cycleId)
        .orElseThrow(() -> new IllegalArgumentException(
            messageService.getMessage("error.cycle.not.found", cycleId)));

    // Calculate available capacity for the team
    int availableCapacityDays = calculateTeamAvailableCapacity(team, cycle);
    int requestedAppetite = pitch.getAppetiteDays();

    int difference = availableCapacityDays - requestedAppetite;
    double utilization = availableCapacityDays > 0
        ? (requestedAppetite * 100.0 / availableCapacityDays)
        : 100.0;

    AppetiteComparisonDTO.FitAssessment assessment =
        AppetiteComparisonDTO.calculateAssessment(utilization);
    String recommendation = AppetiteComparisonDTO.generateRecommendation(
        assessment, requestedAppetite, availableCapacityDays);

    return AppetiteComparisonDTO.builder()
        .pitchId(pitch.getId())
        .pitchTitle(pitch.getTitle())
        .requestedAppetiteDays(requestedAppetite)
        .teamId(team.getId())
        .teamName(team.getName())
        .availableCapacityDays(availableCapacityDays)
        .fits(requestedAppetite <= availableCapacityDays)
        .capacityDifferenceDays(difference)
        .utilizationPercentage(utilization)
        .assessment(assessment)
        .recommendation(recommendation)
        .build();
  }

  /**
   * Bulk compare a pitch against all teams in a cycle.
   */
  @Transactional(readOnly = true)
  public List<AppetiteComparisonDTO> compareAppetiteAllTeams(Long pitchId, Long cycleId) {
    List<Team> teams = teamRepository.findByCycleId(cycleId);
    return teams.stream()
        .map(team -> compareAppetite(pitchId, team.getId(), cycleId))
        .sorted(Comparator.comparing(AppetiteComparisonDTO::getUtilizationPercentage))
        .collect(Collectors.toList());
  }

  /**
   * Check if a decision has already been recorded for a pitch in a cycle.
   */
  @Transactional(readOnly = true)
  public boolean hasDecisionForPitchInCycle(Long pitchId, Long cycleId) {
    return bettingDecisionRepository.existsByPitchIdAndCycleId(pitchId, cycleId);
  }

  /**
   * Get decisions for a specific project (paginated).
   */
  @Transactional(readOnly = true)
  public Page<BettingDecisionDTO> getProjectDecisions(Long projectId, Pageable pageable) {
    return bettingDecisionRepository.findByProjectId(projectId, pageable)
        .map(this::toDTO);
  }

  /**
   * Get all committed decisions for a cycle.
   */
  @Transactional(readOnly = true)
  public List<BettingDecisionDTO> getCommittedDecisions(Long cycleId) {
    return bettingDecisionRepository.findCommittedByCycleId(cycleId)
        .stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
  }

  /**
   * Get all rejected decisions for a cycle.
   */
  @Transactional(readOnly = true)
  public List<BettingDecisionDTO> getRejectedDecisions(Long cycleId) {
    return bettingDecisionRepository.findRejectedByCycleId(cycleId)
        .stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
  }

  // ==================== Private Helper Methods ====================

  /**
   * Calculate available capacity for a team in a cycle.
   * This considers existing slot assignments and team composition.
   */
  private int calculateTeamAvailableCapacity(Team team, Cycle cycle) {
    // Get cycle duration in days
    long cycleDays = ChronoUnit.DAYS.between(cycle.getStartDate(), cycle.getEndDate());

    // Get already assigned slots for this team
    List<BettingSlot> assignedSlots = bettingSlotRepository.findByCycleIdAndTeamId(cycle.getId(), team.getId())
        .stream()
        .filter(slot -> slot.getPitch() != null)
        .collect(Collectors.toList());

    // Calculate used capacity from assigned pitches
    int usedCapacity = assignedSlots.stream()
        .mapToInt(slot -> slot.getPitch().getAppetiteDays())
        .sum();

    // Total capacity is cycle days, available is total minus used
    return Math.max(0, (int) cycleDays - usedCapacity);
  }

  /**
   * Convert entity to DTO.
   */
  private BettingDecisionDTO toDTO(BettingDecision decision) {
    return BettingDecisionDTO.builder()
        .id(decision.getId())
        .pitchId(decision.getPitch().getId())
        .pitchTitle(decision.getPitch().getTitle())
        .cycleId(decision.getCycle().getId())
        .cycleName(decision.getCycle().getName())
        .decision(decision.getDecision())
        .reason(decision.getReason())
        .decidedById(decision.getDecidedBy().getId())
        .decidedByUsername(decision.getDecidedBy().getUsername())
        .decidedAt(decision.getDecidedAt())
        .requestedAppetiteDays(decision.getRequestedAppetiteDays())
        .availableCapacityDays(decision.getAvailableCapacityDays())
        .appetiteComparisonNotes(decision.getAppetiteComparisonNotes())
        .appetiteFitsCapacity(decision.appetiteFitsCapacity())
        .consideredTeamId(decision.getConsideredTeam() != null ? decision.getConsideredTeam().getId() : null)
        .consideredTeamName(decision.getConsideredTeam() != null ? decision.getConsideredTeam().getName() : null)
        .priorityScore(decision.getPriorityScore())
        .strategicAlignmentNotes(decision.getStrategicAlignmentNotes())
        .createdAt(decision.getCreatedAt())
        .updatedAt(decision.getUpdatedAt())
        .build();
  }
}
