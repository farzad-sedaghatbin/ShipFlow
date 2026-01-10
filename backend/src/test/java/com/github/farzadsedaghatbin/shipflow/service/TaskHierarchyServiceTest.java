package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.CreateTaskRequest;
import com.github.farzadsedaghatbin.shipflow.dto.TaskDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.entity.Task;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskCategory;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskPriority;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskStatus;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PersonRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TaskRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskHierarchyServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private CycleRepository cycleRepository;

    @Mock
    private PersonRepository personRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DashboardNotificationService notificationService;

    @InjectMocks
    private TaskService taskService;

    private Cycle cycle;
    private Task parentTask;
    private Task childTask;
    private CreateTaskRequest createRequest;

    @BeforeEach
    void setUp() {
        Project project = Project.builder()
                .id(1L)
                .name("Test Project")
                .projectKey("TEST")
                .build();

        cycle = Cycle.builder()
                .id(1L)
                .name("Sprint 1")
                .project(project)
                .build();

        parentTask = Task.builder()
                .id(1L)
                .title("Parent Task")
                .cycle(cycle)
                .status(TaskStatus.IN_PROGRESS)
                .priority(TaskPriority.HIGH)
                .category(TaskCategory.PITCH_SCOPE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        childTask = Task.builder()
                .id(2L)
                .title("Child Task")
                .cycle(cycle)
                .parentTask(parentTask)
                .status(TaskStatus.BACKLOG)
                .priority(TaskPriority.MEDIUM)
                .category(TaskCategory.PITCH_SCOPE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        createRequest = CreateTaskRequest.builder()
                .title("New Task")
                .description("Test task description")
                .cycleId(1L)
                .status(TaskStatus.BACKLOG)
                .priority(TaskPriority.MEDIUM)
                .category(TaskCategory.PITCH_SCOPE)
                .build();
    }

    @Test
    void shouldCreateTaskWithParent() {
        // Given
        createRequest.setParentTaskId(1L);
        when(cycleRepository.findById(1L)).thenReturn(Optional.of(cycle));
        when(taskRepository.findById(1L)).thenReturn(Optional.of(parentTask));
        
        Task savedTask = Task.builder()
                .id(3L)
                .title("New Task")
                .cycle(cycle)
                .parentTask(parentTask)
                .status(TaskStatus.BACKLOG)
                .priority(TaskPriority.MEDIUM)
                .category(TaskCategory.PITCH_SCOPE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        when(taskRepository.save(any(Task.class))).thenReturn(savedTask);

        // When
        TaskDTO result = taskService.createTask(createRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getParentTaskId()).isEqualTo(1L);
        assertThat(result.getParentTaskTitle()).isEqualTo("Parent Task");
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    void shouldFailToCreateTaskWithNonExistentParent() {
        // Given
        createRequest.setParentTaskId(999L);
        when(cycleRepository.findById(1L)).thenReturn(Optional.of(cycle));
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> taskService.createTask(createRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Parent task not found");
    }

    @Test
    void shouldFailToCreateTaskWithParentFromDifferentCycle() {
        // Given
        Cycle differentCycle = Cycle.builder()
                .id(2L)
                .name("Sprint 2")
                .build();
        
        Task parentFromDifferentCycle = Task.builder()
                .id(1L)
                .title("Parent Task")
                .cycle(differentCycle)
                .build();
        
        createRequest.setParentTaskId(1L);
        when(cycleRepository.findById(1L)).thenReturn(Optional.of(cycle));
        when(taskRepository.findById(1L)).thenReturn(Optional.of(parentFromDifferentCycle));

        // When / Then
        assertThatThrownBy(() -> taskService.createTask(createRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Parent task must belong to the same cycle");
    }

    @Test
    void shouldGetSubTasks() {
        // Given
        Task child1 = Task.builder()
                .id(2L)
                .title("Child 1")
                .cycle(cycle)
                .parentTask(parentTask)
                .status(TaskStatus.BACKLOG)
                .priority(TaskPriority.MEDIUM)
                .category(TaskCategory.PITCH_SCOPE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        Task child2 = Task.builder()
                .id(3L)
                .title("Child 2")
                .cycle(cycle)
                .parentTask(parentTask)
                .status(TaskStatus.IN_PROGRESS)
                .priority(TaskPriority.HIGH)
                .category(TaskCategory.PITCH_SCOPE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        when(taskRepository.findByParentTaskId(1L)).thenReturn(Arrays.asList(child1, child2));

        // When
        List<TaskDTO> result = taskService.getSubTasks(1L);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTitle()).isEqualTo("Child 1");
        assertThat(result.get(1).getTitle()).isEqualTo("Child 2");
        assertThat(result.get(0).getParentTaskId()).isEqualTo(1L);
        assertThat(result.get(1).getParentTaskId()).isEqualTo(1L);
    }

    @Test
    void shouldGetRootTasks() {
        // Given
        Task root1 = Task.builder()
                .id(1L)
                .title("Root Task 1")
                .cycle(cycle)
                .parentTask(null)
                .status(TaskStatus.BACKLOG)
                .priority(TaskPriority.HIGH)
                .category(TaskCategory.PITCH_SCOPE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        Task root2 = Task.builder()
                .id(2L)
                .title("Root Task 2")
                .cycle(cycle)
                .parentTask(null)
                .status(TaskStatus.IN_PROGRESS)
                .priority(TaskPriority.MEDIUM)
                .category(TaskCategory.PITCH_SCOPE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        when(taskRepository.findRootTasksByCycleId(1L)).thenReturn(Arrays.asList(root1, root2));

        // When
        List<TaskDTO> result = taskService.getRootTasksByCycleId(1L);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTitle()).isEqualTo("Root Task 1");
        assertThat(result.get(1).getTitle()).isEqualTo("Root Task 2");
        assertThat(result.get(0).getParentTaskId()).isNull();
        assertThat(result.get(1).getParentTaskId()).isNull();
    }

    @Test
    void shouldGetTaskTreeWithNestedChildren() {
        // Given
        Task grandChild = Task.builder()
                .id(3L)
                .title("Grandchild Task")
                .cycle(cycle)
                .parentTask(childTask)
                .status(TaskStatus.BACKLOG)
                .priority(TaskPriority.LOW)
                .category(TaskCategory.PITCH_SCOPE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        childTask.setChildren(Arrays.asList(grandChild));
        parentTask.setChildren(Arrays.asList(childTask));
        
        when(taskRepository.findRootTasksByCycleId(1L)).thenReturn(Arrays.asList(parentTask));

        // When
        List<TaskDTO> result = taskService.getTaskTreeByCycleId(1L);

        // Then
        assertThat(result).hasSize(1);
        TaskDTO root = result.get(0);
        assertThat(root.getTitle()).isEqualTo("Parent Task");
        assertThat(root.getChildren()).hasSize(1);
        
        TaskDTO child = root.getChildren().get(0);
        assertThat(child.getTitle()).isEqualTo("Child Task");
        assertThat(child.getChildren()).hasSize(1);
        
        TaskDTO grandchild = child.getChildren().get(0);
        assertThat(grandchild.getTitle()).isEqualTo("Grandchild Task");
    }

    @Test
    void shouldPreventCircularReferenceWhenUpdating() {
        // Given
        Task taskA = Task.builder()
                .id(1L)
                .title("Task A")
                .cycle(cycle)
                .status(TaskStatus.BACKLOG)
                .priority(TaskPriority.MEDIUM)
                .category(TaskCategory.PITCH_SCOPE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        Task taskB = Task.builder()
                .id(2L)
                .title("Task B")
                .cycle(cycle)
                .parentTask(taskA)
                .status(TaskStatus.BACKLOG)
                .priority(TaskPriority.MEDIUM)
                .category(TaskCategory.PITCH_SCOPE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        // Try to make taskA a child of taskB (would create circular reference)
        createRequest.setParentTaskId(2L);
        
        when(taskRepository.findById(1L)).thenReturn(Optional.of(taskA));
        when(taskRepository.findById(2L)).thenReturn(Optional.of(taskB));

        // When / Then
        assertThatThrownBy(() -> taskService.updateTask(1L, createRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("circular reference");
    }

    @Test
    void shouldPreventTaskFromBeingItsOwnParent() {
        // Given
        createRequest.setParentTaskId(1L);  // Try to set task as its own parent
        
        when(taskRepository.findById(1L)).thenReturn(Optional.of(parentTask));

        // When / Then
        assertThatThrownBy(() -> taskService.updateTask(1L, createRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("circular reference");
    }

    @Test
    void shouldUpdateTaskParent() {
        // Given
        Task newParent = Task.builder()
                .id(10L)
                .title("New Parent Task")
                .cycle(cycle)
                .status(TaskStatus.IN_PROGRESS)
                .priority(TaskPriority.HIGH)
                .category(TaskCategory.PITCH_SCOPE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        createRequest.setParentTaskId(10L);
        
        when(taskRepository.findById(2L)).thenReturn(Optional.of(childTask));
        when(taskRepository.findById(10L)).thenReturn(Optional.of(newParent));
        when(taskRepository.save(any(Task.class))).thenReturn(childTask);

        // When
        TaskDTO result = taskService.updateTask(2L, createRequest);

        // Then
        assertThat(result).isNotNull();
        verify(taskRepository).save(argThat(task -> 
            task.getParentTask() != null && task.getParentTask().getId().equals(10L)
        ));
    }

    @Test
    void shouldRemoveParentWhenUpdatedWithNullParent() {
        // Given
        createRequest.setParentTaskId(null);  // Remove parent
        
        when(taskRepository.findById(2L)).thenReturn(Optional.of(childTask));
        when(taskRepository.save(any(Task.class))).thenReturn(childTask);

        // When
        TaskDTO result = taskService.updateTask(2L, createRequest);

        // Then
        assertThat(result).isNotNull();
        verify(taskRepository).save(argThat(task -> task.getParentTask() == null));
    }
}
