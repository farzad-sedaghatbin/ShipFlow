package com.github.farzadsedaghatbin.shipflow.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.CreateTaskDependencyRequest;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Task;
import com.github.farzadsedaghatbin.shipflow.entity.TaskDependency;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CyclePhase;
import com.github.farzadsedaghatbin.shipflow.entity.enums.DependencyType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskPriority;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskStatus;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TaskDependencyRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TaskRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
class TaskDependencyControllerIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private TaskRepository taskRepository;

  @Autowired
  private TaskDependencyRepository taskDependencyRepository;

  @Autowired
  private CycleRepository cycleRepository;

  private Cycle testCycle;
  private Task task1;
  private Task task2;
  private Task task3;

  @BeforeEach
  void setUp() {
    taskDependencyRepository.deleteAll();
    taskRepository.deleteAll();
    cycleRepository.deleteAll();

    testCycle = Cycle.builder().name("Test Cycle").phase(CyclePhase.BUILD).startDate(LocalDate.now())
        .endDate(LocalDate.now().plusWeeks(6)).isActive(true).build();
    testCycle = cycleRepository.save(testCycle);

    task1 = Task.builder().title("Task 1").description("First task").status(TaskStatus.TODO)
        .priority(TaskPriority.MEDIUM).cycle(testCycle).createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now()).build();
    task1 = taskRepository.save(task1);

    task2 = Task.builder().title("Task 2").description("Second task").status(TaskStatus.TODO)
        .priority(TaskPriority.HIGH).cycle(testCycle).createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now()).build();
    task2 = taskRepository.save(task2);

    task3 = Task.builder().title("Task 3").description("Third task").status(TaskStatus.TODO)
        .priority(TaskPriority.LOW).cycle(testCycle).createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now()).build();
    task3 = taskRepository.save(task3);
  }

  @Test
  void addDependency_Success() throws Exception {
    // Given
    CreateTaskDependencyRequest request = CreateTaskDependencyRequest.builder().targetTaskId(task2.getId())
        .dependencyType(DependencyType.BLOCKS).build();

    // When & Then
    mockMvc.perform(post("/api/tasks/{taskId}/dependencies", task1.getId()).contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))).andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists()).andExpect(jsonPath("$.sourceTaskId").value(task1.getId()))
        .andExpect(jsonPath("$.sourceTaskTitle").value("Task 1"))
        .andExpect(jsonPath("$.targetTaskId").value(task2.getId()))
        .andExpect(jsonPath("$.targetTaskTitle").value("Task 2"))
        .andExpect(jsonPath("$.dependencyType").value("BLOCKS"));
  }

  @Test
  void addDependency_WithDependsOnType() throws Exception {
    // Given
    CreateTaskDependencyRequest request = CreateTaskDependencyRequest.builder().targetTaskId(task2.getId())
        .dependencyType(DependencyType.DEPENDS_ON).build();

    // When & Then
    mockMvc.perform(post("/api/tasks/{taskId}/dependencies", task1.getId()).contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))).andExpect(status().isCreated())
        .andExpect(jsonPath("$.dependencyType").value("DEPENDS_ON"));
  }

  @Test
  void addDependency_SelfReference_ReturnsBadRequest() throws Exception {
    // Given
    CreateTaskDependencyRequest request = CreateTaskDependencyRequest.builder().targetTaskId(task1.getId())
        .dependencyType(DependencyType.BLOCKS).build();

    // When & Then
    mockMvc.perform(post("/api/tasks/{taskId}/dependencies", task1.getId()).contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest());
  }

  @Test
  void addDependency_CircularDependency_ReturnsBadRequest() throws Exception {
    // Given - Create Task1 -> Task2 dependency first
    TaskDependency existingDep = TaskDependency.builder().sourceTask(task1).targetTask(task2)
        .dependencyType(DependencyType.BLOCKS).createdAt(LocalDateTime.now()).build();
    taskDependencyRepository.save(existingDep);

    // Try to create Task2 -> Task1 (circular)
    CreateTaskDependencyRequest request = CreateTaskDependencyRequest.builder().targetTaskId(task1.getId())
        .dependencyType(DependencyType.BLOCKS).build();

    // When & Then
    mockMvc.perform(post("/api/tasks/{taskId}/dependencies", task2.getId()).contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value(containsString("circular")));
  }

  @Test
  void addDependency_DuplicateDependency_ReturnsBadRequest() throws Exception {
    // Given - Create dependency first
    TaskDependency existingDep = TaskDependency.builder().sourceTask(task1).targetTask(task2)
        .dependencyType(DependencyType.BLOCKS).createdAt(LocalDateTime.now()).build();
    taskDependencyRepository.save(existingDep);

    // Try to create the same dependency again
    CreateTaskDependencyRequest request = CreateTaskDependencyRequest.builder().targetTaskId(task2.getId())
        .dependencyType(DependencyType.BLOCKS).build();

    // When & Then
    mockMvc.perform(post("/api/tasks/{taskId}/dependencies", task1.getId()).contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value(containsString("already exists")));
  }

  @Test
  void addDependency_TaskNotFound_ReturnsBadRequest() throws Exception {
    // Given
    CreateTaskDependencyRequest request = CreateTaskDependencyRequest.builder().targetTaskId(task2.getId())
        .dependencyType(DependencyType.BLOCKS).build();

    // When & Then
    mockMvc.perform(post("/api/tasks/{taskId}/dependencies", 99999L).contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Source task not found with id: 99999"));
  }

  @Test
  void getDependencies_Success() throws Exception {
    // Given - Create some dependencies
    TaskDependency blocking = TaskDependency.builder().sourceTask(task1).targetTask(task2)
        .dependencyType(DependencyType.BLOCKS).createdAt(LocalDateTime.now()).build();
    taskDependencyRepository.save(blocking);

    TaskDependency blockedBy = TaskDependency.builder().sourceTask(task3).targetTask(task1)
        .dependencyType(DependencyType.BLOCKS).createdAt(LocalDateTime.now()).build();
    taskDependencyRepository.save(blockedBy);

    // When & Then
    mockMvc.perform(get("/api/tasks/{taskId}/dependencies", task1.getId())).andExpect(status().isOk())
        .andExpect(jsonPath("$.blocking").isArray()).andExpect(jsonPath("$.blocking", hasSize(1)))
        .andExpect(jsonPath("$.blocking[0].targetTaskId").value(task2.getId()))
        .andExpect(jsonPath("$.blockedBy").isArray()).andExpect(jsonPath("$.blockedBy", hasSize(1)))
        .andExpect(jsonPath("$.blockedBy[0].sourceTaskId").value(task3.getId()));
  }

  @Test
  void getDependencies_NoDependencies_ReturnsEmptyLists() throws Exception {
    // When & Then
    mockMvc.perform(get("/api/tasks/{taskId}/dependencies", task1.getId())).andExpect(status().isOk())
        .andExpect(jsonPath("$.blocking").isArray()).andExpect(jsonPath("$.blocking", hasSize(0)))
        .andExpect(jsonPath("$.blockedBy").isArray()).andExpect(jsonPath("$.blockedBy", hasSize(0)));
  }

  @Test
  void getBlockingDependencies_Success() throws Exception {
    // Given
    TaskDependency dep1 = TaskDependency.builder().sourceTask(task1).targetTask(task2)
        .dependencyType(DependencyType.BLOCKS).createdAt(LocalDateTime.now()).build();
    taskDependencyRepository.save(dep1);

    TaskDependency dep2 = TaskDependency.builder().sourceTask(task1).targetTask(task3)
        .dependencyType(DependencyType.BLOCKS).createdAt(LocalDateTime.now()).build();
    taskDependencyRepository.save(dep2);

    // When & Then
    mockMvc.perform(get("/api/tasks/{taskId}/dependencies/blocking", task1.getId())).andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray()).andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[*].sourceTaskId", everyItem(is(task1.getId().intValue()))));
  }

  @Test
  void getBlockedByDependencies_Success() throws Exception {
    // Given
    TaskDependency dep1 = TaskDependency.builder().sourceTask(task2).targetTask(task1)
        .dependencyType(DependencyType.BLOCKS).createdAt(LocalDateTime.now()).build();
    taskDependencyRepository.save(dep1);

    TaskDependency dep2 = TaskDependency.builder().sourceTask(task3).targetTask(task1)
        .dependencyType(DependencyType.BLOCKS).createdAt(LocalDateTime.now()).build();
    taskDependencyRepository.save(dep2);

    // When & Then
    mockMvc.perform(get("/api/tasks/{taskId}/dependencies/blocked-by", task1.getId())).andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray()).andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[*].targetTaskId", everyItem(is(task1.getId().intValue()))));
  }

  @Test
  void removeDependency_Success() throws Exception {
    // Given
    TaskDependency dependency = TaskDependency.builder().sourceTask(task1).targetTask(task2)
        .dependencyType(DependencyType.BLOCKS).createdAt(LocalDateTime.now()).build();
    dependency = taskDependencyRepository.save(dependency);

    // When & Then
    mockMvc.perform(delete("/api/tasks/{taskId}/dependencies/{dependencyId}", task1.getId(), dependency.getId()))
        .andExpect(status().isNoContent());

    // Verify dependency was deleted
    mockMvc.perform(get("/api/tasks/{taskId}/dependencies", task1.getId())).andExpect(status().isOk())
        .andExpect(jsonPath("$.blocking", hasSize(0)));
  }

  @Test
  void removeDependency_NotFound_ReturnsBadRequest() throws Exception {
    // When & Then
    mockMvc.perform(delete("/api/tasks/{taskId}/dependencies/{dependencyId}", task1.getId(), 99999L))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Dependency not found with id: 99,999"));
  }

  @Test
  void complexDependencyChain_PreventsCircular() throws Exception {
    // Given - Create chain: Task1 -> Task2 -> Task3
    TaskDependency dep1 = TaskDependency.builder().sourceTask(task1).targetTask(task2)
        .dependencyType(DependencyType.BLOCKS).createdAt(LocalDateTime.now()).build();
    taskDependencyRepository.save(dep1);

    TaskDependency dep2 = TaskDependency.builder().sourceTask(task2).targetTask(task3)
        .dependencyType(DependencyType.BLOCKS).createdAt(LocalDateTime.now()).build();
    taskDependencyRepository.save(dep2);

    // Try to create Task3 -> Task1 (would create circular chain)
    CreateTaskDependencyRequest request = CreateTaskDependencyRequest.builder().targetTaskId(task1.getId())
        .dependencyType(DependencyType.BLOCKS).build();

    // When & Then
    mockMvc.perform(post("/api/tasks/{taskId}/dependencies", task3.getId()).contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value(containsString("circular")));
  }

  @Test
  void multipleDependencies_DifferentTypes() throws Exception {
    // Given
    CreateTaskDependencyRequest blocksRequest = CreateTaskDependencyRequest.builder().targetTaskId(task2.getId())
        .dependencyType(DependencyType.BLOCKS).build();

    CreateTaskDependencyRequest dependsOnRequest = CreateTaskDependencyRequest.builder().targetTaskId(task3.getId())
        .dependencyType(DependencyType.DEPENDS_ON).build();

    // When - Add two dependencies with different types
    mockMvc.perform(post("/api/tasks/{taskId}/dependencies", task1.getId()).contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(blocksRequest))).andExpect(status().isCreated());

    mockMvc.perform(post("/api/tasks/{taskId}/dependencies", task1.getId()).contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(dependsOnRequest))).andExpect(status().isCreated());

    // Then - Verify both dependencies exist
    mockMvc.perform(get("/api/tasks/{taskId}/dependencies", task1.getId())).andExpect(status().isOk())
        .andExpect(jsonPath("$.blocking", hasSize(2)))
        .andExpect(jsonPath("$.blocking[*].dependencyType", containsInAnyOrder("BLOCKS", "DEPENDS_ON")));
  }
}
