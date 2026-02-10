package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.farzadsedaghatbin.shipflow.dto.CreateTaskRequest;
import com.github.farzadsedaghatbin.shipflow.dto.TaskDTO;
import com.github.farzadsedaghatbin.shipflow.dto.TaskStatisticsDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Person;
import com.github.farzadsedaghatbin.shipflow.entity.Task;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.UserRole;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CyclePhase;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskCategory;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskPriority;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskStatus;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PersonRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TaskRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TaskServiceTest {

  @Autowired
  private TaskRepository taskRepository;

  @Autowired
  private CycleRepository cycleRepository;

  @Autowired
  private PersonRepository personRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private MessageService messageService;

  @Autowired
  private TaskService taskService;

  private Task testTask;
  private Cycle testCycle;
  private Person testPerson;
  private Person testPairPerson;
  private User testUser;
  private CreateTaskRequest testRequest;

  @BeforeEach
  void setUp() {
    // Clean up any existing data
    taskRepository.deleteAll();
    cycleRepository.deleteAll();
    personRepository.deleteAll();
    userRepository.deleteAll();

    // Create real test data
    testCycle = Cycle.builder()
        .name("Test Cycle")
        .startDate(LocalDate.now())
        .endDate(LocalDate.now().plusWeeks(6))
        .phase(CyclePhase.BUILD)
        .isActive(true)
        .build();
    testCycle = cycleRepository.save(testCycle);

    testPerson = Person.builder()
        .name("John Doe")
        .email("john.tasktest@example.com")
        .isActive(true)
        .createdAt(LocalDateTime.now())
        .build();
    testPerson = personRepository.save(testPerson);

    testPairPerson = Person.builder()
        .name("Jane Smith")
        .email("jane.tasktest@example.com")
        .isActive(true)
        .createdAt(LocalDateTime.now())
        .build();
    testPairPerson = personRepository.save(testPairPerson);

    // Create test user for security context
    testUser = User.builder()
        .username("testuserTask")
        .email("testuserTask@example.com")
        .password("testpassword")
        .role(UserRole.MEMBER)
        .person(testPerson)
        .isActive(true)
        .build();
    testUser = userRepository.save(testUser);

    // Set up security context
    UsernamePasswordAuthenticationToken auth = 
        new UsernamePasswordAuthenticationToken("testuserTask", "testpassword");
    SecurityContextHolder.getContext().setAuthentication(auth);

    testPairPerson = Person.builder()
        .name("Jane Smith")
        .email("jane@example.com")
        .isActive(true)
        .createdAt(LocalDateTime.now())
        .build();
    testPairPerson = personRepository.save(testPairPerson);

    testTask = Task.builder()
        .title("Test Task")
        .description("Test task description")
        .status(TaskStatus.TODO)
        .priority(TaskPriority.MEDIUM)
        .category(TaskCategory.PITCH_SCOPE)
        .estimateHours(BigDecimal.valueOf(4.0))
        .cycle(testCycle)
        .assignee(testPerson)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
    testTask = taskRepository.save(testTask);

    testRequest = new CreateTaskRequest();
    testRequest.setTitle("New Task");
    testRequest.setDescription("New task description");
    testRequest.setCycleId(testCycle.getId());
    testRequest.setStatus(TaskStatus.TODO);
    testRequest.setPriority(TaskPriority.HIGH);
    testRequest.setEstimateHours(BigDecimal.valueOf(8.0));
    testRequest.setAssigneeId(testPerson.getId());
  }

  @Test
  void getAllTasks_ShouldReturnAllTasks() {
    List<TaskDTO> result = taskService.getAllTasks();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getTitle()).isEqualTo("Test Task");
  }

  @Test
  void getTaskById_WhenExists_ShouldReturnTask() {
    TaskDTO result = taskService.getTaskById(testTask.getId());

    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(testTask.getId());
    assertThat(result.getTitle()).isEqualTo("Test Task");
  }

  @Test
  void getTaskById_WhenNotExists_ShouldThrowException() {
    assertThatThrownBy(() -> taskService.getTaskById(999L))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Task not found");
  }

  @Test
  void getTasksByCycleId_ShouldReturnTasksForCycle() {
    List<TaskDTO> result = taskService.getTasksByCycleId(testCycle.getId());

    assertThat(result).hasSize(1);
  }

  @Test
  void getTasksByCycleIdAndStatus_ShouldReturnFilteredTasks() {
    List<TaskDTO> result = taskService.getTasksByCycleIdAndStatus(testCycle.getId(), TaskStatus.TODO);

    assertThat(result).hasSize(1);
  }

  @Test
  void createTask_ShouldSaveTask() {
    TaskDTO result = taskService.createTask(testRequest);

    assertThat(result).isNotNull();
    assertThat(result.getTitle()).isEqualTo(testRequest.getTitle());
    assertThat(result.getCycleId()).isEqualTo(testCycle.getId());
    assertThat(result.getAssigneeId()).isEqualTo(testPerson.getId());
  }

  @Test
  void createTask_WithPairAssignee_ShouldSaveTaskWithBothAssignees() {
    testRequest.setPairAssigneeId(testPairPerson.getId());

    TaskDTO result = taskService.createTask(testRequest);

    assertThat(result).isNotNull();
    assertThat(result.getAssigneeId()).isEqualTo(testPerson.getId());
    assertThat(result.getPairAssigneeId()).isEqualTo(testPairPerson.getId());
  }

  @Test
  void createTask_WithInvalidCycle_ShouldThrowException() {
    testRequest.setCycleId(999L);

    assertThatThrownBy(() -> taskService.createTask(testRequest)).isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Cycle not found");
  }

  @Test
  void updateTask_WhenExists_ShouldUpdateTask() {
    testRequest.setTitle("Updated Task");
    TaskDTO result = taskService.updateTask(testTask.getId(), testRequest);

    assertThat(result).isNotNull();
    assertThat(result.getTitle()).isEqualTo("Updated Task");
  }

  @Test
  void updateTaskStatus_ShouldUpdateStatus() {
    TaskDTO result = taskService.updateTaskStatus(testTask.getId(), TaskStatus.IN_PROGRESS);

    assertThat(result).isNotNull();
    assertThat(result.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
  }

  @Test
  void updateTaskStatus_ToDone_ShouldSetCompletedAt() {
    TaskDTO result = taskService.updateTaskStatus(testTask.getId(), TaskStatus.DONE);

    assertThat(result).isNotNull();
    assertThat(result.getStatus()).isEqualTo(TaskStatus.DONE);
    // Verify the task was actually updated in the database
    Task updatedTask = taskRepository.findById(testTask.getId()).orElseThrow();
    assertThat(updatedTask.getCompletedAt()).isNotNull();
  }

  @Test
  void deleteTask_WhenExists_ShouldDelete() {
    Long taskId = testTask.getId();
    
    taskService.deleteTask(taskId);

    // Verify soft delete - task still exists in DB but is marked as deleted
    assertThat(taskRepository.existsById(taskId)).isTrue(); // Task still exists in database
    assertThat(taskRepository.findByIdNotDeleted(taskId)).isEmpty(); // But not found by soft-delete-aware query
  }

  @Test
  void deleteTask_WhenNotExists_ShouldThrowException() {
    assertThatThrownBy(() -> taskService.deleteTask(999L)).isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Task not found");
  }

  @Test
  void getTaskStatisticsByCycleId_ShouldReturnStatistics() {
    // Create additional tasks with different statuses for testing
    Task todoTask = Task.builder()
        .title("Todo Task")
        .description("Todo task")
        .status(TaskStatus.TODO)
        .priority(TaskPriority.LOW)
        .category(TaskCategory.PITCH_SCOPE)
        .estimateHours(BigDecimal.valueOf(2.0))
        .cycle(testCycle)
        .assignee(testPerson)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
    taskRepository.save(todoTask);

    Task doneTask = Task.builder()
        .title("Done Task")
        .description("Done task")
        .status(TaskStatus.DONE)
        .priority(TaskPriority.HIGH)
        .category(TaskCategory.PITCH_SCOPE)
        .estimateHours(BigDecimal.valueOf(6.0))
        .actualHours(BigDecimal.valueOf(5.0))
        .cycle(testCycle)
        .assignee(testPerson)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .completedAt(LocalDateTime.now())
        .build();
    taskRepository.save(doneTask);

    TaskStatisticsDTO result = taskService.getTaskStatisticsByCycleId(testCycle.getId());

    assertThat(result).isNotNull();
    assertThat(result.getTotalTasks()).isEqualTo(3); // testTask + todoTask + doneTask
    assertThat(result.getDoneTasks()).isEqualTo(1);
    assertThat(result.getTodoTasks()).isEqualTo(2); // testTask (TODO) + todoTask (TODO)
  }

  @Test
  void getTasksByAssigneeId_ShouldReturnTasksForAssignee() {
    List<TaskDTO> result = taskService.getTasksByAssigneeId(testPerson.getId());

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getAssigneeId()).isEqualTo(testPerson.getId());
  }

  @Test
  void getTasksByPersonId_ShouldReturnTasksForPerson() {
    List<TaskDTO> result = taskService.getTasksByPersonId(testPerson.getId());

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getId()).isEqualTo(testTask.getId());
  }

  // ========== Category Tests ==========

  @Test
  void createTask_WithCategory_ShouldSaveTaskWithCategory() {
    testRequest.setCategory(TaskCategory.DEBT_IMPROVEMENT);

    TaskDTO result = taskService.createTask(testRequest);

    assertThat(result).isNotNull();
    assertThat(result.getCategory()).isEqualTo(TaskCategory.DEBT_IMPROVEMENT);
  }

  @Test
  void createTask_WithoutCategory_ShouldDefaultToPitchScope() {
    testRequest.setCategory(null);

    TaskDTO result = taskService.createTask(testRequest);

    assertThat(result).isNotNull();
    assertThat(result.getCategory()).isEqualTo(TaskCategory.PITCH_SCOPE);
  }

  @Test
  void updateTask_ShouldUpdateCategory() {
    testRequest.setCategory(TaskCategory.DEBT_IMPROVEMENT);

    TaskDTO result = taskService.updateTask(testTask.getId(), testRequest);

    assertThat(result).isNotNull();
    assertThat(result.getCategory()).isEqualTo(TaskCategory.DEBT_IMPROVEMENT);
  }

  @Test
  void getTasksByCycleIdAndCategory_ShouldReturnFilteredTasks() {
    List<TaskDTO> result = taskService.getTasksByCycleIdAndCategory(testCycle.getId(), TaskCategory.PITCH_SCOPE);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getCategory()).isEqualTo(TaskCategory.PITCH_SCOPE);
  }

  @Test
  void countTasksByCycleIdAndCategory_ShouldReturnCount() {
    // Create additional tasks with DEBT_IMPROVEMENT category for testing
    Task debtTask1 = Task.builder()
        .title("Debt Task 1")
        .description("Debt improvement task 1")
        .status(TaskStatus.TODO)
        .priority(TaskPriority.LOW)
        .category(TaskCategory.DEBT_IMPROVEMENT)
        .estimateHours(BigDecimal.valueOf(2.0))
        .cycle(testCycle)
        .assignee(testPerson)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
    taskRepository.save(debtTask1);

    Task debtTask2 = Task.builder()
        .title("Debt Task 2")
        .description("Debt improvement task 2")
        .status(TaskStatus.DONE)
        .priority(TaskPriority.HIGH)
        .category(TaskCategory.DEBT_IMPROVEMENT)
        .estimateHours(BigDecimal.valueOf(4.0))
        .cycle(testCycle)
        .assignee(testPerson)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
    taskRepository.save(debtTask2);

    int result = taskService.countTasksByCycleIdAndCategory(testCycle.getId(), TaskCategory.DEBT_IMPROVEMENT);

    assertThat(result).isEqualTo(2);
  }

  @Test
  void toDTO_ShouldIncludeCategory() {
    TaskDTO result = taskService.getTaskById(testTask.getId());

    assertThat(result.getCategory()).isEqualTo(TaskCategory.PITCH_SCOPE);
  }

  @Test
  void createTask_WithPitchAndNoParent_ShouldAutoCreateLinkedScope() {
    // Given: A root task with a pitch
    CreateTaskRequest request = CreateTaskRequest.builder()
        .title("Auto-Scope Task")
        .description("This should create a scope")
        .cycleId(testCycle.getId())
        .pitchId(testTask.getPitch().getId())
        .assigneeId(testPerson.getId())
        .status(TaskStatus.TODO)
        .priority(TaskPriority.MEDIUM)
        .category(TaskCategory.PITCH_SCOPE)
        .estimateHours(BigDecimal.valueOf(8.0))
        .build();

    // When: Creating the task
    TaskDTO result = taskService.createTask(request);

    // Then: Should have an auto-created scope ID
    assertThat(result.getAutoCreatedScopeId()).isNotNull();
    assertThat(result.getShowOnHillChart()).isTrue();
  }

  @Test
  void createTask_WithParentTask_ShouldNotAutoCreateScope() {
    // Given: A subtask with a parent
    CreateTaskRequest request = CreateTaskRequest.builder()
        .title("Subtask")
        .description("This should NOT create a scope")
        .cycleId(testCycle.getId())
        .pitchId(testTask.getPitch().getId())
        .parentTaskId(testTask.getId())
        .assigneeId(testPerson.getId())
        .status(TaskStatus.TODO)
        .priority(TaskPriority.MEDIUM)
        .category(TaskCategory.PITCH_SCOPE)
        .estimateHours(BigDecimal.valueOf(4.0))
        .build();

    // When: Creating the subtask
    TaskDTO result = taskService.createTask(request);

    // Then: Should NOT have an auto-created scope
    assertThat(result.getAutoCreatedScopeId()).isNull();
    assertThat(result.getShowOnHillChart()).isFalse();
  }

  @Test
  void createTask_WithoutPitch_ShouldNotAutoCreateScope() {
    // Given: A task without a pitch (technical debt)
    CreateTaskRequest request = CreateTaskRequest.builder()
        .title("Tech Debt Task")
        .description("No pitch, so no scope")
        .cycleId(testCycle.getId())
        .assigneeId(testPerson.getId())
        .status(TaskStatus.TODO)
        .priority(TaskPriority.HIGH)
        .category(TaskCategory.DEBT_IMPROVEMENT)
        .estimateHours(BigDecimal.valueOf(6.0))
        .build();

    // When: Creating the task
    TaskDTO result = taskService.createTask(request);

    // Then: Should NOT have an auto-created scope
    assertThat(result.getAutoCreatedScopeId()).isNull();
    assertThat(result.getShowOnHillChart()).isFalse();
  }
}
