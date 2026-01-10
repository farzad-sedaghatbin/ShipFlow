package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.CreateWorkLogRequest;
import com.github.farzadsedaghatbin.shipflow.dto.WorkLogDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.Person;
import com.github.farzadsedaghatbin.shipflow.entity.Task;
import com.github.farzadsedaghatbin.shipflow.entity.WorkLog;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PersonRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TaskRepository;
import com.github.farzadsedaghatbin.shipflow.repository.WorkLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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

    @InjectMocks
    private WorkLogService workLogService;

    private WorkLog testWorkLog;
    private Person testPerson;
    private Pitch testPitch;
    private Task testTask;
    private CreateWorkLogRequest testRequest;

    @BeforeEach
    void setUp() {
        testPerson = Person.builder()
                .id(1L)
                .name("John Doe")
                .email("john@example.com")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        testPitch = Pitch.builder()
                .id(1L)
                .title("Test Pitch")
                .build();
        
        testTask = Task.builder()
                .id(1L)
                .title("Test Task")
                .build();

        testWorkLog = WorkLog.builder()
                .id(1L)
                .person(testPerson)
                .pitch(testPitch)
                .date(LocalDate.now())
                .hoursSpent(BigDecimal.valueOf(8.0))
                .note("Test work log entry")
                .build();

        testRequest = new CreateWorkLogRequest();
        testRequest.setPersonId(1L);
        testRequest.setPitchId(1L);
        testRequest.setDate(LocalDate.now());
        testRequest.setHoursSpent(BigDecimal.valueOf(8.0));
        testRequest.setNote("Test work log entry");
    }

    @Test
    void getAllWorkLogs_ShouldReturnAllWorkLogs() {
        when(workLogRepository.findAll()).thenReturn(Arrays.asList(testWorkLog));

        List<WorkLogDTO> result = workLogService.getAllWorkLogs();

        assertThat(result).hasSize(1);
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

        assertThatThrownBy(() -> workLogService.getWorkLogById(999L))
                .isInstanceOf(RuntimeException.class)
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
        when(workLogRepository.findByPitchId(1L)).thenReturn(Arrays.asList(testWorkLog));

        List<WorkLogDTO> result = workLogService.getWorkLogsByPitchId(1L);

        assertThat(result).hasSize(1);
        verify(workLogRepository).findByPitchId(1L);
    }

    @Test
    void getWorkLogsByPersonId_ShouldReturnWorkLogs() {
        when(workLogRepository.findByPersonId(1L)).thenReturn(Arrays.asList(testWorkLog));

        List<WorkLogDTO> result = workLogService.getWorkLogsByPersonId(1L);

        assertThat(result).hasSize(1);
        verify(workLogRepository).findByPersonId(1L);
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
        
        WorkLog taskWorkLog = WorkLog.builder()
                .id(2L)
                .person(testPerson)
                .task(testTask)
                .date(LocalDate.now())
                .hoursSpent(BigDecimal.valueOf(4.0))
                .note("Working on task")
                .build();
        
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

        assertThatThrownBy(() -> workLogService.createWorkLog(invalidRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Either pitchId or taskId must be provided, but not both");
    }
    
    @Test
    void getWorkLogsByTaskId_ShouldReturnWorkLogs() {
        WorkLog taskWorkLog = WorkLog.builder()
                .id(2L)
                .person(testPerson)
                .task(testTask)
                .date(LocalDate.now())
                .hoursSpent(BigDecimal.valueOf(4.0))
                .build();
        
        when(workLogRepository.findByTaskId(1L)).thenReturn(Arrays.asList(taskWorkLog));

        List<WorkLogDTO> result = workLogService.getWorkLogsByTaskId(1L);

        assertThat(result).hasSize(1);
        verify(workLogRepository).findByTaskId(1L);
    }
}
