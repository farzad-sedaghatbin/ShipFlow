package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.farzadsedaghatbin.shipflow.entity.enums.ImportSourceFormat;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskPriority;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskStatus;
import com.github.farzadsedaghatbin.shipflow.repository.*;
import com.github.farzadsedaghatbin.shipflow.entity.enums.BugSeverity;
import com.github.farzadsedaghatbin.shipflow.entity.enums.BugStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Pure unit tests for CsvImportService — no Spring context.
 * Covers format detection and status/priority mapping logic.
 */
@ExtendWith(MockitoExtension.class)
class CsvImportServiceTest {

  @Mock private ImportJobRepository importJobRepository;
  @Mock private ProjectRepository projectRepository;
  @Mock private TaskRepository taskRepository;
  @Mock private EpicRepository epicRepository;
  @Mock private CycleRepository cycleRepository;
  @Mock private PersonRepository personRepository;
  @Mock private UserRepository userRepository;
  @Mock private BugReportRepository bugReportRepository;

  private CsvImportService service;

  @BeforeEach
  void setUp() {
    service =
        new CsvImportService(
            importJobRepository,
            projectRepository,
            taskRepository,
            epicRepository,
            cycleRepository,
            personRepository,
            userRepository,
            bugReportRepository);
  }

  // ---- detectFormat -------------------------------------------------------

  @Test
  @DisplayName("detectFormat: Jira CSV identified by Summary + Issue Type")
  void detectFormat_jira() {
    String[] headers = {"Summary", "Issue Type", "Sprint", "Status", "Priority"};
    assertThat(service.detectFormat(headers)).isEqualTo(ImportSourceFormat.JIRA_CSV);
  }

  @Test
  @DisplayName("detectFormat: Jira CSV identified by Summary + Issue key")
  void detectFormat_jira_issueKey() {
    String[] headers = {"Summary", "Issue key", "Status"};
    assertThat(service.detectFormat(headers)).isEqualTo(ImportSourceFormat.JIRA_CSV);
  }

  @Test
  @DisplayName("detectFormat: Linear CSV identified by Title + Cycle")
  void detectFormat_linear() {
    String[] headers = {"Title", "Status", "Cycle", "Estimate", "Assignee"};
    assertThat(service.detectFormat(headers)).isEqualTo(ImportSourceFormat.LINEAR_CSV);
  }

  @Test
  @DisplayName("detectFormat: Asana CSV identified by Task Name + Section")
  void detectFormat_asana() {
    String[] headers = {"Task Name", "Assignee", "Section", "Due Date"};
    assertThat(service.detectFormat(headers)).isEqualTo(ImportSourceFormat.ASANA_CSV);
  }

  @Test
  @DisplayName("detectFormat: Generic CSV falls back when no known markers present")
  void detectFormat_generic() {
    String[] headers = {"name", "description", "owner", "due"};
    assertThat(service.detectFormat(headers)).isEqualTo(ImportSourceFormat.GENERIC_CSV);
  }

  @Test
  @DisplayName("detectFormat: Asana CSV identified by Name + Assignee Email")
  void detectFormat_asana_assigneeEmail() {
    String[] headers = {"Name", "Assignee Email", "Tags"};
    assertThat(service.detectFormat(headers)).isEqualTo(ImportSourceFormat.ASANA_CSV);
  }

  // ---- Jira status mapping ------------------------------------------------

  @Test
  @DisplayName("mapJiraStatus: 'Done' maps to DONE")
  void jiraStatus_done() {
    assertThat(service.mapJiraStatus("Done")).isEqualTo(TaskStatus.DONE);
  }

  @Test
  @DisplayName("mapJiraStatus: 'In Progress' maps to IN_PROGRESS")
  void jiraStatus_inProgress() {
    assertThat(service.mapJiraStatus("In Progress")).isEqualTo(TaskStatus.IN_PROGRESS);
  }

  @Test
  @DisplayName("mapJiraStatus: 'To Do' maps to TODO")
  void jiraStatus_todo() {
    assertThat(service.mapJiraStatus("To Do")).isEqualTo(TaskStatus.TODO);
  }

  @Test
  @DisplayName("mapJiraStatus: unknown value defaults to TODO")
  void jiraStatus_unknown() {
    assertThat(service.mapJiraStatus("Waiting for review")).isEqualTo(TaskStatus.TODO);
  }

  @Test
  @DisplayName("mapJiraStatus: null defaults to TODO")
  void jiraStatus_null() {
    assertThat(service.mapJiraStatus(null)).isEqualTo(TaskStatus.TODO);
  }

  // ---- Jira priority mapping ----------------------------------------------

  @Test
  @DisplayName("mapJiraPriority: 'Highest' maps to HIGH")
  void jiraPriority_highest() {
    assertThat(service.mapJiraPriority("Highest")).isEqualTo(TaskPriority.HIGH);
  }

  @Test
  @DisplayName("mapJiraPriority: 'High' maps to HIGH")
  void jiraPriority_high() {
    assertThat(service.mapJiraPriority("High")).isEqualTo(TaskPriority.HIGH);
  }

  @Test
  @DisplayName("mapJiraPriority: 'Low' maps to LOW")
  void jiraPriority_low() {
    assertThat(service.mapJiraPriority("Low")).isEqualTo(TaskPriority.LOW);
  }

  @Test
  @DisplayName("mapJiraPriority: 'Lowest' maps to LOW")
  void jiraPriority_lowest() {
    assertThat(service.mapJiraPriority("Lowest")).isEqualTo(TaskPriority.LOW);
  }

  @Test
  @DisplayName("mapJiraPriority: 'Medium' maps to MEDIUM")
  void jiraPriority_medium() {
    assertThat(service.mapJiraPriority("Medium")).isEqualTo(TaskPriority.MEDIUM);
  }

  @Test
  @DisplayName("mapJiraPriority: null defaults to MEDIUM")
  void jiraPriority_null() {
    assertThat(service.mapJiraPriority(null)).isEqualTo(TaskPriority.MEDIUM);
  }

  @Test
  @DisplayName("mapJiraPriority: 'Critical' maps to HIGH")
  void jiraPriority_critical() {
    assertThat(service.mapJiraPriority("Critical")).isEqualTo(TaskPriority.HIGH);
  }

  // ---- Linear status mapping ----------------------------------------------

  @Test
  @DisplayName("mapLinearStatus: 'Done' maps to DONE")
  void linearStatus_done() {
    assertThat(service.mapLinearStatus("Done")).isEqualTo(TaskStatus.DONE);
  }

  @Test
  @DisplayName("mapLinearStatus: 'In Progress' maps to IN_PROGRESS")
  void linearStatus_inProgress() {
    assertThat(service.mapLinearStatus("In Progress")).isEqualTo(TaskStatus.IN_PROGRESS);
  }

  @Test
  @DisplayName("mapLinearStatus: 'Backlog' maps to BACKLOG")
  void linearStatus_backlog() {
    assertThat(service.mapLinearStatus("Backlog")).isEqualTo(TaskStatus.BACKLOG);
  }

  @Test
  @DisplayName("mapLinearStatus: 'Todo' maps to TODO")
  void linearStatus_todo() {
    assertThat(service.mapLinearStatus("Todo")).isEqualTo(TaskStatus.TODO);
  }

  // ---- Asana status mapping -----------------------------------------------

  @Test
  @DisplayName("mapAsanaStatus: 'Done' section maps to DONE")
  void asanaStatus_done() {
    assertThat(service.mapAsanaStatus("Done")).isEqualTo(TaskStatus.DONE);
  }

  @Test
  @DisplayName("mapAsanaStatus: 'In Progress' section maps to IN_PROGRESS")
  void asanaStatus_inProgress() {
    assertThat(service.mapAsanaStatus("In Progress")).isEqualTo(TaskStatus.IN_PROGRESS);
  }

  @Test
  @DisplayName("mapAsanaStatus: unknown section defaults to TODO")
  void asanaStatus_unknown() {
    assertThat(service.mapAsanaStatus("Backlog")).isEqualTo(TaskStatus.TODO);
  }

  // ---- Linear priority mapping --------------------------------------------

  @Test
  @DisplayName("mapLinearPriority: 'Urgent' maps to HIGH")
  void linearPriority_urgent() {
    assertThat(service.mapLinearPriority("Urgent")).isEqualTo(TaskPriority.HIGH);
  }

  @Test
  @DisplayName("mapLinearPriority: 'No priority' maps to LOW")
  void linearPriority_noPriority() {
    assertThat(service.mapLinearPriority("No priority")).isEqualTo(TaskPriority.LOW);
  }

  // ---- Jira bug severity mapping ------------------------------------------

  @Test
  @DisplayName("mapJiraSeverity: 'Blocker' maps to CRITICAL")
  void jiraSeverity_blocker() {
    assertThat(service.mapJiraSeverity("Blocker")).isEqualTo(BugSeverity.CRITICAL);
  }

  @Test
  @DisplayName("mapJiraSeverity: 'Critical' maps to CRITICAL")
  void jiraSeverity_critical() {
    assertThat(service.mapJiraSeverity("Critical")).isEqualTo(BugSeverity.CRITICAL);
  }

  @Test
  @DisplayName("mapJiraSeverity: 'Major' maps to MAJOR")
  void jiraSeverity_major() {
    assertThat(service.mapJiraSeverity("Major")).isEqualTo(BugSeverity.MAJOR);
  }

  @Test
  @DisplayName("mapJiraSeverity: 'Minor' maps to MINOR")
  void jiraSeverity_minor() {
    assertThat(service.mapJiraSeverity("Minor")).isEqualTo(BugSeverity.MINOR);
  }

  @Test
  @DisplayName("mapJiraSeverity: 'Trivial' maps to TRIVIAL")
  void jiraSeverity_trivial() {
    assertThat(service.mapJiraSeverity("Trivial")).isEqualTo(BugSeverity.TRIVIAL);
  }

  @Test
  @DisplayName("mapJiraSeverity: null defaults to MAJOR")
  void jiraSeverity_null() {
    assertThat(service.mapJiraSeverity(null)).isEqualTo(BugSeverity.MAJOR);
  }

  // ---- Jira bug status mapping --------------------------------------------

  @Test
  @DisplayName("mapJiraBugStatus: 'Open' maps to OPEN")
  void jiraBugStatus_open() {
    assertThat(service.mapJiraBugStatus("Open")).isEqualTo(BugStatus.OPEN);
  }

  @Test
  @DisplayName("mapJiraBugStatus: 'In Progress' maps to IN_PROGRESS")
  void jiraBugStatus_inProgress() {
    assertThat(service.mapJiraBugStatus("In Progress")).isEqualTo(BugStatus.IN_PROGRESS);
  }

  @Test
  @DisplayName("mapJiraBugStatus: 'Resolved' maps to RESOLVED")
  void jiraBugStatus_resolved() {
    assertThat(service.mapJiraBugStatus("Resolved")).isEqualTo(BugStatus.RESOLVED);
  }

  @Test
  @DisplayName("mapJiraBugStatus: 'Won't Fix' maps to WONT_FIX")
  void jiraBugStatus_wontFix() {
    assertThat(service.mapJiraBugStatus("Won't Fix")).isEqualTo(BugStatus.WONT_FIX);
  }

  @Test
  @DisplayName("mapJiraBugStatus: null defaults to OPEN")
  void jiraBugStatus_null() {
    assertThat(service.mapJiraBugStatus(null)).isEqualTo(BugStatus.OPEN);
  }
}
