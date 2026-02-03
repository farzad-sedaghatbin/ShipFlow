package com.github.farzadsedaghatbin.shipflow.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.CreateTaskRequest;
import com.github.farzadsedaghatbin.shipflow.entity.*;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CyclePhase;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskPriority;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskStatus;
import com.github.farzadsedaghatbin.shipflow.repository.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@WithMockUser(username = "admin", roles = {"ADMIN"})
class TaskControllerIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private TaskRepository taskRepository;

  @Autowired
  private PersonRepository personRepository;

  @Autowired
  private CycleRepository cycleRepository;

  @Autowired
  private UserRepository userRepository;

  private Cycle testCycle;
  private Person testPerson;
  private Person testPairPerson;
  private Task testTask;

  @BeforeEach
  void setUp() {
    taskRepository.deleteAll();
    cycleRepository.deleteAll();
    userRepository.deleteAll();
    personRepository.deleteAll();
    taskRepository.flush();
    cycleRepository.flush();
    userRepository.flush();
    personRepository.flush();

    testCycle = Cycle.builder().name("Test Cycle").phase(CyclePhase.BUILD).startDate(LocalDate.now())
        .endDate(LocalDate.now().plusWeeks(6)).isActive(true).build();
    testCycle = cycleRepository.save(testCycle);

    testPerson = Person.builder().name("John Doe").email("john.doe@example.com").isActive(true)
        .createdAt(LocalDateTime.now()).build();
    testPerson = personRepository.save(testPerson);

    testPairPerson = Person.builder().name("Jane Smith").email("jane.smith@example.com").isActive(true)
        .createdAt(LocalDateTime.now()).build();
    testPairPerson = personRepository.save(testPairPerson);

    testTask = Task.builder().title("Test Task").description("Test task description").status(TaskStatus.TODO)
        .priority(TaskPriority.MEDIUM).estimateHours(new BigDecimal("4.00")).cycle(testCycle)
        .assignee(testPerson).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    testTask = taskRepository.save(testTask);
    taskRepository.flush();
  }

  @Test
  void getAllTasks_ShouldReturnTasks() throws Exception {
    mockMvc.perform(get("/api/tasks").param("page", "0").param("size", "10").param("sortBy", "createdAt")
        .param("sortOrder", "desc")).andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
        .andExpect(jsonPath("$.content[0].title", is("Test Task")))
        .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(1)));
  }

  @Test
  void getTaskById_WhenExists_ShouldReturnTask() throws Exception {
    mockMvc.perform(get("/api/tasks/{id}", testTask.getId())).andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id", is(testTask.getId().intValue())))
        .andExpect(jsonPath("$.title", is("Test Task"))).andExpect(jsonPath("$.status", is("TODO")))
        .andExpect(jsonPath("$.priority", is("MEDIUM")));
  }

  @Test
  void getTaskById_WhenNotExists_ShouldReturn400() throws Exception {
    mockMvc.perform(get("/api/tasks/{id}", 9999L)).andExpect(status().isBadRequest());
  }

  @Test
  void getTasksByCycleId_ShouldReturnTasksForCycle() throws Exception {
    mockMvc.perform(get("/api/tasks/cycle/{cycleId}", testCycle.getId())).andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
        .andExpect(jsonPath("$.content[0].cycleId", is(testCycle.getId().intValue())))
        .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(1)));
  }

  @Test
  void getTasksByCycleIdAndStatus_ShouldReturnFilteredTasks() throws Exception {
    mockMvc.perform(get("/api/tasks/cycle/{cycleId}/status/{status}", testCycle.getId(), "TODO"))
        .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
        .andExpect(jsonPath("$.content[0].status", is("TODO")))
        .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(1)));
  }

  @Test
  void getTaskStatisticsByCycleId_ShouldReturnStatistics() throws Exception {
    mockMvc.perform(get("/api/tasks/cycle/{cycleId}/statistics", testCycle.getId())).andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.cycleId", is(testCycle.getId().intValue())))
        .andExpect(jsonPath("$.totalTasks", greaterThanOrEqualTo(1)))
        .andExpect(jsonPath("$.todoTasks", greaterThanOrEqualTo(1)));
  }

  @Test
  void createTask_WithValidData_ShouldCreateTask() throws Exception {
    CreateTaskRequest request = CreateTaskRequest.builder().title("New Task").description("New task description")
        .cycleId(testCycle.getId()).status(TaskStatus.BACKLOG).priority(TaskPriority.HIGH)
        .estimateHours(new BigDecimal("8.00")).assigneeId(testPerson.getId()).build();

    mockMvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))).andExpect(status().isCreated())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.title", is("New Task"))).andExpect(jsonPath("$.status", is("BACKLOG")))
        .andExpect(jsonPath("$.priority", is("HIGH"))).andExpect(jsonPath("$.assigneeName", is("John Doe")));
  }

  @Test
  void createTask_WithPairAssignee_ShouldCreateTaskWithBothAssignees() throws Exception {
    CreateTaskRequest request = CreateTaskRequest.builder().title("Pair Task")
        .description("Task for pair programming").cycleId(testCycle.getId()).status(TaskStatus.TODO)
        .priority(TaskPriority.MEDIUM).estimateHours(new BigDecimal("6.00")).assigneeId(testPerson.getId())
        .pairAssigneeId(testPairPerson.getId()).build();

    mockMvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))).andExpect(status().isCreated())
        .andExpect(jsonPath("$.title", is("Pair Task"))).andExpect(jsonPath("$.assigneeName", is("John Doe")))
        .andExpect(jsonPath("$.pairAssigneeName", is("Jane Smith")));
  }

  @Test
  void createTask_WithDueDate_ShouldCreateTaskWithDueDate() throws Exception {
    CreateTaskRequest request = CreateTaskRequest.builder().title("Task with Due Date").cycleId(testCycle.getId())
        .priority(TaskPriority.URGENT).dueDate(LocalDate.now().plusDays(7)).build();

    mockMvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))).andExpect(status().isCreated())
        .andExpect(jsonPath("$.dueDate", notNullValue()));
  }

  @Test
  void createTask_WithTags_ShouldCreateTaskWithTags() throws Exception {
    CreateTaskRequest request = CreateTaskRequest.builder().title("Tagged Task").cycleId(testCycle.getId())
        .tags("bug,tech-debt,refactor").build();

    mockMvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))).andExpect(status().isCreated())
        .andExpect(jsonPath("$.tags", is("bug,tech-debt,refactor")));
  }

  @Test
  void createTask_WithoutTitle_ShouldReturn400() throws Exception {
    CreateTaskRequest request = CreateTaskRequest.builder().cycleId(testCycle.getId()).build();

    mockMvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest());
  }

  @Test
  void updateTask_WithValidData_ShouldUpdateTask() throws Exception {
    CreateTaskRequest request = CreateTaskRequest.builder().title("Updated Task").description("Updated description")
        .cycleId(testCycle.getId()).status(TaskStatus.IN_PROGRESS).priority(TaskPriority.HIGH)
        .estimateHours(new BigDecimal("10.00")).assigneeId(testPerson.getId()).build();

    mockMvc.perform(put("/api/tasks/{id}", testTask.getId()).contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))).andExpect(status().isOk())
        .andExpect(jsonPath("$.title", is("Updated Task"))).andExpect(jsonPath("$.status", is("IN_PROGRESS")))
        .andExpect(jsonPath("$.priority", is("HIGH")));
  }

  @Test
  void updateTaskStatus_ShouldUpdateOnlyStatus() throws Exception {
    mockMvc.perform(patch("/api/tasks/{id}/status", testTask.getId()).contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(Map.of("status", "IN_PROGRESS")))).andExpect(status().isOk())
        .andExpect(jsonPath("$.status", is("IN_PROGRESS")));
  }

  @Test
  void updateTaskStatus_ToDone_ShouldSetCompletedAt() throws Exception {
    mockMvc.perform(patch("/api/tasks/{id}/status", testTask.getId()).contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(Map.of("status", "DONE")))).andExpect(status().isOk())
        .andExpect(jsonPath("$.status", is("DONE"))).andExpect(jsonPath("$.completedAt", notNullValue()));
  }

  @Test
  void deleteTask_WhenExists_ShouldReturn204() throws Exception {
    mockMvc.perform(delete("/api/tasks/{id}", testTask.getId())).andExpect(status().isNoContent());
  }

  @Test
  void deleteTask_WhenNotExists_ShouldReturn400() throws Exception {
    mockMvc.perform(delete("/api/tasks/{id}", 9999L)).andExpect(status().isBadRequest());
  }

  @Test
  void getTasksByAssigneeId_ShouldReturnTasksForAssignee() throws Exception {
    mockMvc.perform(get("/api/tasks/assignee/{assigneeId}", testPerson.getId())).andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
        .andExpect(jsonPath("$[0].assigneeId", is(testPerson.getId().intValue())));
  }

  @Test
  void getTasksByPersonId_ShouldReturnTasksForPersonAsAssigneeOrPair() throws Exception {
    mockMvc.perform(get("/api/tasks/person/{personId}", testPerson.getId())).andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
  }

  @Test
  void getTasksWithFilters_ByStatuses_ShouldReturnFilteredTasks() throws Exception {
    // Create additional tasks with different statuses
    Task inProgressTask = Task.builder().title("In Progress Task").description("Task in progress")
        .status(TaskStatus.IN_PROGRESS).priority(TaskPriority.HIGH).cycle(testCycle).assignee(testPerson)
        .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    taskRepository.save(inProgressTask);

    mockMvc.perform(get("/api/tasks/cycle/{cycleId}/filter", testCycle.getId())
        .param("statuses", "TODO,IN_PROGRESS").param("exclude", "false")).andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(2))))
        .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(2)));
  }

  @Test
  void getTasksWithFilters_ByPriorities_ShouldReturnFilteredTasks() throws Exception {
    // Create additional task with different priority
    Task highPriorityTask = Task.builder().title("High Priority Task").description("Important task")
        .status(TaskStatus.TODO).priority(TaskPriority.HIGH).cycle(testCycle).assignee(testPerson)
        .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    taskRepository.save(highPriorityTask);

    mockMvc.perform(get("/api/tasks/cycle/{cycleId}/filter", testCycle.getId()).param("priorities", "HIGH")
        .param("exclude", "false")).andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
        .andExpect(jsonPath("$.content[0].priority", is("HIGH")));
  }

  @Test
  void getTasksWithFilters_WithExclude_ShouldReturnExcludedTasks() throws Exception {
    // Create tasks with different statuses
    Task inProgressTask = Task.builder().title("In Progress Task").status(TaskStatus.IN_PROGRESS)
        .priority(TaskPriority.MEDIUM).cycle(testCycle).createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now()).build();
    taskRepository.save(inProgressTask);

    mockMvc.perform(get("/api/tasks/cycle/{cycleId}/filter", testCycle.getId()).param("statuses", "DONE,CANCELLED")
        .param("exclude", "true")).andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(2))));
  }

  @Test
  void getTasksWithFilters_ByAssignees_ShouldReturnFilteredTasks() throws Exception {
    mockMvc.perform(get("/api/tasks/cycle/{cycleId}/filter", testCycle.getId())
        .param("assigneeIds", testPerson.getId().toString()).param("exclude", "false"))
        .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
        .andExpect(jsonPath("$.content[0].assigneeId", is(testPerson.getId().intValue())));
  }
}
