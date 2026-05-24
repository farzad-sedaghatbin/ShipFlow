package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.*;
import com.github.farzadsedaghatbin.shipflow.entity.*;
import com.github.farzadsedaghatbin.shipflow.exception.BadRequestException;
import com.github.farzadsedaghatbin.shipflow.repository.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class WorkLogTimerService {

  private final WorkLogTimerRepository timerRepository;
  private final PitchRepository pitchRepository;
  private final TaskRepository taskRepository;
  private final WorkLogRepository workLogRepository;
  private final UserRepository userRepository;
  private final LocalizationService localizationService;

  public WorkLogTimerDTO startTimer(StartTimerRequest request) {
    Person person = getCurrentUserPerson();

    // Validate pitch or task
    if ((request.getPitchId() == null && request.getTaskId() == null)
        || (request.getPitchId() != null && request.getTaskId() != null)) {
      throw new BadRequestException(localizationService.getMessage("timer.invalid.params"));
    }

    // Check if user already has an active timer
    Optional<WorkLogTimer> existingTimer = timerRepository.findByPersonId(person.getId());
    if (existingTimer.isPresent()) {
      throw new BadRequestException(localizationService.getMessage("timer.already.active"));
    }

    // Validate pitch or task existence
    Pitch pitch = null;
    Task task = null;

    if (request.getPitchId() != null) {
      pitch = pitchRepository.findById(request.getPitchId())
          .orElseThrow(() -> new BadRequestException("Pitch not found with id: " + request.getPitchId()));
    }

    if (request.getTaskId() != null) {
      task = taskRepository.findById(request.getTaskId())
          .orElseThrow(() -> new BadRequestException("Task not found with id: " + request.getTaskId()));
    }

    // Create timer
    WorkLogTimer timer = WorkLogTimer.builder().person(person).pitch(pitch).task(task)
        .startTime(LocalDateTime.now()).note(request.getNote()).status("RUNNING").totalPausedSeconds(0L).build();

    WorkLogTimer saved = timerRepository.save(timer);
    log.info("Started timer {} for person {}", saved.getId(), person.getName());

    return toDTO(saved);
  }

  public StopTimerResponse stopTimer() {
    Person person = getCurrentUserPerson();

    WorkLogTimer timer = timerRepository.findByPersonId(person.getId())
        .orElseThrow(() -> new BadRequestException("No active timer found"));

    LocalDateTime stopTime = LocalDateTime.now();

    // Compute effective elapsed seconds (exclude paused time)
    long elapsedSeconds = computeElapsedSeconds(timer, stopTime);

    // Calculate hours with 2 decimal places (minimum 0.25 hours = 15 minutes)
    double elapsedMinutes = elapsedSeconds / 60.0;
    double hours = Math.max(0.25, Math.round(elapsedMinutes / 15.0) * 0.25);

    // Create work log from timer
    WorkLog workLog = WorkLog.builder().person(person).pitch(timer.getPitch()).task(timer.getTask())
        .date(LocalDate.now()).hoursSpent(BigDecimal.valueOf(hours).setScale(2, RoundingMode.HALF_UP))
        .note(timer.getNote()).build();

    WorkLog savedWorkLog = workLogRepository.save(workLog);

    // Delete timer
    timerRepository.delete(timer);

    log.info("Stopped timer {} and created work log {} for {} hours", timer.getId(), savedWorkLog.getId(), hours);

    return StopTimerResponse.builder().workLogId(savedWorkLog.getId()).hoursSpent(hours)
        .message("Timer stopped and work log created successfully").build();
  }

  public Optional<WorkLogTimerDTO> getActiveTimer() {
    Person person = getCurrentUserPerson();
    return timerRepository.findByPersonId(person.getId()).map(this::toDTO);
  }

  public void cancelTimer() {
    Person person = getCurrentUserPerson();

    WorkLogTimer timer = timerRepository.findByPersonId(person.getId())
        .orElseThrow(() -> new BadRequestException("No active timer found"));

    timerRepository.delete(timer);
    log.info("Cancelled timer {} for person {}", timer.getId(), person.getName());
  }

  public WorkLogTimerDTO pauseTimer() {
    Person person = getCurrentUserPerson();

    WorkLogTimer timer = timerRepository.findByPersonIdAndStatus(person.getId(), "RUNNING")
        .orElseThrow(() -> new BadRequestException("No running timer found to pause"));

    timer.setStatus("PAUSED");
    timer.setPausedAt(LocalDateTime.now());
    WorkLogTimer saved = timerRepository.save(timer);

    log.info("Paused timer {} for person {}", saved.getId(), person.getName());
    return toDTO(saved);
  }

  public WorkLogTimerDTO resumeTimer() {
    Person person = getCurrentUserPerson();

    WorkLogTimer timer = timerRepository.findByPersonIdAndStatus(person.getId(), "PAUSED")
        .orElseThrow(() -> new BadRequestException("No paused timer found to resume"));

    LocalDateTime now = LocalDateTime.now();
    if (timer.getPausedAt() != null) {
      long additionalPausedSeconds = Duration.between(timer.getPausedAt(), now).getSeconds();
      timer.setTotalPausedSeconds(timer.getTotalPausedSeconds() + additionalPausedSeconds);
    }

    timer.setStatus("RUNNING");
    timer.setPausedAt(null);
    WorkLogTimer saved = timerRepository.save(timer);

    log.info("Resumed timer {} for person {}", saved.getId(), person.getName());
    return toDTO(saved);
  }

  private long computeElapsedSeconds(WorkLogTimer timer, LocalDateTime referenceTime) {
    if ("PAUSED".equals(timer.getStatus()) && timer.getPausedAt() != null) {
      // For a paused timer: elapsed = (pausedAt - startTime) - totalPausedSeconds
      long wallClockSeconds = Duration.between(timer.getStartTime(), timer.getPausedAt()).getSeconds();
      return Math.max(0, wallClockSeconds - timer.getTotalPausedSeconds());
    } else {
      // For a running timer: elapsed = (now - startTime) - totalPausedSeconds
      long wallClockSeconds = Duration.between(timer.getStartTime(), referenceTime).getSeconds();
      return Math.max(0, wallClockSeconds - timer.getTotalPausedSeconds());
    }
  }

  private WorkLogTimerDTO toDTO(WorkLogTimer timer) {
    long elapsedSeconds = computeElapsedSeconds(timer, LocalDateTime.now());

    return WorkLogTimerDTO.builder().id(timer.getId()).personId(timer.getPerson().getId())
        .personName(timer.getPerson().getName())
        .pitchId(timer.getPitch() != null ? timer.getPitch().getId() : null)
        .pitchTitle(timer.getPitch() != null ? timer.getPitch().getTitle() : null)
        .taskId(timer.getTask() != null ? timer.getTask().getId() : null)
        .taskTitle(timer.getTask() != null ? timer.getTask().getTitle() : null).startTime(timer.getStartTime())
        .note(timer.getNote()).elapsedSeconds(elapsedSeconds).status(timer.getStatus())
        .pausedAt(timer.getPausedAt()).build();
  }

  private Person getCurrentUserPerson() {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    User user = userRepository.findByUsernameWithPerson(username)
        .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

    if (user.getPerson() == null) {
      throw new IllegalArgumentException(localizationService.getMessage("timer.no.person.profile"));
    }

    return user.getPerson();
  }
}
