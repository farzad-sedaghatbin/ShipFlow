package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Task;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.UserRole;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskPriority;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskCategory;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import com.github.farzadsedaghatbin.shipflow.repository.TaskRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Task Soft Delete Tests")
class TaskSoftDeleteTest {

  @Mock
  private TaskRepository taskRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private LocalizationService messageService;

  @InjectMocks
  private TaskService taskService;

  @Mock
  private SecurityContext securityContext;

  @Mock
  private Authentication authentication;

  private User testUser;
  private Task testTask;
  private Cycle testCycle;

  @BeforeEach
  void setUp() {
    // Setup test user
    testUser = User.builder().id(1L).username("testuser").email("test@example.com").role(UserRole.MANAGER)
        .isActive(true).build();

    // Setup test cycle
    testCycle = Cycle.builder().id(1L).name("Test Cycle").build();

    // Setup test task
    testTask = Task.builder().id(1L).title("Test Task").description("Test Description").status(TaskStatus.TODO)
        .priority(TaskPriority.MEDIUM).category(TaskCategory.PITCH_SCOPE).cycle(testCycle)
        .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

    // Mock security context
    SecurityContextHolder.setContext(securityContext);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getName()).thenReturn("testuser");
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
  }

  @Test
  @DisplayName("Should soft delete task successfully")
  void shouldSoftDeleteTaskSuccessfully() {
    // Given
    when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
    when(taskRepository.save(any(Task.class))).thenReturn(testTask);

    // When
    taskService.deleteTask(1L);

    // Then
    verify(taskRepository).save(testTask);
    assertThat(testTask.getDeletedAt()).isNotNull();
    assertThat(testTask.getDeletedBy()).isEqualTo(testUser);
  }

  @Test
  @DisplayName("Should throw exception when task not found")
  void shouldThrowExceptionWhenTaskNotFound() {
    // Given
    when(taskRepository.findById(999L)).thenReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> taskService.deleteTask(999L)).isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Task not found with id: 999");
  }

  @Test
  @DisplayName("Should throw exception when task already deleted")
  void shouldThrowExceptionWhenTaskAlreadyDeleted() {
    // Given
    testTask.setDeletedAt(LocalDateTime.now());
    testTask.setDeletedBy(testUser);
    when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));

    // When & Then
    assertThatThrownBy(() -> taskService.deleteTask(1L)).isInstanceOf(IllegalStateException.class)
        .hasMessage("Task is already deleted");
  }

  @Test
  @DisplayName("Should not return deleted tasks in getAllTasks")
  void shouldNotReturnDeletedTasksInGetAllTasks() {
    // Given
    when(taskRepository.findAllNotDeleted()).thenReturn(java.util.List.of());

    // When
    var result = taskService.getAllTasks();

    // Then
    assertThat(result).isEmpty();
    verify(taskRepository).findAllNotDeleted();
    verify(taskRepository, never()).findAll();
  }

  @Test
  @DisplayName("Should not return deleted task in getTaskById")
  void shouldNotReturnDeletedTaskInGetTaskById() {
    // Given
    when(taskRepository.findByIdNotDeleted(1L)).thenReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> taskService.getTaskById(1L)).isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Task not found with id: 1");
  }

  @Test
  @DisplayName("Should exclude deleted tasks from cycle queries")
  void shouldExcludeDeletedTasksFromCycleQueries() {
    // Given
    when(taskRepository.findByCycleIdNotDeleted(1L)).thenReturn(java.util.List.of());

    // When
    var result = taskService.getTasksByCycleId(1L);

    // Then
    assertThat(result).isEmpty();
    verify(taskRepository).findByCycleIdNotDeleted(1L);
  }
}
