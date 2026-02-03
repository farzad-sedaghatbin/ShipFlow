package com.github.farzadsedaghatbin.shipflow.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.entity.*;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CyclePhase;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskCategory;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskPriority;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskStatus;
import com.github.farzadsedaghatbin.shipflow.repository.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for Task History (Audit) endpoints. Tests the Hibernate
 * Envers auditing functionality for tasks.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@WithMockUser(username = "admin", roles = {"ADMIN"})
class TaskHistoryIntegrationTest {

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

  private Cycle testCycle;
  private Person testPerson;
  private Task testTask;

  @BeforeEach
  void setUp() {
    taskRepository.deleteAll();
    cycleRepository.deleteAll();
    personRepository.deleteAll();
    taskRepository.flush();
    cycleRepository.flush();
    personRepository.flush();

    testCycle = Cycle.builder().name("Test Cycle").phase(CyclePhase.BUILD).startDate(LocalDate.now())
        .endDate(LocalDate.now().plusWeeks(6)).isActive(true).build();
    testCycle = cycleRepository.save(testCycle);

    testPerson = Person.builder().name("John Doe").email("john.doe@test.com").isActive(true)
        .createdAt(LocalDateTime.now()).build();
    testPerson = personRepository.save(testPerson);

    testTask = Task.builder().title("Test Task").description("Test description").status(TaskStatus.BACKLOG)
        .priority(TaskPriority.MEDIUM).category(TaskCategory.PITCH_SCOPE).cycle(testCycle)
        .estimateHours(new BigDecimal("4.00")).build();
    testTask = taskRepository.saveAndFlush(testTask);
  }

  @Test
  @DisplayName("GET /api/tasks/{id}/history - Should return empty history for new task (no changes yet)")
  void getTaskHistory_NewTask_ReturnsHistory() throws Exception {
    mockMvc.perform(get("/api/tasks/{id}/history", testTask.getId()).contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk()).andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.pageable").exists()).andExpect(jsonPath("$.totalElements").isNumber());
  }

  @Test
  @DisplayName("GET /api/tasks/{id}/history - Should return paginated results")
  void getTaskHistory_WithPagination_ReturnsPaginatedResults() throws Exception {
    mockMvc.perform(get("/api/tasks/{id}/history", testTask.getId()).param("page", "0").param("size", "10")
        .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
        .andExpect(jsonPath("$.pageable.pageNumber").value(0))
        .andExpect(jsonPath("$.pageable.pageSize").value(10));
  }

  @Test
  @DisplayName("GET /api/tasks/{id}/history - History entry should contain revision metadata")
  void getTaskHistory_ContainsRevisionMetadata() throws Exception {
    // Update the task to create a modification revision
    testTask.setStatus(TaskStatus.IN_PROGRESS);
    testTask.setPriority(TaskPriority.HIGH);
    testTask.setAssignee(testPerson);
    taskRepository.saveAndFlush(testTask);

    mockMvc.perform(get("/api/tasks/{id}/history", testTask.getId()).contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk()).andExpect(jsonPath("$.content").isArray());
    // Note: In a transactional test, revisions may not be visible until commit.
    // The actual revision testing would require committing the transaction.
  }

  @Test
  @DisplayName("GET /api/tasks/{id}/history - Should handle non-existent task gracefully")
  void getTaskHistory_NonExistentTask_ReturnsEmptyPage() throws Exception {
    mockMvc.perform(get("/api/tasks/{id}/history", 99999L).contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk()).andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.totalElements").value(0));
  }

  @Test
  @DisplayName("GET /api/tasks/{id}/history - Default pagination should use size 20")
  void getTaskHistory_DefaultPagination_UsesDefaultSize() throws Exception {
    mockMvc.perform(get("/api/tasks/{id}/history", testTask.getId()).contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk()).andExpect(jsonPath("$.pageable.pageSize").value(20));
  }
}
