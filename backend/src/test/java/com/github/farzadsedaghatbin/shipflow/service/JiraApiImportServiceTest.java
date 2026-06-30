package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskPriority;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskStatus;
import com.github.farzadsedaghatbin.shipflow.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

/**
 * Pure unit tests for JiraApiImportService — covers status and priority mapping helpers.
 * No Spring context, no HTTP calls.
 */
@ExtendWith(MockitoExtension.class)
class JiraApiImportServiceTest {

  @Mock private JiraOAuthService jiraOAuthService;
  @Mock private ProjectRepository projectRepository;
  @Mock private CycleRepository cycleRepository;
  @Mock private EpicRepository epicRepository;
  @Mock private TaskRepository taskRepository;
  @Mock private BugReportRepository bugReportRepository;
  @Mock private ImportJobRepository importJobRepository;
  @Mock private PersonRepository personRepository;
  @Mock private RestTemplate restTemplate;

  private JiraApiImportService service;

  @BeforeEach
  void setUp() {
    service =
        new JiraApiImportService(
            jiraOAuthService,
            projectRepository,
            cycleRepository,
            epicRepository,
            taskRepository,
            bugReportRepository,
            importJobRepository,
            personRepository,
            restTemplate,
            new ObjectMapper());
  }

  // ── mapJiraStatus ─────────────────────────────────────────────────────────

  @Test
  @DisplayName("mapJiraStatus: 'Done' → DONE")
  void status_done() {
    assertThat(service.mapJiraStatus("Done")).isEqualTo(TaskStatus.DONE);
  }

  @Test
  @DisplayName("mapJiraStatus: 'Closed' → DONE")
  void status_closed() {
    assertThat(service.mapJiraStatus("Closed")).isEqualTo(TaskStatus.DONE);
  }

  @Test
  @DisplayName("mapJiraStatus: 'Resolved' → DONE")
  void status_resolved() {
    assertThat(service.mapJiraStatus("Resolved")).isEqualTo(TaskStatus.DONE);
  }

  @Test
  @DisplayName("mapJiraStatus: 'In Progress' → IN_PROGRESS")
  void status_inProgress() {
    assertThat(service.mapJiraStatus("In Progress")).isEqualTo(TaskStatus.IN_PROGRESS);
  }

  @Test
  @DisplayName("mapJiraStatus: 'In Review' → IN_PROGRESS")
  void status_inReview() {
    assertThat(service.mapJiraStatus("In Review")).isEqualTo(TaskStatus.IN_PROGRESS);
  }

  @Test
  @DisplayName("mapJiraStatus: 'Cancelled' → CANCELLED")
  void status_cancelled() {
    assertThat(service.mapJiraStatus("Cancelled")).isEqualTo(TaskStatus.CANCELLED);
  }

  @Test
  @DisplayName("mapJiraStatus: \"Won't Fix\" → CANCELLED")
  void status_wontFix() {
    assertThat(service.mapJiraStatus("Won't Fix")).isEqualTo(TaskStatus.CANCELLED);
  }

  @Test
  @DisplayName("mapJiraStatus: 'Blocked' → BLOCKED")
  void status_blocked() {
    assertThat(service.mapJiraStatus("Blocked")).isEqualTo(TaskStatus.BLOCKED);
  }

  @Test
  @DisplayName("mapJiraStatus: 'Backlog' → BACKLOG")
  void status_backlog() {
    assertThat(service.mapJiraStatus("Backlog")).isEqualTo(TaskStatus.BACKLOG);
  }

  @Test
  @DisplayName("mapJiraStatus: 'To Do' → TODO")
  void status_toDo() {
    assertThat(service.mapJiraStatus("To Do")).isEqualTo(TaskStatus.TODO);
  }

  @Test
  @DisplayName("mapJiraStatus: null → TODO")
  void status_null() {
    assertThat(service.mapJiraStatus(null)).isEqualTo(TaskStatus.TODO);
  }

  @Test
  @DisplayName("mapJiraStatus: unknown string → TODO")
  void status_unknown() {
    assertThat(service.mapJiraStatus("Open")).isEqualTo(TaskStatus.TODO);
  }

  // ── mapJiraPriority ───────────────────────────────────────────────────────

  @Test
  @DisplayName("mapJiraPriority: 'Highest' → URGENT")
  void priority_highest() {
    assertThat(service.mapJiraPriority("Highest")).isEqualTo(TaskPriority.URGENT);
  }

  @Test
  @DisplayName("mapJiraPriority: 'Critical' → URGENT")
  void priority_critical() {
    assertThat(service.mapJiraPriority("Critical")).isEqualTo(TaskPriority.URGENT);
  }

  @Test
  @DisplayName("mapJiraPriority: 'High' → HIGH")
  void priority_high() {
    assertThat(service.mapJiraPriority("High")).isEqualTo(TaskPriority.HIGH);
  }

  @Test
  @DisplayName("mapJiraPriority: 'Medium' → MEDIUM")
  void priority_medium() {
    assertThat(service.mapJiraPriority("Medium")).isEqualTo(TaskPriority.MEDIUM);
  }

  @Test
  @DisplayName("mapJiraPriority: 'Low' → LOW")
  void priority_low() {
    assertThat(service.mapJiraPriority("Low")).isEqualTo(TaskPriority.LOW);
  }

  @Test
  @DisplayName("mapJiraPriority: 'Lowest' → LOW")
  void priority_lowest() {
    assertThat(service.mapJiraPriority("Lowest")).isEqualTo(TaskPriority.LOW);
  }

  @Test
  @DisplayName("mapJiraPriority: null → MEDIUM")
  void priority_null() {
    assertThat(service.mapJiraPriority(null)).isEqualTo(TaskPriority.MEDIUM);
  }

  @Test
  @DisplayName("mapJiraPriority: unknown string → MEDIUM")
  void priority_unknown() {
    assertThat(service.mapJiraPriority("SomethingElse")).isEqualTo(TaskPriority.MEDIUM);
  }
}
