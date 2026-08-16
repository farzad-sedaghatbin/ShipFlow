package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.farzadsedaghatbin.shipflow.dto.BulkCreateTaskRequest;
import com.github.farzadsedaghatbin.shipflow.dto.BulkCreateTaskResult;
import com.github.farzadsedaghatbin.shipflow.dto.BulkTaskUpdateRequest;
import com.github.farzadsedaghatbin.shipflow.dto.BulkUpdateResult;
import com.github.farzadsedaghatbin.shipflow.dto.CreateTaskRequest;
import com.github.farzadsedaghatbin.shipflow.dto.TaskDTO;
import com.github.farzadsedaghatbin.shipflow.dto.TaskStatisticsDTO;
import com.github.farzadsedaghatbin.shipflow.dto.pitch.TaskSuggestionDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Epic;
import com.github.farzadsedaghatbin.shipflow.entity.Person;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.entity.Release;
import com.github.farzadsedaghatbin.shipflow.entity.Task;
import com.github.farzadsedaghatbin.shipflow.entity.Team;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.UserRole;
import com.github.farzadsedaghatbin.shipflow.entity.enums.BulkAction;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CyclePhase;
import com.github.farzadsedaghatbin.shipflow.entity.enums.Discipline;
import com.github.farzadsedaghatbin.shipflow.entity.enums.EpicStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PitchStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ProjectType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ReleaseStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.SuggestionSource;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskCategory;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskPriority;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskStatus;
import com.github.farzadsedaghatbin.shipflow.exception.BadRequestException;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.repository.EpicRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PersonRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
import com.github.farzadsedaghatbin.shipflow.repository.ProjectRepository;
import com.github.farzadsedaghatbin.shipflow.repository.ReleaseRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TaskRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TeamRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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
  private PitchRepository pitchRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private TeamRepository teamRepository;

  @Autowired
  private ProjectRepository projectRepository;

  @Autowired
  private ReleaseRepository releaseRepository;

  @Autowired
  private EpicRepository epicRepository;

  @Autowired
  private TaskService taskService;

  private Task testTask;
  private Cycle testCycle;
  private Pitch testPitch;
  private Person testPerson;
  private Person testPairPerson;
  private User testUser;
  private CreateTaskRequest testRequest;

  @BeforeEach
  void setUp() {
    // Clean up any existing data
    taskRepository.deleteAll();
    pitchRepository.deleteAll();
    cycleRepository.deleteAll();
    personRepository.deleteAll();
    userRepository.deleteAll();

    // Create real test data
    testCycle = Cycle.builder()
        .name("Test Cycle")
        .startDate(LocalDate.now())
        .endDate(LocalDate.now().plusWeeks(6))
        .phase(CyclePhase.SHAPING_BUILDING)
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

    // Create test pitch for scope auto-creation tests
    testPitch = Pitch.builder()
        .title("Test Pitch")
        .description("Test pitch for auto-scope creation")
        .cycle(testCycle)
        .status(PitchStatus.IN_PROGRESS)
        .appetiteDays(14)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
    testPitch = pitchRepository.save(testPitch);

    testTask = Task.builder()
        .title("Test Task")
        .description("Test task description")
        .status(TaskStatus.TODO)
        .priority(TaskPriority.MEDIUM)
        .category(TaskCategory.PITCH_SCOPE)
        .estimateHours(BigDecimal.valueOf(4.0))
        .cycle(testCycle)
        .pitch(testPitch)
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
  void createTask_DebtImprovementWithoutCycleInShapeUpProject_ShouldCreateBacklogTask() {
    Project project = Project.builder().name("Shape Up Project").projectKey("SUP1")
        .projectType(ProjectType.SHAPE_UP).isActive(true).build();
    project = projectRepository.save(project);

    CreateTaskRequest request = new CreateTaskRequest();
    request.setTitle("Opportunistic cleanup");
    request.setProjectId(project.getId());
    request.setCategory(TaskCategory.DEBT_IMPROVEMENT);

    TaskDTO result = taskService.createTask(request);

    assertThat(result).isNotNull();
    assertThat(result.getCycleId()).isNull();
    assertThat(result.getCategory()).isEqualTo(TaskCategory.DEBT_IMPROVEMENT);
  }

  @Test
  void createTask_PitchScopeWithoutCycleInShapeUpProject_ShouldCreateBacklogTask() {
    // Cycle-at-creation is no longer required for any task category/project-type combination
    // (2026-08 cycle model change) — a PITCH_SCOPE task with an explicit projectId and no
    // cycle now succeeds, matching the DEBT_IMPROVEMENT no-cycle path.
    Project project = Project.builder().name("Shape Up Project").projectKey("SUP2")
        .projectType(ProjectType.SHAPE_UP).isActive(true).build();
    project = projectRepository.save(project);

    CreateTaskRequest request = new CreateTaskRequest();
    request.setTitle("Shaped pitch work");
    request.setProjectId(project.getId());
    request.setCategory(TaskCategory.PITCH_SCOPE);

    TaskDTO result = taskService.createTask(request);

    assertThat(result).isNotNull();
    assertThat(result.getCycleId()).isNull();
    assertThat(result.getCategory()).isEqualTo(TaskCategory.PITCH_SCOPE);
  }

  @Test
  void createTask_WithoutCycleInScrumProject_ShouldCreateProductBacklogTask() {
    Project project = Project.builder().name("Scrum Project").projectKey("SCR1")
        .projectType(ProjectType.SCRUM).isActive(true).build();
    project = projectRepository.save(project);

    CreateTaskRequest request = new CreateTaskRequest();
    request.setTitle("Sprint-less backlog item");
    request.setProjectId(project.getId());

    TaskDTO result = taskService.createTask(request);

    assertThat(result).isNotNull();
    assertThat(result.getCycleId()).isNull();
  }

  @Test
  void updateTask_WhenExists_ShouldUpdateTask() {
    testRequest.setTitle("Updated Task");
    TaskDTO result = taskService.updateTask(testTask.getId(), testRequest);

    assertThat(result).isNotNull();
    assertThat(result.getTitle()).isEqualTo("Updated Task");
  }

  @Test
  void createTask_SubtaskWithExplicitBacklogStatus_ShouldThrowException() {
    CreateTaskRequest request = new CreateTaskRequest();
    request.setTitle("Subtask");
    request.setCycleId(testCycle.getId());
    request.setParentTaskId(testTask.getId());
    request.setStatus(TaskStatus.BACKLOG);

    assertThatThrownBy(() -> taskService.createTask(request))
        .isInstanceOf(com.github.farzadsedaghatbin.shipflow.exception.BadRequestException.class);
  }

  @Test
  void createTask_SubtaskWithoutExplicitStatus_ShouldDefaultToTodoNotBacklog() {
    CreateTaskRequest request = new CreateTaskRequest();
    request.setTitle("Subtask");
    request.setCycleId(testCycle.getId());
    request.setParentTaskId(testTask.getId());

    TaskDTO result = taskService.createTask(request);

    assertThat(result.getStatus()).isEqualTo(TaskStatus.TODO);
  }

  @Test
  void updateTask_SubtaskWithBacklogStatus_ShouldThrowException() {
    Task subtask = Task.builder().title("Existing Subtask").status(TaskStatus.TODO)
        .priority(TaskPriority.MEDIUM).category(TaskCategory.PITCH_SCOPE).cycle(testCycle)
        .parentTask(testTask).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    subtask = taskRepository.save(subtask);

    CreateTaskRequest request = new CreateTaskRequest();
    request.setTitle("Existing Subtask");
    request.setCycleId(testCycle.getId());
    request.setParentTaskId(testTask.getId());
    request.setStatus(TaskStatus.BACKLOG);

    Long subtaskId = subtask.getId();
    assertThatThrownBy(() -> taskService.updateTask(subtaskId, request))
        .isInstanceOf(com.github.farzadsedaghatbin.shipflow.exception.BadRequestException.class);
  }

  @Test
  void updateTask_AssigningParentAndBacklogStatusInSameRequest_ShouldThrowException() {
    // A fresh project-linked cycle is needed here (unlike testCycle, which has no project) so
    // updateTask's "fail closed" parent/project validation doesn't short-circuit before we ever
    // reach the subtask-Backlog check. This request both makes the task a subtask AND sets
    // BACKLOG in the same call — must be rejected, not just the "already a subtask" case.
    Project project = Project.builder().name("Hierarchy Project").projectKey("HP1")
        .projectType(ProjectType.SHAPE_UP).isActive(true).build();
    project = projectRepository.save(project);

    Cycle cycle = Cycle.builder().name("Hierarchy Cycle").project(project).startDate(LocalDate.now())
        .endDate(LocalDate.now().plusWeeks(6)).phase(CyclePhase.SHAPING_BUILDING).isActive(true).build();
    cycle = cycleRepository.save(cycle);

    Task futureParent = Task.builder().title("Future Parent").status(TaskStatus.TODO)
        .priority(TaskPriority.MEDIUM).category(TaskCategory.PITCH_SCOPE).cycle(cycle)
        .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    futureParent = taskRepository.save(futureParent);

    Task plainTask = Task.builder().title("Plain Task").status(TaskStatus.TODO)
        .priority(TaskPriority.MEDIUM).category(TaskCategory.PITCH_SCOPE).cycle(cycle)
        .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    plainTask = taskRepository.save(plainTask);

    CreateTaskRequest request = new CreateTaskRequest();
    request.setTitle("Plain Task");
    request.setCycleId(cycle.getId());
    request.setParentTaskId(futureParent.getId());
    request.setStatus(TaskStatus.BACKLOG);

    Long taskId = plainTask.getId();
    assertThatThrownBy(() -> taskService.updateTask(taskId, request))
        .isInstanceOf(com.github.farzadsedaghatbin.shipflow.exception.BadRequestException.class);
  }

  @Test
  void updateTaskStatus_ShouldUpdateStatus() {
    TaskDTO result = taskService.updateTaskStatus(testTask.getId(), TaskStatus.IN_PROGRESS);

    assertThat(result).isNotNull();
    assertThat(result.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
  }

  @Test
  void updateTaskStatus_SubtaskToBacklog_ShouldThrowException() {
    Task subtask = Task.builder().title("Existing Subtask").status(TaskStatus.TODO)
        .priority(TaskPriority.MEDIUM).category(TaskCategory.PITCH_SCOPE).cycle(testCycle)
        .parentTask(testTask).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    subtask = taskRepository.save(subtask);

    Long subtaskId = subtask.getId();
    assertThatThrownBy(() -> taskService.updateTaskStatus(subtaskId, TaskStatus.BACKLOG))
        .isInstanceOf(com.github.farzadsedaghatbin.shipflow.exception.BadRequestException.class);
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
  void createTask_WithoutCategoryOrPitch_ShouldDefaultToDebtImprovement() {
    testRequest.setCategory(null);
    testRequest.setPitchId(null);

    TaskDTO result = taskService.createTask(testRequest);

    assertThat(result).isNotNull();
    assertThat(result.getCategory()).isEqualTo(TaskCategory.DEBT_IMPROVEMENT);
  }

  @Test
  void createTask_WithoutCategoryButWithPitch_ShouldDefaultToPitchScope() {
    testRequest.setCategory(null);
    testRequest.setPitchId(testPitch.getId());

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
        .pitchId(testPitch.getId())
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
        .pitchId(testPitch.getId())
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

  // ========== Cycle Update Tests ==========

  @Test
  void updateTask_WithNewCycleId_ShouldUpdateCycle() {
    // Given: A second cycle
    Cycle newCycle = Cycle.builder()
        .name("New Cycle")
        .startDate(LocalDate.now().plusWeeks(7))
        .endDate(LocalDate.now().plusWeeks(13))
        .phase(CyclePhase.SHAPING_BUILDING)
        .isActive(false)
        .build();
    newCycle = cycleRepository.save(newCycle);

    testRequest.setCycleId(newCycle.getId());

    // When: Updating the task's cycle
    TaskDTO result = taskService.updateTask(testTask.getId(), testRequest);

    // Then: Cycle should be updated
    assertThat(result.getCycleId()).isEqualTo(newCycle.getId());
  }

  @Test
  void updateTask_ChangingCycle_ShouldClearParentInOldCycle() {
    // Given: A parent task in the original cycle and a child linked to it
    Task parentTask = Task.builder()
        .title("Parent Task")
        .description("Parent in original cycle")
        .status(TaskStatus.TODO)
        .priority(TaskPriority.MEDIUM)
        .category(TaskCategory.PITCH_SCOPE)
        .estimateHours(BigDecimal.valueOf(4.0))
        .cycle(testCycle)
        .assignee(testPerson)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
    parentTask = taskRepository.save(parentTask);

    testTask.setParentTask(parentTask);
    testTask = taskRepository.save(testTask);

    // Create a new cycle to move the child into
    Cycle newCycle = Cycle.builder()
        .name("New Cycle")
        .startDate(LocalDate.now().plusWeeks(7))
        .endDate(LocalDate.now().plusWeeks(13))
        .phase(CyclePhase.SHAPING_BUILDING)
        .isActive(false)
        .build();
    newCycle = cycleRepository.save(newCycle);

    testRequest.setCycleId(newCycle.getId());
    testRequest.setParentTaskId(null);

    // When: Moving task to the new cycle
    TaskDTO result = taskService.updateTask(testTask.getId(), testRequest);

    // Then: Cycle updated and parent cleared (parent is in old cycle)
    assertThat(result.getCycleId()).isEqualTo(newCycle.getId());
    assertThat(result.getParentTaskId()).isNull();
  }

  // ========== Team Assignment Tests ==========

  @Test
  void updateTask_WithTeamId_ShouldAssignTeam() {
    // Given: A team
    Team team = Team.builder().name("Backend Team").build();
    team = teamRepository.save(team);

    testRequest.setTeamId(team.getId());

    // When: Updating the task with a team
    TaskDTO result = taskService.updateTask(testTask.getId(), testRequest);

    // Then: Team should be assigned
    assertThat(result.getTeamId()).isEqualTo(team.getId());
    assertThat(result.getTeamName()).isEqualTo("Backend Team");
  }

  @Test
  void updateTask_ClearingTeamId_ShouldRemoveTeam() {
    // Given: A task with a team assigned
    Team team = Team.builder().name("Backend Team").build();
    team = teamRepository.save(team);
    testTask.setTeam(team);
    testTask = taskRepository.save(testTask);

    testRequest.setTeamId(null);

    // When: Updating the task without a team
    TaskDTO result = taskService.updateTask(testTask.getId(), testRequest);

    // Then: Team should be cleared
    assertThat(result.getTeamId()).isNull();
    assertThat(result.getTeamName()).isNull();
  }

  @Test
  void setTargetRelease_WhenTaskAndReleaseExist_ShouldSetRelease() {
    Project project = Project.builder().name("Test Project").projectKey("TRT").isActive(true).build();
    project = projectRepository.save(project);

    Release release = Release.builder().name("Q3 Release").version("v3.0.0")
        .status(ReleaseStatus.PLANNING).project(project).build();
    release = releaseRepository.save(release);

    TaskDTO result = taskService.setTargetRelease(testTask.getId(), release.getId());

    assertThat(result).isNotNull();
    assertThat(result.getTargetReleaseId()).isEqualTo(release.getId());
    assertThat(result.getTargetReleaseName()).isEqualTo("Q3 Release");
    assertThat(result.getTargetReleaseVersion()).isEqualTo("v3.0.0");
  }

  @Test
  void setTargetRelease_WhenTaskNotFound_ShouldThrowException() {
    Project project = Project.builder().name("Test Project").projectKey("TRT2").isActive(true).build();
    project = projectRepository.save(project);

    Release release = Release.builder().name("Q3 Release").version("v3.0.0")
        .status(ReleaseStatus.PLANNING).project(project).build();
    release = releaseRepository.save(release);

    Long releaseId = release.getId();
    assertThatThrownBy(() -> taskService.setTargetRelease(999L, releaseId))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Task not found");
  }

  @Test
  void setTargetRelease_WhenReleaseNotFound_ShouldThrowException() {
    Long taskId = testTask.getId();
    assertThatThrownBy(() -> taskService.setTargetRelease(taskId, 999L))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Release not found");
  }

  @Test
  void clearTargetRelease_WhenTaskHasRelease_ShouldClearRelease() {
    Project project = Project.builder().name("Test Project").projectKey("TRT3").isActive(true).build();
    project = projectRepository.save(project);

    Release release = Release.builder().name("Q3 Release").version("v3.0.0")
        .status(ReleaseStatus.PLANNING).project(project).build();
    release = releaseRepository.save(release);

    taskService.setTargetRelease(testTask.getId(), release.getId());

    TaskDTO result = taskService.clearTargetRelease(testTask.getId());

    assertThat(result).isNotNull();
    assertThat(result.getTargetReleaseId()).isNull();
    assertThat(result.getTargetReleaseName()).isNull();
  }

  @Test
  void clearTargetRelease_WhenTaskNotFound_ShouldThrowException() {
    assertThatThrownBy(() -> taskService.clearTargetRelease(999L))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Task not found");
  }

  // ── bulkCreate ───────────────────────────────────────────────────────────────

  @Test
  void bulkCreate_WithValidSuggestions_ShouldCreateAllTasksUnderPitchAndCycle() {
    BulkCreateTaskRequest request = new BulkCreateTaskRequest();
    request.setPitchId(testPitch.getId());
    request.setCycleId(testCycle.getId());
    request.setTasks(List.of(
        TaskSuggestionDTO.builder().title("Build API endpoint")
            .description("Backend + mobile deliverable").estimateHours(BigDecimal.valueOf(8))
            .sourceContext(SuggestionSource.PITCH)
            .disciplines(List.of(Discipline.BACKEND, Discipline.MOBILE)).build(),
        TaskSuggestionDTO.builder().title("Write migration script")
            .description("Pure backend").sourceContext(SuggestionSource.PITCH)
            .disciplines(List.of(Discipline.BACKEND)).build()));

    BulkCreateTaskResult result = taskService.bulkCreate(request);

    assertThat(result.getSuccessCount()).isEqualTo(2);
    assertThat(result.getFailureCount()).isZero();
    assertThat(result.getErrors()).isEmpty();
    assertThat(result.getCreatedTasks()).hasSize(2);
    assertThat(result.getCreatedTasks()).extracting(TaskDTO::getTitle)
        .containsExactlyInAnyOrder("Build API endpoint", "Write migration script");
    assertThat(result.getCreatedTasks()).allSatisfy(dto -> {
      assertThat(dto.getPitchId()).isEqualTo(testPitch.getId());
      assertThat(dto.getCategory()).isEqualTo(TaskCategory.PITCH_SCOPE);
    });
  }

  @Test
  void bulkCreate_WithOneInvalidPitch_ShouldReportPartialFailure() {
    BulkCreateTaskRequest request = new BulkCreateTaskRequest();
    request.setPitchId(999L);
    request.setCycleId(testCycle.getId());
    request.setTasks(List.of(
        TaskSuggestionDTO.builder().title("Orphan task").sourceContext(SuggestionSource.PITCH)
            .disciplines(List.of(Discipline.BACKEND)).build()));

    BulkCreateTaskResult result = taskService.bulkCreate(request);

    assertThat(result.getSuccessCount()).isZero();
    assertThat(result.getFailureCount()).isEqualTo(1);
    assertThat(result.getErrors().get(0)).contains("Orphan task").contains("Pitch not found");
    assertThat(result.getCreatedTasks()).isEmpty();
  }

  // ========== createTask cycle/project resolution order (B.2) ==========

  @Test
  void createTask_WithExplicitCycleId_ShouldWinOverPitchDerivation() {
    // testPitch (from setUp) is linked to testCycle, which has no project — if cycleId weren't
    // taking priority, pitch-derivation would fail to resolve a project and throw.
    CreateTaskRequest request = CreateTaskRequest.builder().title("Explicit cycle wins")
        .cycleId(testCycle.getId()).pitchId(testPitch.getId()).build();

    TaskDTO result = taskService.createTask(request);

    assertThat(result.getCycleId()).isEqualTo(testCycle.getId());
    assertThat(result.getPitchId()).isEqualTo(testPitch.getId());
  }

  @Test
  void createTask_WithExplicitProjectId_ShouldWinOverPitchDerivation() {
    Project project = projectRepository.save(Project.builder().name("Explicit Project Wins")
        .projectKey("EPW1").projectType(ProjectType.SHAPE_UP).isActive(true).build());

    // pitchId is also supplied, but with no cycleId, explicit projectId must take priority over
    // pitch-derivation — testPitch's own cycle (testCycle) has no project set, so if
    // pitch-derivation ran instead it would reject the request with error.task.pitch.no.project.
    CreateTaskRequest request = CreateTaskRequest.builder().title("Explicit project wins")
        .projectId(project.getId()).pitchId(testPitch.getId()).build();

    TaskDTO result = taskService.createTask(request);

    assertThat(result.getCycleId()).isNull();
    assertThat(result.getProjectId()).isEqualTo(project.getId());
    assertThat(result.getPitchId()).isEqualTo(testPitch.getId());
  }

  @Test
  void createTask_WithOnlyPitchId_ShouldDeriveCycleAndProjectFromPitchCycle() {
    Project project = projectRepository.save(Project.builder().name("Pitch Derivation Project")
        .projectKey("PDP1").projectType(ProjectType.SHAPE_UP).isActive(true).build());
    Cycle pitchCycle = cycleRepository.save(Cycle.builder().name("Pitch Cycle").project(project)
        .startDate(LocalDate.now()).endDate(LocalDate.now().plusWeeks(6))
        .phase(CyclePhase.SHAPING_BUILDING).isActive(true).build());
    Pitch betPitch = pitchRepository.save(Pitch.builder().title("Bet Pitch").status(PitchStatus.PENDING)
        .appetiteDays(14).cycle(pitchCycle).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

    CreateTaskRequest request = CreateTaskRequest.builder().title("Pitch-derived task")
        .pitchId(betPitch.getId()).build();

    TaskDTO result = taskService.createTask(request);

    assertThat(result.getCycleId()).isEqualTo(pitchCycle.getId());
    assertThat(result.getProjectId()).isEqualTo(project.getId());
    assertThat(result.getPitchId()).isEqualTo(betPitch.getId());
  }

  @Test
  void createTask_WithPitchNotYetBet_ShouldSucceedWithNullCycle() {
    // A pitch not yet bet (no cycle) but linked to an epic still resolves a project via the
    // epic fallback (B.1) — the task should be created successfully with cycle = null.
    Project project = projectRepository.save(Project.builder().name("Epic Fallback Project")
        .projectKey("EFP1").projectType(ProjectType.SHAPE_UP).isActive(true).build());
    Epic epic = epicRepository.save(Epic.builder().name("Test Epic").status(EpicStatus.PLANNED)
        .project(project).build());
    Pitch unbetPitch = pitchRepository.save(Pitch.builder().title("Unbet Pitch").status(PitchStatus.SHAPED)
        .appetiteDays(10).epic(epic).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

    CreateTaskRequest request = CreateTaskRequest.builder().title("Task on unbet pitch")
        .pitchId(unbetPitch.getId()).build();

    TaskDTO result = taskService.createTask(request);

    assertThat(result.getCycleId()).isNull();
    assertThat(result.getProjectId()).isEqualTo(project.getId());
  }

  @Test
  void createTask_WithPitchResolvingNoProject_ShouldThrowBadRequestException() {
    // A pitch with no cycle, no direct project, and no epic cannot resolve a project — must be
    // rejected with the dedicated error.task.pitch.no.project message, not silently create an
    // unqueryable project-less task.
    Pitch orphanPitch = pitchRepository.save(Pitch.builder().title("Orphan Pitch").status(PitchStatus.IDEA)
        .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

    CreateTaskRequest request = CreateTaskRequest.builder().title("Should be rejected")
        .pitchId(orphanPitch.getId()).build();

    assertThatThrownBy(() -> taskService.createTask(request))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  void createTask_WithNoCycleProjectOrPitch_ShouldThrowBadRequestException() {
    CreateTaskRequest request = CreateTaskRequest.builder().title("Nowhere to go").build();

    assertThatThrownBy(() -> taskService.createTask(request))
        .isInstanceOf(BadRequestException.class);
  }

  // ========== bulkUpdate NPE fix (cycle-less pitch-scope tasks) ==========

  @Test
  void bulkUpdate_WithCycleLessPitchScopeTask_ShouldNotThrowNPE() {
    // Regression test: bulkUpdate used to derive project via t.getCycle().getProject().getId(),
    // which NPEs once cycle-less PITCH_SCOPE tasks (not yet bet on a cycle) are allowed to exist.
    Project project = projectRepository.save(Project.builder().name("Bulk Update NPE Project")
        .projectKey("BUNP1").projectType(ProjectType.SHAPE_UP).isActive(true).build());
    Pitch unbetPitch = pitchRepository.save(Pitch.builder().title("Unbet Pitch For Bulk").status(PitchStatus.IDEA)
        .project(project).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

    CreateTaskRequest createRequest = CreateTaskRequest.builder().title("Cycle-less pitch task")
        .pitchId(unbetPitch.getId()).build();
    TaskDTO created = taskService.createTask(createRequest);
    assertThat(created.getCycleId()).isNull();

    BulkTaskUpdateRequest bulkRequest = new BulkTaskUpdateRequest();
    bulkRequest.setTaskIds(List.of(created.getId()));
    bulkRequest.setAction(BulkAction.CHANGE_PRIORITY);
    bulkRequest.setValue("HIGH");

    BulkUpdateResult result = taskService.bulkUpdate(bulkRequest);

    assertThat(result.getSuccessCount()).isEqualTo(1);
    assertThat(result.getFailureCount()).isZero();
  }
}
