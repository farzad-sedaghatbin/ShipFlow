package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.CreateWorkLogForSelfRequest;
import com.github.farzadsedaghatbin.shipflow.dto.CreateWorkLogRequest;
import com.github.farzadsedaghatbin.shipflow.dto.WorkLogDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Person;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.Task;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.WorkLog;
import com.github.farzadsedaghatbin.shipflow.repository.PersonRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TaskRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import com.github.farzadsedaghatbin.shipflow.repository.WorkLogRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkLogService {

  private final WorkLogRepository workLogRepository;
  private final PersonRepository personRepository;
  private final PitchRepository pitchRepository;
  private final TaskRepository taskRepository;
  private final UserRepository userRepository;
  private final ApplicationEventPublisher eventPublisher;
  private final AICacheService cacheService;
  private final MessageService messageService;

  public Page<WorkLogDTO> getAllWorkLogs(Pageable pageable) {
    return workLogRepository.findAll(pageable).map(this::toDTO);
  }

  public Page<WorkLogDTO> getWorkLogsByPitchId(Long pitchId, Pageable pageable) {
    return workLogRepository.findByPitchId(pitchId, pageable).map(this::toDTO);
  }

  public Page<WorkLogDTO> getWorkLogsByTaskId(Long taskId, Pageable pageable) {
    return workLogRepository.findByTaskId(taskId, pageable).map(this::toDTO);
  }

  public Page<WorkLogDTO> getWorkLogsByPersonId(Long personId, Pageable pageable) {
    return workLogRepository.findByPersonId(personId, pageable).map(this::toDTO);
  }

  public Page<WorkLogDTO> getWorkLogsByPersonAndDate(Long personId, LocalDate date, Pageable pageable) {
    return workLogRepository.findByPersonIdAndDate(personId, date, pageable).map(this::toDTO);
  }

  public Page<WorkLogDTO> getWorkLogsByCycleId(Long cycleId, Pageable pageable) {
    return workLogRepository.findByCycleId(cycleId, pageable).map(this::toDTO);
  }

  public WorkLogDTO getWorkLogById(Long id) {
    WorkLog workLog = workLogRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Work log not found with id: " + id));
    return toDTO(workLog);
  }

  public WorkLogDTO createWorkLog(CreateWorkLogRequest request) {
    // Validate that either pitchId or taskId is provided, but not both
    if ((request.getPitchId() == null && request.getTaskId() == null)
        || (request.getPitchId() != null && request.getTaskId() != null)) {
      throw new IllegalArgumentException(messageService.getMessage("error.worklog.pitch.or.task.required"));
    }

    Person person = personRepository.findById(request.getPersonId())
        .orElseThrow(() -> new IllegalArgumentException("Person not found with id: " + request.getPersonId()));

    Pitch pitch = null;
    Task task = null;

    if (request.getPitchId() != null) {
      pitch = pitchRepository.findById(request.getPitchId()).orElseThrow(
          () -> new IllegalArgumentException("Pitch not found with id: " + request.getPitchId()));
    } else {
      task = taskRepository.findById(request.getTaskId())
          .orElseThrow(() -> new IllegalArgumentException("Task not found with id: " + request.getTaskId()));
    }

    WorkLog workLog = WorkLog.builder().person(person).pitch(pitch).task(task).date(request.getDate())
        .hoursSpent(request.getHoursSpent()).note(request.getNote()).build();

    WorkLog saved = workLogRepository.save(workLog);

    // Publish event for knowledge ingestion (only if note is present)
    if (saved.getNote() != null && !saved.getNote().trim().isEmpty()) {
      eventPublisher.publishEvent(new KnowledgeEventListener.WorkLogKnowledgeEvent(saved.getId()));
    }

    // Invalidate risk analysis cache since hours changed
    if (pitch != null) {
      invalidateCacheForPitch(pitch);
    }

    return toDTO(saved);
  }

  public WorkLogDTO updateWorkLog(Long id, CreateWorkLogRequest request) {
    WorkLog workLog = workLogRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Work log not found with id: " + id));

    workLog.setDate(request.getDate());
    workLog.setHoursSpent(request.getHoursSpent());
    workLog.setNote(request.getNote());

    WorkLog saved = workLogRepository.save(workLog);

    // Publish event for knowledge ingestion (only if note is present)
    if (saved.getNote() != null && !saved.getNote().trim().isEmpty()) {
      eventPublisher.publishEvent(new KnowledgeEventListener.WorkLogKnowledgeEvent(saved.getId()));
    }

    // Invalidate risk analysis cache since hours changed
    invalidateCacheForPitch(saved.getPitch());

    return toDTO(saved);
  }

  public void deleteWorkLog(Long id) {
    WorkLog workLog = workLogRepository.findById(id).orElse(null);
    Pitch pitch = workLog != null ? workLog.getPitch() : null;

    workLogRepository.deleteById(id);

    // Invalidate risk analysis cache since hours changed
    if (pitch != null) {
      invalidateCacheForPitch(pitch);
    }
  }

  /** Invalidate cache for pitch and its cycle when work log data changes. */
  private void invalidateCacheForPitch(Pitch pitch) {
    if (pitch != null) {
      cacheService.invalidatePitchRiskCache(pitch.getId());
      if (pitch.getCycle() != null) {
        cacheService.invalidateCycleRiskCache(pitch.getCycle().getId());
      }
    }
  }

  // ========== Methods for current user's own work logs ==========

  /** Get the current authenticated user's Person entity */
  private Person getCurrentUserPerson() {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    User user = userRepository.findByUsernameWithPerson(username)
        .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

    if (user.getPerson() == null) {
      throw new IllegalArgumentException(messageService.getMessage("error.user.no.person.profile"));
    }

    return user.getPerson();
  }

  /** Get all work logs for the current user */
  public Page<WorkLogDTO> getMyWorkLogs(Pageable pageable) {
    Person person = getCurrentUserPerson();
    return workLogRepository.findByPersonId(person.getId(), pageable).map(this::toDTO);
  }

  /** Get work logs for the current user by cycle */
  public Page<WorkLogDTO> getMyWorkLogsByCycle(Long cycleId, Pageable pageable) {
    Person person = getCurrentUserPerson();
    return workLogRepository.findByPersonIdAndCycleId(person.getId(), cycleId, pageable).map(this::toDTO);
  }

  /** Get work logs for the current user by date */
  public Page<WorkLogDTO> getMyWorkLogsByDate(LocalDate date, Pageable pageable) {
    Person person = getCurrentUserPerson();
    return workLogRepository.findByPersonIdAndDate(person.getId(), date, pageable).map(this::toDTO);
  }

  /** Create a work log for the current user (for themselves) */
  public WorkLogDTO createMyWorkLog(CreateWorkLogForSelfRequest request) {
    // Validate that either pitchId or taskId is provided, but not both
    if ((request.getPitchId() == null && request.getTaskId() == null)
        || (request.getPitchId() != null && request.getTaskId() != null)) {
      throw new IllegalArgumentException(messageService.getMessage("error.worklog.pitch.or.task.required"));
    }

    Person person = getCurrentUserPerson();

    Pitch pitch = null;
    Task task = null;

    if (request.getPitchId() != null) {
      pitch = pitchRepository.findById(request.getPitchId()).orElseThrow(
          () -> new IllegalArgumentException("Pitch not found with id: " + request.getPitchId()));
    } else {
      task = taskRepository.findById(request.getTaskId())
          .orElseThrow(() -> new IllegalArgumentException("Task not found with id: " + request.getTaskId()));
    }

    WorkLog workLog = WorkLog.builder().person(person).pitch(pitch).task(task).date(request.getDate())
        .hoursSpent(request.getHoursSpent()).note(request.getNote()).build();

    WorkLog saved = workLogRepository.save(workLog);

    // Publish event for knowledge ingestion (only if note is present)
    if (saved.getNote() != null && !saved.getNote().trim().isEmpty()) {
      eventPublisher.publishEvent(new KnowledgeEventListener.WorkLogKnowledgeEvent(saved.getId()));
    }

    return toDTO(saved);
  }

  /** Update a work log owned by the current user */
  public WorkLogDTO updateMyWorkLog(Long id, CreateWorkLogForSelfRequest request) {
    // Validate that either pitchId or taskId is provided, but not both
    if ((request.getPitchId() == null && request.getTaskId() == null)
        || (request.getPitchId() != null && request.getTaskId() != null)) {
      throw new IllegalArgumentException(messageService.getMessage("error.worklog.pitch.or.task.required"));
    }

    Person person = getCurrentUserPerson();
    WorkLog workLog = workLogRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Work log not found with id: " + id));

    // Verify ownership
    if (!workLog.getPerson().getId().equals(person.getId())) {
      throw new IllegalArgumentException(messageService.getMessage("error.worklog.update.own.only"));
    }

    Pitch pitch = null;
    Task task = null;

    if (request.getPitchId() != null) {
      pitch = pitchRepository.findById(request.getPitchId()).orElseThrow(
          () -> new IllegalArgumentException("Pitch not found with id: " + request.getPitchId()));
    } else {
      task = taskRepository.findById(request.getTaskId())
          .orElseThrow(() -> new IllegalArgumentException("Task not found with id: " + request.getTaskId()));
    }

    workLog.setPitch(pitch);
    workLog.setTask(task);
    workLog.setDate(request.getDate());
    workLog.setHoursSpent(request.getHoursSpent());
    workLog.setNote(request.getNote());

    WorkLog saved = workLogRepository.save(workLog);

    // Publish event for knowledge ingestion (only if note is present)
    if (saved.getNote() != null && !saved.getNote().trim().isEmpty()) {
      eventPublisher.publishEvent(new KnowledgeEventListener.WorkLogKnowledgeEvent(saved.getId()));
    }

    return toDTO(saved);
  }

  /** Delete a work log owned by the current user */
  public void deleteMyWorkLog(Long id) {
    Person person = getCurrentUserPerson();
    WorkLog workLog = workLogRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Work log not found with id: " + id));

    // Verify ownership
    if (!workLog.getPerson().getId().equals(person.getId())) {
      throw new IllegalArgumentException(messageService.getMessage("error.worklog.delete.own.only"));
    }

    workLogRepository.deleteById(id);
  }

  private WorkLogDTO toDTO(WorkLog workLog) {
    WorkLogDTO.WorkLogDTOBuilder builder = WorkLogDTO.builder().id(workLog.getId())
        .personId(workLog.getPerson().getId()).personName(workLog.getPerson().getName()).date(workLog.getDate())
        .hoursSpent(workLog.getHoursSpent()).note(workLog.getNote());

    // Add pitch information if present
    if (workLog.getPitch() != null) {
      builder.pitchId(workLog.getPitch().getId()).pitchTitle(workLog.getPitch().getTitle())
          .cycleId(workLog.getPitch().getCycle() != null ? workLog.getPitch().getCycle().getId() : null)
          .cycleName(workLog.getPitch().getCycle() != null ? workLog.getPitch().getCycle().getName() : null)
          .projectId(
              workLog.getPitch().getCycle() != null && workLog.getPitch().getCycle().getProject() != null
                  ? workLog.getPitch().getCycle().getProject().getId()
                  : null)
          .projectName(
              workLog.getPitch().getCycle() != null && workLog.getPitch().getCycle().getProject() != null
                  ? workLog.getPitch().getCycle().getProject().getName()
                  : null)
          .projectKey(
              workLog.getPitch().getCycle() != null && workLog.getPitch().getCycle().getProject() != null
                  ? workLog.getPitch().getCycle().getProject().getProjectKey()
                  : null);
    }

    // Add task information if present
    if (workLog.getTask() != null) {
      builder.taskId(workLog.getTask().getId()).taskTitle(workLog.getTask().getTitle())
          .cycleId(workLog.getTask().getCycle() != null ? workLog.getTask().getCycle().getId() : null)
          .cycleName(workLog.getTask().getCycle() != null ? workLog.getTask().getCycle().getName() : null)
          .projectId(workLog.getTask().getCycle() != null && workLog.getTask().getCycle().getProject() != null
              ? workLog.getTask().getCycle().getProject().getId()
              : null)
          .projectName(
              workLog.getTask().getCycle() != null && workLog.getTask().getCycle().getProject() != null
                  ? workLog.getTask().getCycle().getProject().getName()
                  : null)
          .projectKey(
              workLog.getTask().getCycle() != null && workLog.getTask().getCycle().getProject() != null
                  ? workLog.getTask().getCycle().getProject().getProjectKey()
                  : null);
    }

    return builder.build();
  }
}
