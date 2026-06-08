package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.github.farzadsedaghatbin.shipflow.dto.StartTimerRequest;
import com.github.farzadsedaghatbin.shipflow.dto.StopTimerResponse;
import com.github.farzadsedaghatbin.shipflow.dto.WorkLogTimerDTO;
import com.github.farzadsedaghatbin.shipflow.entity.*;
import com.github.farzadsedaghatbin.shipflow.exception.BadRequestException;
import com.github.farzadsedaghatbin.shipflow.repository.*;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class WorkLogTimerServiceTest {

  @Mock
  private WorkLogTimerRepository timerRepository;

  @Mock
  private PersonRepository personRepository;

  @Mock
  private PitchRepository pitchRepository;

  @Mock
  private TaskRepository taskRepository;

  @Mock
  private WorkLogRepository workLogRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private SecurityContext securityContext;

  @Mock
  private Authentication authentication;

  @Mock
  private LocalizationService localizationService;

  @InjectMocks
  private WorkLogTimerService timerService;

  private User user;
  private Person person;
  private Pitch pitch;
  private Task task;

  @BeforeEach
  void setUp() {
    user = User.builder().id(1L).username("testuser").build();

    person = Person.builder().id(1L).name("Test Person").user(user).build();

    pitch = Pitch.builder().id(1L).title("Test Pitch").build();

    task = Task.builder().id(1L).title("Test Task").build();

    user.setPerson(person);

    SecurityContextHolder.setContext(securityContext);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getName()).thenReturn("testuser");
    when(userRepository.findByUsernameWithPerson("testuser")).thenReturn(Optional.of(user));
  }

  @Test
  void shouldStartTimerForPitch() {
    // Given
    StartTimerRequest request = StartTimerRequest.builder().pitchId(1L).note("Working on feature").build();

    when(pitchRepository.findById(1L)).thenReturn(Optional.of(pitch));
    when(timerRepository.findByPersonId(1L)).thenReturn(Optional.empty());

    WorkLogTimer savedTimer = WorkLogTimer.builder().id(1L).person(person).pitch(pitch)
        .startTime(LocalDateTime.now()).note("Working on feature").status("RUNNING").totalPausedSeconds(0L).build();

    when(timerRepository.save(any(WorkLogTimer.class))).thenReturn(savedTimer);

    // When
    WorkLogTimerDTO result = timerService.startTimer(request);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(1L);
    assertThat(result.getPitchId()).isEqualTo(1L);
    assertThat(result.getPitchTitle()).isEqualTo("Test Pitch");
    assertThat(result.getTaskId()).isNull();
    assertThat(result.getNote()).isEqualTo("Working on feature");
    assertThat(result.getStatus()).isEqualTo("RUNNING");
    verify(timerRepository).save(any(WorkLogTimer.class));
  }

  @Test
  void shouldStartTimerForTask() {
    // Given
    StartTimerRequest request = StartTimerRequest.builder().taskId(1L).note("Working on task").build();

    when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
    when(timerRepository.findByPersonId(1L)).thenReturn(Optional.empty());

    WorkLogTimer savedTimer = WorkLogTimer.builder().id(1L).person(person).task(task).startTime(LocalDateTime.now())
        .note("Working on task").status("RUNNING").totalPausedSeconds(0L).build();

    when(timerRepository.save(any(WorkLogTimer.class))).thenReturn(savedTimer);

    // When
    WorkLogTimerDTO result = timerService.startTimer(request);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getTaskId()).isEqualTo(1L);
    assertThat(result.getTaskTitle()).isEqualTo("Test Task");
    assertThat(result.getPitchId()).isNull();
    assertThat(result.getStatus()).isEqualTo("RUNNING");
    verify(timerRepository).save(any(WorkLogTimer.class));
  }

  @Test
  void shouldFailWhenNoPitchOrTask() {
    // Given
    StartTimerRequest request = StartTimerRequest.builder().note("Invalid request").build();

    when(localizationService.getMessage("timer.invalid.params"))
        .thenReturn("Either pitchId or taskId must be provided");

    // When / Then
    assertThatThrownBy(() -> timerService.startTimer(request)).isInstanceOf(BadRequestException.class)
        .hasMessageContaining("Either pitchId or taskId must be provided");
  }

  @Test
  void shouldFailWhenBothPitchAndTask() {
    // Given
    StartTimerRequest request = StartTimerRequest.builder().pitchId(1L).taskId(1L).note("Invalid request").build();
    when(localizationService.getMessage("timer.invalid.params"))
        .thenReturn("Either pitchId or taskId must be provided, but not both");
    // When / Then
    assertThatThrownBy(() -> timerService.startTimer(request)).isInstanceOf(BadRequestException.class)
        .hasMessageContaining("Either pitchId or taskId must be provided");
  }

  @Test
  void shouldFailWhenActiveTimerExists() {
    // Given
    StartTimerRequest request = StartTimerRequest.builder().pitchId(1L).build();

    WorkLogTimer existingTimer = WorkLogTimer.builder().id(1L).person(person).pitch(pitch)
        .startTime(LocalDateTime.now().minusHours(1)).status("RUNNING").totalPausedSeconds(0L).build();

    when(timerRepository.findByPersonId(1L)).thenReturn(Optional.of(existingTimer));

    when(localizationService.getMessage("timer.already.active")).thenReturn("You already have an active timer");

    // When / Then
    assertThatThrownBy(() -> timerService.startTimer(request)).isInstanceOf(BadRequestException.class)
        .hasMessageContaining("already have an active timer");
  }

  @Test
  void shouldStopTimerAndCreateWorkLog() {
    // Given
    WorkLogTimer timer = WorkLogTimer.builder().id(1L).person(person).pitch(pitch)
        .startTime(LocalDateTime.now().minusMinutes(45)).note("Working on feature").status("RUNNING")
        .totalPausedSeconds(0L).build();

    when(timerRepository.findByPersonId(1L)).thenReturn(Optional.of(timer));

    WorkLog savedWorkLog = WorkLog.builder().id(1L).person(person).pitch(pitch).build();

    when(workLogRepository.save(any(WorkLog.class))).thenReturn(savedWorkLog);

    // When
    StopTimerResponse response = timerService.stopTimer();

    // Then
    assertThat(response).isNotNull();
    assertThat(response.getWorkLogId()).isEqualTo(1L);
    assertThat(response.getHoursSpent()).isGreaterThanOrEqualTo(0.25);
    assertThat(response.getMessage()).contains("successfully");
    verify(workLogRepository).save(any(WorkLog.class));
    verify(timerRepository).delete(timer);
  }

  @Test
  void shouldRoundTimeToQuarterHours() {
    // Given - 17 minutes should round to 0.25 hours (15 minutes minimum)
    WorkLogTimer timer = WorkLogTimer.builder().id(1L).person(person).pitch(pitch)
        .startTime(LocalDateTime.now().minusMinutes(17)).status("RUNNING").totalPausedSeconds(0L).build();

    when(timerRepository.findByPersonId(1L)).thenReturn(Optional.of(timer));

    WorkLog savedWorkLog = WorkLog.builder().id(1L).person(person).pitch(pitch).build();

    when(workLogRepository.save(any(WorkLog.class))).thenReturn(savedWorkLog);

    // When
    StopTimerResponse response = timerService.stopTimer();

    // Then
    assertThat(response.getHoursSpent()).isEqualTo(0.25);
  }

  @Test
  void shouldGetActiveTimer() {
    // Given
    WorkLogTimer timer = WorkLogTimer.builder().id(1L).person(person).pitch(pitch)
        .startTime(LocalDateTime.now().minusMinutes(30)).note("Active work").status("RUNNING")
        .totalPausedSeconds(0L).build();

    when(timerRepository.findByPersonId(1L)).thenReturn(Optional.of(timer));

    // When
    Optional<WorkLogTimerDTO> result = timerService.getActiveTimer();

    // Then
    assertThat(result).isPresent();
    assertThat(result.get().getId()).isEqualTo(1L);
    assertThat(result.get().getElapsedSeconds()).isGreaterThan(1700); // ~30 minutes
    assertThat(result.get().getStatus()).isEqualTo("RUNNING");
  }

  @Test
  void shouldReturnEmptyWhenNoActiveTimer() {
    // Given
    when(timerRepository.findByPersonId(1L)).thenReturn(Optional.empty());

    // When
    Optional<WorkLogTimerDTO> result = timerService.getActiveTimer();

    // Then
    assertThat(result).isEmpty();
  }

  @Test
  void shouldCancelTimer() {
    // Given
    WorkLogTimer timer = WorkLogTimer.builder().id(1L).person(person).pitch(pitch)
        .startTime(LocalDateTime.now().minusMinutes(10)).status("RUNNING").totalPausedSeconds(0L).build();

    when(timerRepository.findByPersonId(1L)).thenReturn(Optional.of(timer));

    // When
    timerService.cancelTimer();

    // Then
    verify(timerRepository).delete(timer);
    verifyNoInteractions(workLogRepository);
  }

  @Test
  void shouldFailCancelWhenNoActiveTimer() {
    // Given
    when(timerRepository.findByPersonId(1L)).thenReturn(Optional.empty());

    // When / Then
    assertThatThrownBy(() -> timerService.cancelTimer()).isInstanceOf(BadRequestException.class)
        .hasMessageContaining("No active timer found");
  }

  @Test
  void shouldPauseRunningTimer() {
    // Given
    WorkLogTimer timer = WorkLogTimer.builder().id(1L).person(person).pitch(pitch)
        .startTime(LocalDateTime.now().minusMinutes(10)).status("RUNNING").totalPausedSeconds(0L).build();

    when(timerRepository.findByPersonIdAndStatus(1L, "RUNNING")).thenReturn(Optional.of(timer));

    WorkLogTimer pausedTimer = WorkLogTimer.builder().id(1L).person(person).pitch(pitch)
        .startTime(timer.getStartTime()).status("PAUSED").pausedAt(LocalDateTime.now()).totalPausedSeconds(0L).build();

    when(timerRepository.save(any(WorkLogTimer.class))).thenReturn(pausedTimer);

    // When
    WorkLogTimerDTO result = timerService.pauseTimer();

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getStatus()).isEqualTo("PAUSED");
    verify(timerRepository).save(any(WorkLogTimer.class));
  }

  @Test
  void shouldFailPauseWhenNoRunningTimer() {
    // Given
    when(timerRepository.findByPersonIdAndStatus(1L, "RUNNING")).thenReturn(Optional.empty());

    // When / Then
    assertThatThrownBy(() -> timerService.pauseTimer()).isInstanceOf(BadRequestException.class)
        .hasMessageContaining("No running timer found to pause");
  }

  @Test
  void shouldResumeAPausedTimer() {
    // Given
    LocalDateTime pausedAt = LocalDateTime.now().minusMinutes(5);
    WorkLogTimer timer = WorkLogTimer.builder().id(1L).person(person).pitch(pitch)
        .startTime(LocalDateTime.now().minusMinutes(20)).status("PAUSED").pausedAt(pausedAt)
        .totalPausedSeconds(0L).build();

    when(timerRepository.findByPersonIdAndStatus(1L, "PAUSED")).thenReturn(Optional.of(timer));

    WorkLogTimer resumedTimer = WorkLogTimer.builder().id(1L).person(person).pitch(pitch)
        .startTime(timer.getStartTime()).status("RUNNING").totalPausedSeconds(300L).build();

    when(timerRepository.save(any(WorkLogTimer.class))).thenReturn(resumedTimer);

    // When
    WorkLogTimerDTO result = timerService.resumeTimer();

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getStatus()).isEqualTo("RUNNING");
    verify(timerRepository).save(any(WorkLogTimer.class));
  }

  @Test
  void shouldFailResumeWhenNoPausedTimer() {
    // Given
    when(timerRepository.findByPersonIdAndStatus(1L, "PAUSED")).thenReturn(Optional.empty());

    // When / Then
    assertThatThrownBy(() -> timerService.resumeTimer()).isInstanceOf(BadRequestException.class)
        .hasMessageContaining("No paused timer found to resume");
  }

  @Test
  void shouldExcludePausedTimeFromElapsed() {
    // Given - timer started 30 min ago, paused for 10 min (total paused = 600s)
    WorkLogTimer timer = WorkLogTimer.builder().id(1L).person(person).pitch(pitch)
        .startTime(LocalDateTime.now().minusMinutes(30)).status("RUNNING").totalPausedSeconds(600L).build();

    when(timerRepository.findByPersonId(1L)).thenReturn(Optional.of(timer));

    // When
    Optional<WorkLogTimerDTO> result = timerService.getActiveTimer();

    // Then
    assertThat(result).isPresent();
    // Wall clock ~1800s - 600s paused = ~1200s elapsed
    assertThat(result.get().getElapsedSeconds()).isGreaterThan(1100);
    assertThat(result.get().getElapsedSeconds()).isLessThan(1300);
  }

  @Test
  void shouldComputeCorrectElapsedForPausedTimer() {
    // Given - started 20 min ago, paused 5 min ago (no prior paused seconds)
    LocalDateTime pausedAt = LocalDateTime.now().minusMinutes(5);
    WorkLogTimer timer = WorkLogTimer.builder().id(1L).person(person).pitch(pitch)
        .startTime(LocalDateTime.now().minusMinutes(20)).status("PAUSED").pausedAt(pausedAt)
        .totalPausedSeconds(0L).build();

    when(timerRepository.findByPersonId(1L)).thenReturn(Optional.of(timer));

    // When
    Optional<WorkLogTimerDTO> result = timerService.getActiveTimer();

    // Then - elapsed = (pausedAt - startTime) - 0 = 15 min = ~900s
    assertThat(result).isPresent();
    assertThat(result.get().getElapsedSeconds()).isGreaterThan(850);
    assertThat(result.get().getElapsedSeconds()).isLessThan(950);
    assertThat(result.get().getStatus()).isEqualTo("PAUSED");
  }
}
