package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.github.farzadsedaghatbin.shipflow.dto.CreateWorkLogRequest;
import com.github.farzadsedaghatbin.shipflow.dto.WorkLogDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Person;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.Task;
import com.github.farzadsedaghatbin.shipflow.entity.WorkLog;
import com.github.farzadsedaghatbin.shipflow.repository.PersonRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TaskRepository;
import com.github.farzadsedaghatbin.shipflow.repository.WorkLogRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class WorkLogServiceTest {

  @Mock
  private WorkLogRepository workLogRepository;

  @Mock
  private PersonRepository personRepository;

  @Mock
  private PitchRepository pitchRepository;

  @Mock
  private TaskRepository taskRepository;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  @Mock
  private AICacheService cacheService;

  @Mock
  private MessageService messageService;

  @InjectMocks
  private WorkLogService workLogService;

  private WorkLog testWorkLog;
  private Person testPerson;
  private Pitch testPitch;
  private Task testTask;
  private CreateWorkLogRequest testRequest;

  @BeforeEach
  void setUp() {
    testPerson = Person.builder().id(1L).name("John Doe").email("john@example.com").isActive(true)
        .createdAt(LocalDateTime.now()).build();

    testPitch = Pitch.builder().id(1L).title("Test Pitch").build();

    testTask = Task.builder().id(1L).title("Test Task").build();

    testWorkLog = WorkLog.builder().id(1L).person(testPerson).pitch(testPitch).date(LocalDate.now())
        .hoursSpent(BigDecimal.valueOf(8.0)).note("Test work log entry").build();

    testRequest = new CreateWorkLogRequest();
    testRequest.setPersonId(1L);
    testRequest.setPitchId(1L);
    testRequest.setDate(LocalDate.now());
    testRequest.setHoursSpent(BigDecimal.valueOf(8.0));
    testRequest.setNote("Test work log entry");
  }

  @Test
  void getAllWorkLogs_ShouldReturnAllWorkLogs() {
    Pageable pageable = PageRequest.of(0, 20);
    when(workLogRepository.findAll(pageable)).thenReturn(new PageImpl<>(Arrays.asList(testWorkLog)));

    Page<WorkLogDTO> result = workLogService.getAllWorkLogs(pageable);

    assertThat(result.getContent()).hasSize(1);
  }

  @Test
  void getWorkLogById_WhenExists_ShouldReturnWorkLog() {
    when(workLogRepository.findById(1L)).thenReturn(Optional.of(testWorkLog));

    WorkLogDTO result = workLogService.getWorkLogById(1L);

    assertThat(result).isNotNull();
  }

  @Test
  void getWorkLogById_WhenNotExists_ShouldThrowException() {
    when(workLogRepository.findById(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> workLogService.getWorkLogById(999L)).isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Work log not found");
  }

  @Test
  void createWorkLog_ShouldSaveWorkLog() {
    when(personRepository.findById(1L)).thenReturn(Optional.of(testPerson));
    when(pitchRepository.findById(1L)).thenReturn(Optional.of(testPitch));
    when(workLogRepository.save(any(WorkLog.class))).thenReturn(testWorkLog);

    WorkLogDTO result = workLogService.createWorkLog(testRequest);

    assertThat(result).isNotNull();
    verify(workLogRepository).save(any(WorkLog.class));
  }

  @Test
  void updateWorkLog_WhenExists_ShouldUpdateWorkLog() {
    when(workLogRepository.findById(1L)).thenReturn(Optional.of(testWorkLog));
    when(pitchRepository.findById(1L)).thenReturn(Optional.of(testPitch));
    when(workLogRepository.save(any(WorkLog.class))).thenReturn(testWorkLog);

    testRequest.setHoursSpent(BigDecimal.valueOf(6.0));
    WorkLogDTO result = workLogService.updateWorkLog(1L, testRequest);

    assertThat(result).isNotNull();
    verify(workLogRepository).save(any(WorkLog.class));
  }

  @Test
  void deleteWorkLog_ShouldCallRepository() {
    doNothing().when(workLogRepository).deleteById(1L);

    workLogService.deleteWorkLog(1L);

    verify(workLogRepository).deleteById(1L);
  }

  @Test
  void getWorkLogsByPitchId_ShouldReturnWorkLogs() {
    Pageable pageable = PageRequest.of(0, 20);
    when(workLogRepository.findByPitchId(1L, pageable)).thenReturn(new PageImpl<>(Arrays.asList(testWorkLog)));

    Page<WorkLogDTO> result = workLogService.getWorkLogsByPitchId(1L, pageable);

    assertThat(result.getContent()).hasSize(1);
  }

  @Test
  void getWorkLogsByPersonId_ShouldReturnWorkLogs() {
    Pageable pageable = PageRequest.of(0, 20);
    when(workLogRepository.findByPersonId(1L, pageable)).thenReturn(new PageImpl<>(Arrays.asList(testWorkLog)));

    Page<WorkLogDTO> result = workLogService.getWorkLogsByPersonId(1L, pageable);

    assertThat(result.getContent()).hasSize(1);
  }

  // ========== Task-based Work Log Tests ==========

  @Test
  void createWorkLog_WithTask_ShouldSaveWorkLog() {
    CreateWorkLogRequest taskRequest = new CreateWorkLogRequest();
    taskRequest.setPersonId(1L);
    taskRequest.setTaskId(1L);
    taskRequest.setDate(LocalDate.now());
    taskRequest.setHoursSpent(BigDecimal.valueOf(4.0));
    taskRequest.setNote("Working on task");

    WorkLog taskWorkLog = WorkLog.builder().id(2L).person(testPerson).task(testTask).date(LocalDate.now())
        .hoursSpent(BigDecimal.valueOf(4.0)).note("Working on task").build();

    when(personRepository.findById(1L)).thenReturn(Optional.of(testPerson));
    when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
    when(workLogRepository.save(any(WorkLog.class))).thenReturn(taskWorkLog);

    WorkLogDTO result = workLogService.createWorkLog(taskRequest);

    assertThat(result).isNotNull();
    verify(workLogRepository).save(any(WorkLog.class));
    verify(taskRepository).findById(1L);
  }

  @Test
  void createWorkLog_WithoutPitchOrTask_ShouldThrowException() {
    CreateWorkLogRequest invalidRequest = new CreateWorkLogRequest();
    invalidRequest.setPersonId(1L);
    invalidRequest.setDate(LocalDate.now());
    invalidRequest.setHoursSpent(BigDecimal.valueOf(4.0));

    when(messageService.getMessage("error.worklog.pitch.or.task.required"))
        .thenReturn("Either pitchId or taskId must be provided");

    assertThatThrownBy(() -> workLogService.createWorkLog(invalidRequest))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Either pitchId or taskId must be provided");
  }

  @Test
  void createWorkLog_WithBothPitchAndTask_ShouldThrowException() {
    CreateWorkLogRequest invalidRequest = new CreateWorkLogRequest();
    invalidRequest.setPersonId(1L);
    invalidRequest.setPitchId(1L);
    invalidRequest.setTaskId(1L);
    invalidRequest.setDate(LocalDate.now());
    invalidRequest.setHoursSpent(BigDecimal.valueOf(4.0));

    when(messageService.getMessage("error.worklog.pitch.or.task.required"))
        .thenReturn("Either pitchId or taskId must be provided, but not both");

    assertThatThrownBy(() -> workLogService.createWorkLog(invalidRequest))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Either pitchId or taskId must be provided, but not both");
  }

  @Test
  void getWorkLogsByTaskId_ShouldReturnWorkLogs() {
    WorkLog taskWorkLog = WorkLog.builder().id(2L).person(testPerson).task(testTask).date(LocalDate.now())
        .hoursSpent(BigDecimal.valueOf(4.0)).build();

    Pageable pageable = PageRequest.of(0, 20);
    when(workLogRepository.findByTaskId(1L, pageable)).thenReturn(new PageImpl<>(Arrays.asList(taskWorkLog)));

    Page<WorkLogDTO> result = workLogService.getWorkLogsByTaskId(1L, pageable);

    assertThat(result.getContent()).hasSize(1);
  }

  // ========== getWorkLogsFiltered tests ==========

  @Test
  void getWorkLogsFiltered_NoFilters_ShouldReturnAll() {
    Pageable pageable = PageRequest.of(0, 20);
    when(workLogRepository.findAll(pageable)).thenReturn(new PageImpl<>(Arrays.asList(testWorkLog)));

    Page<WorkLogDTO> result = workLogService.getWorkLogsFiltered(null, null, null, null, pageable);

    assertThat(result.getContent()).hasSize(1);
    verify(workLogRepository).findAll(pageable);
  }

  @Test
  void getWorkLogsFiltered_ByPersonIdOnly_ShouldCallPersonIdQuery() {
    Pageable pageable = PageRequest.of(0, 20);
    when(workLogRepository.findByPersonId(1L, pageable)).thenReturn(new PageImpl<>(Arrays.asList(testWorkLog)));

    Page<WorkLogDTO> result = workLogService.getWorkLogsFiltered(1L, null, null, null, pageable);

    assertThat(result.getContent()).hasSize(1);
    verify(workLogRepository).findByPersonId(1L, pageable);
  }

  @Test
  void getWorkLogsFiltered_ByPersonAndProjectId_ShouldCallCombinedQuery() {
    Pageable pageable = PageRequest.of(0, 20);
    when(workLogRepository.findByPersonIdAndProjectId(1L, 2L, pageable))
        .thenReturn(new PageImpl<>(Arrays.asList(testWorkLog)));

    Page<WorkLogDTO> result = workLogService.getWorkLogsFiltered(1L, 2L, null, null, pageable);

    assertThat(result.getContent()).hasSize(1);
    verify(workLogRepository).findByPersonIdAndProjectId(1L, 2L, pageable);
  }

  @Test
  void getWorkLogsFiltered_ByPersonIdAndDateRange_ShouldCallDateBetweenQuery() {
    LocalDate from = LocalDate.now().minusDays(7);
    LocalDate to = LocalDate.now();
    Pageable pageable = PageRequest.of(0, 20);
    when(workLogRepository.findByPersonIdAndDateBetween(1L, from, to, pageable))
        .thenReturn(new PageImpl<>(Arrays.asList(testWorkLog)));

    Page<WorkLogDTO> result = workLogService.getWorkLogsFiltered(1L, null, from, to, pageable);

    assertThat(result.getContent()).hasSize(1);
    verify(workLogRepository).findByPersonIdAndDateBetween(1L, from, to, pageable);
  }

  @Test
  void getWorkLogsFiltered_AllFilters_ShouldCallFullCombinedQuery() {
    LocalDate from = LocalDate.now().minusDays(7);
    LocalDate to = LocalDate.now();
    Pageable pageable = PageRequest.of(0, 20);
    when(workLogRepository.findByPersonIdAndProjectIdAndDateBetween(1L, 2L, from, to, pageable))
        .thenReturn(new PageImpl<>(Arrays.asList(testWorkLog)));

    Page<WorkLogDTO> result = workLogService.getWorkLogsFiltered(1L, 2L, from, to, pageable);

    assertThat(result.getContent()).hasSize(1);
    verify(workLogRepository).findByPersonIdAndProjectIdAndDateBetween(1L, 2L, from, to, pageable);
  }

  // ========== getWorkLogsFilteredByCycle tests ==========

  @Test
  void getWorkLogsFilteredByCycle_NoPersonFilter_ShouldCallCycleOnlyQuery() {
    Pageable pageable = PageRequest.of(0, 20);
    when(workLogRepository.findByCycleId(5L, pageable)).thenReturn(new PageImpl<>(Arrays.asList(testWorkLog)));

    Page<WorkLogDTO> result = workLogService.getWorkLogsFilteredByCycle(5L, null, null, null, pageable);

    assertThat(result.getContent()).hasSize(1);
    verify(workLogRepository).findByCycleId(5L, pageable);
  }

  @Test
  void getWorkLogsFilteredByCycle_ByPersonId_ShouldCallPersonAndCycleQuery() {
    Pageable pageable = PageRequest.of(0, 20);
    when(workLogRepository.findByPersonIdAndCycleId(1L, 5L, pageable))
        .thenReturn(new PageImpl<>(Arrays.asList(testWorkLog)));

    Page<WorkLogDTO> result = workLogService.getWorkLogsFilteredByCycle(5L, 1L, null, null, pageable);

    assertThat(result.getContent()).hasSize(1);
    verify(workLogRepository).findByPersonIdAndCycleId(1L, 5L, pageable);
  }

  @Test
  void getWorkLogsFilteredByCycle_ByPersonIdAndDateRange_ShouldCallFullCombinedQuery() {
    LocalDate from = LocalDate.now().minusDays(14);
    LocalDate to = LocalDate.now();
    Pageable pageable = PageRequest.of(0, 20);
    when(workLogRepository.findByPersonIdAndCycleIdAndDateBetween(1L, 5L, from, to, pageable))
        .thenReturn(new PageImpl<>(Arrays.asList(testWorkLog)));

    Page<WorkLogDTO> result = workLogService.getWorkLogsFilteredByCycle(5L, 1L, from, to, pageable);

    assertThat(result.getContent()).hasSize(1);
    verify(workLogRepository).findByPersonIdAndCycleIdAndDateBetween(1L, 5L, from, to, pageable);
  }
}
