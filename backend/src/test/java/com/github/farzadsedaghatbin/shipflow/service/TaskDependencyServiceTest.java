package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.CreateTaskDependencyRequest;
import com.github.farzadsedaghatbin.shipflow.dto.TaskDependencyDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Task;
import com.github.farzadsedaghatbin.shipflow.entity.TaskDependency;
import com.github.farzadsedaghatbin.shipflow.entity.enums.DependencyType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskPriority;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskStatus;
import com.github.farzadsedaghatbin.shipflow.exception.BadRequestException;
import com.github.farzadsedaghatbin.shipflow.repository.TaskDependencyRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskDependencyServiceTest {

    @Mock
    private TaskDependencyRepository taskDependencyRepository;

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskDependencyService taskDependencyService;

    private Cycle testCycle;
    private Task sourceTask;
    private Task targetTask;
    private Task anotherTask;
    private TaskDependency testDependency;

    @BeforeEach
    void setUp() {
        testCycle = Cycle.builder()
                .id(1L)
                .name("Test Cycle")
                .build();

        sourceTask = Task.builder()
                .id(1L)
                .title("Source Task")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.MEDIUM)
                .cycle(testCycle)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        targetTask = Task.builder()
                .id(2L)
                .title("Target Task")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.MEDIUM)
                .cycle(testCycle)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        anotherTask = Task.builder()
                .id(3L)
                .title("Another Task")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.MEDIUM)
                .cycle(testCycle)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testDependency = TaskDependency.builder()
                .id(1L)
                .sourceTask(sourceTask)
                .targetTask(targetTask)
                .dependencyType(DependencyType.BLOCKS)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void addDependency_Success() {
        // Given
        CreateTaskDependencyRequest request = CreateTaskDependencyRequest.builder()
                .targetTaskId(2L)
                .dependencyType(DependencyType.BLOCKS)
                .build();

        when(taskRepository.findById(1L)).thenReturn(Optional.of(sourceTask));
        when(taskRepository.findById(2L)).thenReturn(Optional.of(targetTask));
        when(taskDependencyRepository.findBySourceTaskIdAndTargetTaskId(1L, 2L))
                .thenReturn(Optional.empty());
        when(taskDependencyRepository.save(any(TaskDependency.class))).thenReturn(testDependency);

        // When
        TaskDependencyDTO result = taskDependencyService.addDependency(1L, request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getSourceTaskId()).isEqualTo(1L);
        assertThat(result.getTargetTaskId()).isEqualTo(2L);
        assertThat(result.getDependencyType()).isEqualTo(DependencyType.BLOCKS);
        verify(taskDependencyRepository).save(any(TaskDependency.class));
    }

    @Test
    void addDependency_SourceTaskNotFound_ThrowsException() {
        // Given
        CreateTaskDependencyRequest request = CreateTaskDependencyRequest.builder()
                .targetTaskId(2L)
                .dependencyType(DependencyType.BLOCKS)
                .build();

        when(taskRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> taskDependencyService.addDependency(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Source task not found");
    }

    @Test
    void addDependency_TargetTaskNotFound_ThrowsException() {
        // Given
        CreateTaskDependencyRequest request = CreateTaskDependencyRequest.builder()
                .targetTaskId(999L)
                .dependencyType(DependencyType.BLOCKS)
                .build();

        when(taskRepository.findById(1L)).thenReturn(Optional.of(sourceTask));
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> taskDependencyService.addDependency(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Target task not found");
    }

    @Test
    void addDependency_TasksInDifferentCycles_ThrowsException() {
        // Given
        Cycle differentCycle = Cycle.builder().id(2L).name("Different Cycle").build();
        targetTask.setCycle(differentCycle);

        CreateTaskDependencyRequest request = CreateTaskDependencyRequest.builder()
                .targetTaskId(2L)
                .dependencyType(DependencyType.BLOCKS)
                .build();

        when(taskRepository.findById(1L)).thenReturn(Optional.of(sourceTask));
        when(taskRepository.findById(2L)).thenReturn(Optional.of(targetTask));

        // When & Then
        assertThatThrownBy(() -> taskDependencyService.addDependency(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("same cycle");
    }

    @Test
    void addDependency_SelfDependency_ThrowsException() {
        // Given
        CreateTaskDependencyRequest request = CreateTaskDependencyRequest.builder()
                .targetTaskId(1L)
                .dependencyType(DependencyType.BLOCKS)
                .build();

        when(taskRepository.findById(1L)).thenReturn(Optional.of(sourceTask));

        // When & Then
        assertThatThrownBy(() -> taskDependencyService.addDependency(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cannot depend on itself");
    }

    @Test
    void addDependency_DuplicateDependency_ThrowsException() {
        // Given
        CreateTaskDependencyRequest request = CreateTaskDependencyRequest.builder()
                .targetTaskId(2L)
                .dependencyType(DependencyType.BLOCKS)
                .build();

        when(taskRepository.findById(1L)).thenReturn(Optional.of(sourceTask));
        when(taskRepository.findById(2L)).thenReturn(Optional.of(targetTask));
        when(taskDependencyRepository.findBySourceTaskIdAndTargetTaskId(1L, 2L))
                .thenReturn(Optional.of(testDependency));

        // When & Then
        assertThatThrownBy(() -> taskDependencyService.addDependency(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void addDependency_CircularDependency_ThrowsException() {
        // Given - Create a scenario where target already depends on source
        CreateTaskDependencyRequest request = CreateTaskDependencyRequest.builder()
                .targetTaskId(2L)
                .dependencyType(DependencyType.BLOCKS)
                .build();

        TaskDependency existingDependency = TaskDependency.builder()
                .id(2L)
                .sourceTask(targetTask)
                .targetTask(sourceTask)
                .dependencyType(DependencyType.BLOCKS)
                .build();

        when(taskRepository.findById(1L)).thenReturn(Optional.of(sourceTask));
        when(taskRepository.findById(2L)).thenReturn(Optional.of(targetTask));
        when(taskDependencyRepository.findBySourceTaskIdAndTargetTaskId(1L, 2L))
                .thenReturn(Optional.empty());
        when(taskDependencyRepository.findBySourceTaskId(2L))
                .thenReturn(Arrays.asList(existingDependency));

        // When & Then
        assertThatThrownBy(() -> taskDependencyService.addDependency(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("circular reference");
    }

    @Test
    void addDependency_DefaultDependencyType() {
        // Given
        CreateTaskDependencyRequest request = CreateTaskDependencyRequest.builder()
                .targetTaskId(2L)
                .build(); // No dependency type specified

        when(taskRepository.findById(1L)).thenReturn(Optional.of(sourceTask));
        when(taskRepository.findById(2L)).thenReturn(Optional.of(targetTask));
        when(taskDependencyRepository.findBySourceTaskIdAndTargetTaskId(1L, 2L))
                .thenReturn(Optional.empty());
        when(taskDependencyRepository.save(any(TaskDependency.class))).thenReturn(testDependency);

        // When
        TaskDependencyDTO result = taskDependencyService.addDependency(1L, request);

        // Then
        assertThat(result.getDependencyType()).isEqualTo(DependencyType.BLOCKS);
    }

    @Test
    void removeDependency_Success() {
        // Given
        when(taskDependencyRepository.existsById(1L)).thenReturn(true);

        // When
        taskDependencyService.removeDependency(1L);

        // Then
        verify(taskDependencyRepository).deleteById(1L);
    }

    @Test
    void removeDependency_NotFound_ThrowsException() {
        // Given
        when(taskDependencyRepository.existsById(999L)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> taskDependencyService.removeDependency(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void getDependenciesForTask_Success() {
        // Given
        TaskDependency outgoingDep = TaskDependency.builder()
                .id(1L)
                .sourceTask(sourceTask)
                .targetTask(targetTask)
                .dependencyType(DependencyType.BLOCKS)
                .createdAt(LocalDateTime.now())
                .build();

        TaskDependency incomingDep = TaskDependency.builder()
                .id(2L)
                .sourceTask(anotherTask)
                .targetTask(sourceTask)
                .dependencyType(DependencyType.BLOCKS)
                .createdAt(LocalDateTime.now())
                .build();

        when(taskRepository.existsById(1L)).thenReturn(true);
        when(taskDependencyRepository.findBySourceTaskId(1L))
                .thenReturn(Arrays.asList(outgoingDep));
        when(taskDependencyRepository.findByTargetTaskId(1L))
                .thenReturn(Arrays.asList(incomingDep));

        // When
        Map<String, List<TaskDependencyDTO>> result = taskDependencyService.getDependenciesForTask(1L);

        // Then
        assertThat(result).containsKeys("blocking", "blockedBy");
        assertThat(result.get("blocking")).hasSize(1);
        assertThat(result.get("blockedBy")).hasSize(1);
        assertThat(result.get("blocking").get(0).getTargetTaskId()).isEqualTo(2L);
        assertThat(result.get("blockedBy").get(0).getSourceTaskId()).isEqualTo(3L);
    }

    @Test
    void getDependenciesForTask_TaskNotFound_ThrowsException() {
        // Given
        when(taskRepository.existsById(999L)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> taskDependencyService.getDependenciesForTask(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void getBlockingDependencies_Success() {
        // Given
        when(taskRepository.existsById(1L)).thenReturn(true);
        when(taskDependencyRepository.findBlockingDependenciesByTaskId(1L))
                .thenReturn(Arrays.asList(testDependency));

        // When
        List<TaskDependencyDTO> result = taskDependencyService.getBlockingDependencies(1L);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSourceTaskId()).isEqualTo(1L);
    }

    @Test
    void getBlockedByDependencies_Success() {
        // Given
        TaskDependency blockingDep = TaskDependency.builder()
                .id(2L)
                .sourceTask(targetTask)
                .targetTask(sourceTask)
                .dependencyType(DependencyType.BLOCKS)
                .createdAt(LocalDateTime.now())
                .build();

        when(taskRepository.existsById(1L)).thenReturn(true);
        when(taskDependencyRepository.findBlockedByDependenciesByTaskId(1L))
                .thenReturn(Arrays.asList(blockingDep));

        // When
        List<TaskDependencyDTO> result = taskDependencyService.getBlockedByDependencies(1L);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTargetTaskId()).isEqualTo(1L);
    }

    @Test
    void addDependency_ComplexCircularDependency_ThrowsException() {
        // Given - Task1 -> Task2 -> Task3, trying to add Task3 -> Task1
        Task task3 = Task.builder()
                .id(3L)
                .title("Task 3")
                .cycle(testCycle)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        TaskDependency dep1to2 = TaskDependency.builder()
                .sourceTask(sourceTask)
                .targetTask(targetTask)
                .build();

        TaskDependency dep2to3 = TaskDependency.builder()
                .sourceTask(targetTask)
                .targetTask(task3)
                .build();

        CreateTaskDependencyRequest request = CreateTaskDependencyRequest.builder()
                .targetTaskId(1L)
                .dependencyType(DependencyType.BLOCKS)
                .build();

        when(taskRepository.findById(3L)).thenReturn(Optional.of(task3));
        when(taskRepository.findById(1L)).thenReturn(Optional.of(sourceTask));
        when(taskDependencyRepository.findBySourceTaskIdAndTargetTaskId(3L, 1L))
                .thenReturn(Optional.empty());
        when(taskDependencyRepository.findBySourceTaskId(1L))
                .thenReturn(Arrays.asList(dep1to2));
        when(taskDependencyRepository.findBySourceTaskId(2L))
                .thenReturn(Arrays.asList(dep2to3));

        // When & Then
        assertThatThrownBy(() -> taskDependencyService.addDependency(3L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("circular reference");
    }
}
