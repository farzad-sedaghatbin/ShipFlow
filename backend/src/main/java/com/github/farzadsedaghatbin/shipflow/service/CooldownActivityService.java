package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.cooldown.*;
import com.github.farzadsedaghatbin.shipflow.entity.CooldownActivity;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CooldownActivityStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CooldownActivityType;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import com.github.farzadsedaghatbin.shipflow.repository.CooldownActivityRepository;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CooldownActivityService {

  private final CooldownActivityRepository cooldownActivityRepository;
  private final CycleRepository cycleRepository;
  private final UserRepository userRepository;
  private final PitchRepository pitchRepository;
  private final MessageService messageService;

  /**
   * Create a new cooldown activity.
   */
  public CooldownActivityDTO createActivity(CreateCooldownActivityRequest request, Long createdByUserId) {
    Cycle cycle = cycleRepository.findById(request.getCycleId())
        .orElseThrow(() -> new ResourceNotFoundException(
            messageService.getMessage("error.cycle.not.found", request.getCycleId())));

    User createdBy = userRepository.findById(createdByUserId)
        .orElseThrow(() -> new ResourceNotFoundException(
            messageService.getMessage("error.user.not.found", createdByUserId)));

    CooldownActivity activity = CooldownActivity.builder()
        .cycle(cycle)
        .activityType(request.getActivityType())
        .title(request.getTitle())
        .description(request.getDescription())
        .createdBy(createdBy)
        .estimatedHours(request.getEstimatedHours())
        .priority(request.getPriority() != null ? request.getPriority() : 3)
        .notes(request.getNotes())
        .build();

    // Set optional assignee
    if (request.getAssigneeId() != null) {
      User assignee = userRepository.findById(request.getAssigneeId())
          .orElseThrow(() -> new ResourceNotFoundException(
              messageService.getMessage("error.user.not.found", request.getAssigneeId())));
      activity.setAssignee(assignee);
    }

    // Set optional related pitch
    if (request.getRelatedPitchId() != null) {
      Pitch relatedPitch = pitchRepository.findById(request.getRelatedPitchId())
          .orElseThrow(() -> new ResourceNotFoundException(
              messageService.getMessage("error.pitch.not.found", request.getRelatedPitchId())));
      activity.setRelatedPitch(relatedPitch);
    }

    CooldownActivity saved = cooldownActivityRepository.save(activity);
    log.info("Created cooldown activity {} in cycle {}: {} ({})", 
        saved.getId(), cycle.getId(), saved.getTitle(), saved.getActivityType());

    return CooldownActivityDTO.fromEntity(saved);
  }

  /**
   * Get an activity by ID.
   */
  public CooldownActivityDTO getActivity(Long id) {
    CooldownActivity activity = cooldownActivityRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Cooldown activity not found: " + id));
    return CooldownActivityDTO.fromEntity(activity);
  }

  /**
   * Get all activities for a cycle.
   */
  public List<CooldownActivityDTO> getActivitiesByCycle(Long cycleId) {
    return cooldownActivityRepository.findByCycleIdOrderByPriorityAscCreatedAtDesc(cycleId)
        .stream()
        .map(CooldownActivityDTO::fromEntity)
        .collect(Collectors.toList());
  }

  /**
   * Get activities by cycle and status.
   */
  public List<CooldownActivityDTO> getActivitiesByStatus(Long cycleId, CooldownActivityStatus status) {
    return cooldownActivityRepository.findByCycleIdAndStatusOrderByPriorityAsc(cycleId, status)
        .stream()
        .map(CooldownActivityDTO::fromEntity)
        .collect(Collectors.toList());
  }

  /**
   * Get activities by cycle and type.
   */
  public List<CooldownActivityDTO> getActivitiesByType(Long cycleId, CooldownActivityType type) {
    return cooldownActivityRepository.findByCycleIdAndActivityTypeOrderByPriorityAsc(cycleId, type)
        .stream()
        .map(CooldownActivityDTO::fromEntity)
        .collect(Collectors.toList());
  }

  /**
   * Get activities assigned to a user.
   */
  public List<CooldownActivityDTO> getActivitiesByAssignee(Long userId) {
    return cooldownActivityRepository.findByAssigneeIdOrderByPriorityAscCreatedAtDesc(userId)
        .stream()
        .map(CooldownActivityDTO::fromEntity)
        .collect(Collectors.toList());
  }

  /**
   * Get comprehensive summary for a cycle's cooldown activities.
   */
  public CooldownSummaryDTO getCycleSummary(Long cycleId) {
    Cycle cycle = cycleRepository.findById(cycleId)
        .orElseThrow(() -> new ResourceNotFoundException("Cycle not found: " + cycleId));

    List<CooldownActivity> activities = 
        cooldownActivityRepository.findByCycleIdOrderByPriorityAscCreatedAtDesc(cycleId);

    // Build count by type
    Map<CooldownActivityType, Long> countByType = new EnumMap<>(CooldownActivityType.class);
    for (Object[] row : cooldownActivityRepository.countActivitiesByTypeForCycle(cycleId)) {
      countByType.put((CooldownActivityType) row[0], (Long) row[1]);
    }

    // Build count by status
    Map<CooldownActivityStatus, Long> countByStatus = new EnumMap<>(CooldownActivityStatus.class);
    for (Object[] row : cooldownActivityRepository.countActivitiesByStatusForCycle(cycleId)) {
      countByStatus.put((CooldownActivityStatus) row[0], (Long) row[1]);
    }

    int completed = countByStatus.getOrDefault(CooldownActivityStatus.COMPLETED, 0L).intValue();
    int inProgress = countByStatus.getOrDefault(CooldownActivityStatus.IN_PROGRESS, 0L).intValue();
    int planned = countByStatus.getOrDefault(CooldownActivityStatus.PLANNED, 0L).intValue();
    int blocked = countByStatus.getOrDefault(CooldownActivityStatus.BLOCKED, 0L).intValue();
    int skipped = countByStatus.getOrDefault(CooldownActivityStatus.SKIPPED, 0L).intValue();

    return CooldownSummaryDTO.builder()
        .cycleId(cycleId)
        .cycleName(cycle.getName())
        .activities(activities.stream().map(CooldownActivityDTO::fromEntity).collect(Collectors.toList()))
        .countByType(countByType)
        .countByStatus(countByStatus)
        .totalActivities(activities.size())
        .completedCount(completed)
        .inProgressCount(inProgress)
        .plannedCount(planned)
        .blockedCount(blocked)
        .skippedCount(skipped)
        .totalEstimatedHours(cooldownActivityRepository.sumEstimatedHoursByCycleId(cycleId))
        .totalActualHours(cooldownActivityRepository.sumActualHoursForCompletedByCycleId(cycleId))
        .completionPercentage(CooldownSummaryDTO.calculateCompletionPercentage(completed, activities.size()))
        .build();
  }

  /**
   * Update activity status.
   */
  public CooldownActivityDTO updateStatus(Long id, CooldownActivityStatus newStatus, Integer actualHours) {
    CooldownActivity activity = cooldownActivityRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Cooldown activity not found: " + id));

    CooldownActivityStatus oldStatus = activity.getStatus();
    activity.setStatus(newStatus);

    // Set started time if moving to in-progress
    if (newStatus == CooldownActivityStatus.IN_PROGRESS && activity.getStartedAt() == null) {
      activity.setStartedAt(LocalDateTime.now());
    }

    // Set completed time and actual hours if completing
    if (newStatus == CooldownActivityStatus.COMPLETED) {
      activity.complete(actualHours);
    }

    CooldownActivity saved = cooldownActivityRepository.save(activity);
    log.info("Updated cooldown activity {} status: {} → {}", id, oldStatus, newStatus);

    return CooldownActivityDTO.fromEntity(saved);
  }

  /**
   * Update activity details.
   */
  public CooldownActivityDTO updateActivity(Long id, CreateCooldownActivityRequest request) {
    CooldownActivity activity = cooldownActivityRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Cooldown activity not found: " + id));

    activity.setActivityType(request.getActivityType());
    activity.setTitle(request.getTitle());
    activity.setDescription(request.getDescription());
    activity.setEstimatedHours(request.getEstimatedHours());
    activity.setPriority(request.getPriority() != null ? request.getPriority() : activity.getPriority());
    activity.setNotes(request.getNotes());

    // Update assignee
    if (request.getAssigneeId() != null) {
      User assignee = userRepository.findById(request.getAssigneeId())
          .orElseThrow(() -> new ResourceNotFoundException(
              messageService.getMessage("error.user.not.found", request.getAssigneeId())));
      activity.setAssignee(assignee);
    } else {
      activity.setAssignee(null);
    }

    // Update related pitch
    if (request.getRelatedPitchId() != null) {
      Pitch relatedPitch = pitchRepository.findById(request.getRelatedPitchId())
          .orElseThrow(() -> new ResourceNotFoundException(
              messageService.getMessage("error.pitch.not.found", request.getRelatedPitchId())));
      activity.setRelatedPitch(relatedPitch);
    } else {
      activity.setRelatedPitch(null);
    }

    CooldownActivity saved = cooldownActivityRepository.save(activity);
    return CooldownActivityDTO.fromEntity(saved);
  }

  /**
   * Delete an activity.
   */
  public void deleteActivity(Long id) {
    if (!cooldownActivityRepository.existsById(id)) {
      throw new ResourceNotFoundException("Cooldown activity not found: " + id);
    }
    cooldownActivityRepository.deleteById(id);
    log.info("Deleted cooldown activity {}", id);
  }

  /**
   * Assign activity to a user.
   */
  public CooldownActivityDTO assignToUser(Long activityId, Long userId) {
    CooldownActivity activity = cooldownActivityRepository.findById(activityId)
        .orElseThrow(() -> new ResourceNotFoundException("Cooldown activity not found: " + activityId));

    if (userId != null) {
      User assignee = userRepository.findById(userId)
          .orElseThrow(() -> new ResourceNotFoundException(
              messageService.getMessage("error.user.not.found", userId)));
      activity.setAssignee(assignee);
    } else {
      activity.setAssignee(null);
    }

    CooldownActivity saved = cooldownActivityRepository.save(activity);
    return CooldownActivityDTO.fromEntity(saved);
  }
}
