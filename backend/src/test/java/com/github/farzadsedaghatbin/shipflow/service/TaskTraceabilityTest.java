package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.CreateTaskRequest;
import com.github.farzadsedaghatbin.shipflow.dto.TaskDTO;
import com.github.farzadsedaghatbin.shipflow.entity.*;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskCategory;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskPriority;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskStatus;
import com.github.farzadsedaghatbin.shipflow.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for Task traceability relationships - pitch and scope linking.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Task Traceability Tests")
class TaskTraceabilityTest {

    @Mock
    private TaskRepository taskRepository;
    
    @Mock
    private CycleRepository cycleRepository;
    
    @Mock
    private PersonRepository personRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private TaskDependencyRepository taskDependencyRepository;
    
    @Mock
    private PitchRepository pitchRepository;
    
    @Mock
    private HillChartPointRepository hillChartPointRepository;
    
    @Mock
    private DashboardNotificationService notificationService;

    @InjectMocks
    private TaskService taskService;

    private Cycle cycle;
    private Pitch pitch;
    private HillChartPoint scope;
    private Person person;

    @BeforeEach
    void setUp() {
        cycle = Cycle.builder()
                .id(1L)
                .name("Cycle 1")
                .build();

        pitch = Pitch.builder()
                .id(1L)
                .title("User Authentication")
                .cycle(cycle)
                .build();

        scope = HillChartPoint.builder()
                .id(1L)
                .scope("Login Form")
                .description("Build login form UI")
                .pitch(pitch)
                .build();

        person = Person.builder()
                .id(1L)
                .name("John Doe")
                .build();
    }

    @Test
    @DisplayName("Should create task with pitch and scope for pitch-scoped work")
    void shouldCreateTaskWithPitchAndScope() {
        // Arrange
        CreateTaskRequest request = CreateTaskRequest.builder()
                .title("Implement login form validation")
                .description("Add client-side validation for login form")
                .cycleId(1L)
                .pitchId(1L)
                .scopeId(1L)
                .category(TaskCategory.PITCH_SCOPE)
                .priority(TaskPriority.HIGH)
                .build();

        when(cycleRepository.findById(1L)).thenReturn(Optional.of(cycle));
        when(pitchRepository.findById(1L)).thenReturn(Optional.of(pitch));
        when(hillChartPointRepository.findById(1L)).thenReturn(Optional.of(scope));
        
        Task savedTask = Task.builder()
                .id(1L)
                .title(request.getTitle())
                .description(request.getDescription())
                .cycle(cycle)
                .pitch(pitch)
                .scope(scope)
                .category(TaskCategory.PITCH_SCOPE)
                .priority(TaskPriority.HIGH)
                .status(TaskStatus.BACKLOG)
                .build();
        
        when(taskRepository.save(any(Task.class))).thenReturn(savedTask);

        // Act
        TaskDTO result = taskService.createTask(request);

        // Assert
        assertNotNull(result);
        assertEquals("Implement login form validation", result.getTitle());
        assertEquals(1L, result.getPitchId());
        assertEquals("User Authentication", result.getPitchTitle());
        assertEquals(1L, result.getScopeId());
        assertEquals("Login Form", result.getScopeName());
        
        verify(pitchRepository).findById(1L);
        verify(hillChartPointRepository).findById(1L);
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    @DisplayName("Should create task without pitch/scope for technical debt")
    void shouldCreateTaskWithoutPitchForTechnicalDebt() {
        // Arrange
        CreateTaskRequest request = CreateTaskRequest.builder()
                .title("Refactor database connection pooling")
                .description("Optimize connection pool configuration")
                .cycleId(1L)
                .category(TaskCategory.DEBT_IMPROVEMENT)
                .priority(TaskPriority.MEDIUM)
                .build();

        when(cycleRepository.findById(1L)).thenReturn(Optional.of(cycle));
        
        Task savedTask = Task.builder()
                .id(2L)
                .title(request.getTitle())
                .description(request.getDescription())
                .cycle(cycle)
                .pitch(null)  // No pitch for technical debt
                .scope(null)  // No scope for technical debt
                .category(TaskCategory.DEBT_IMPROVEMENT)
                .priority(TaskPriority.MEDIUM)
                .status(TaskStatus.BACKLOG)
                .build();
        
        when(taskRepository.save(any(Task.class))).thenReturn(savedTask);

        // Act
        TaskDTO result = taskService.createTask(request);

        // Assert
        assertNotNull(result);
        assertEquals("Refactor database connection pooling", result.getTitle());
        assertNull(result.getPitchId());
        assertNull(result.getPitchTitle());
        assertNull(result.getScopeId());
        assertNull(result.getScopeName());
        assertEquals(TaskCategory.DEBT_IMPROVEMENT, result.getCategory());
        
        verify(pitchRepository, never()).findById(anyLong());
        verify(hillChartPointRepository, never()).findById(anyLong());
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    @DisplayName("Should update task to link with pitch and scope")
    void shouldUpdateTaskToLinkWithPitchAndScope() {
        // Arrange
        Task existingTask = Task.builder()
                .id(1L)
                .title("Generic task")
                .cycle(cycle)
                .pitch(null)
                .scope(null)
                .status(TaskStatus.TODO)
                .priority(TaskPriority.LOW)
                .category(TaskCategory.PITCH_SCOPE)
                .build();

        CreateTaskRequest updateRequest = CreateTaskRequest.builder()
                .title("Generic task - now linked to pitch")
                .cycleId(1L)
                .pitchId(1L)
                .scopeId(1L)
                .build();

        when(taskRepository.findById(1L)).thenReturn(Optional.of(existingTask));
        when(pitchRepository.findById(1L)).thenReturn(Optional.of(pitch));
        when(hillChartPointRepository.findById(1L)).thenReturn(Optional.of(scope));
        when(taskRepository.save(any(Task.class))).thenReturn(existingTask);

        // Act
        TaskDTO result = taskService.updateTask(1L, updateRequest);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getPitchId());
        assertEquals(1L, result.getScopeId());
        
        verify(pitchRepository).findById(1L);
        verify(hillChartPointRepository).findById(1L);
    }

    @Test
    @DisplayName("Should throw exception when pitch not found")
    void shouldThrowExceptionWhenPitchNotFound() {
        // Arrange
        CreateTaskRequest request = CreateTaskRequest.builder()
                .title("Task with invalid pitch")
                .cycleId(1L)
                .pitchId(999L)  // Non-existent pitch
                .build();

        when(cycleRepository.findById(1L)).thenReturn(Optional.of(cycle));
        when(pitchRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> taskService.createTask(request));
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    @DisplayName("Should throw exception when scope not found")
    void shouldThrowExceptionWhenScopeNotFound() {
        // Arrange
        CreateTaskRequest request = CreateTaskRequest.builder()
                .title("Task with invalid scope")
                .cycleId(1L)
                .scopeId(999L)  // Non-existent scope
                .build();

        when(cycleRepository.findById(1L)).thenReturn(Optional.of(cycle));
        when(hillChartPointRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> taskService.createTask(request));
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    @DisplayName("Should clear pitch and scope when updated to null")
    void shouldClearPitchAndScopeWhenUpdatedToNull() {
        // Arrange
        Task existingTask = Task.builder()
                .id(1L)
                .title("Task with pitch")
                .cycle(cycle)
                .pitch(pitch)
                .scope(scope)
                .status(TaskStatus.IN_PROGRESS)
                .priority(TaskPriority.HIGH)
                .category(TaskCategory.PITCH_SCOPE)
                .build();

        CreateTaskRequest updateRequest = CreateTaskRequest.builder()
                .title("Task converted to technical debt")
                .cycleId(1L)
                .category(TaskCategory.DEBT_IMPROVEMENT)
                // pitchId and scopeId are null
                .build();

        when(taskRepository.findById(1L)).thenReturn(Optional.of(existingTask));
        when(taskRepository.save(any(Task.class))).thenReturn(existingTask);

        // Act
        TaskDTO result = taskService.updateTask(1L, updateRequest);

        // Assert
        assertNotNull(result);
        assertNull(result.getPitchId());
        assertNull(result.getScopeId());
        assertEquals(TaskCategory.DEBT_IMPROVEMENT, result.getCategory());
    }
}
