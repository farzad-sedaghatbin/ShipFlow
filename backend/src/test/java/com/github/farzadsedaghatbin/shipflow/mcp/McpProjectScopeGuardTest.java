package com.github.farzadsedaghatbin.shipflow.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;

import com.github.farzadsedaghatbin.shipflow.dto.mcp.McpPitchDTO;
import com.github.farzadsedaghatbin.shipflow.dto.mcp.McpProjectDTO;
import com.github.farzadsedaghatbin.shipflow.dto.mcp.McpTaskDTO;
import com.github.farzadsedaghatbin.shipflow.entity.ApiKey;
import com.github.farzadsedaghatbin.shipflow.entity.BugReport;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Epic;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.entity.Task;
import com.github.farzadsedaghatbin.shipflow.entity.TestCase;
import com.github.farzadsedaghatbin.shipflow.entity.UploadedDocument;
import com.github.farzadsedaghatbin.shipflow.repository.BugReportRepository;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.repository.EpicRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TaskRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TestCaseRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UploadedDocumentRepository;
import com.github.farzadsedaghatbin.shipflow.service.mcp.server.McpProjectScopeGuard;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Per-tool coverage of the project-restriction guard. Project 1 is the key's own project, project 2
 * is foreign. Entity ids: own = 10, foreign = 20.
 */
@ExtendWith(MockitoExtension.class)
class McpProjectScopeGuardTest {

  private static final long OWN = 1L;
  private static final long FOREIGN = 2L;

  @Mock private TaskRepository taskRepository;
  @Mock private PitchRepository pitchRepository;
  @Mock private CycleRepository cycleRepository;
  @Mock private BugReportRepository bugReportRepository;
  @Mock private TestCaseRepository testCaseRepository;
  @Mock private EpicRepository epicRepository;
  @Mock private com.github.farzadsedaghatbin.shipflow.repository.InitiativeRepository initiativeRepository;
  @Mock private UploadedDocumentRepository documentRepository;

  private McpProjectScopeGuard guard;
  private final ApiKey restricted = ApiKey.builder().restrictedToProjectId(OWN).build();
  private final ApiKey unrestricted = ApiKey.builder().build();

  private static Project project(long id) {
    return Project.builder().id(id).build();
  }

  @BeforeEach
  void setUp() {
    guard =
        new McpProjectScopeGuard(
            taskRepository, pitchRepository, cycleRepository, bugReportRepository,
            testCaseRepository, epicRepository, documentRepository, initiativeRepository);

    for (long id : new long[] {10, 20}) {
      Project p = project(id == 10 ? OWN : FOREIGN);
      Task task = new Task();
      task.setId(id);
      task.setProject(p);
      lenient().when(taskRepository.findByIdNotDeleted(id)).thenReturn(Optional.of(task));

      Pitch pitch = new Pitch();
      pitch.setId(id);
      pitch.setProject(p);
      lenient().when(pitchRepository.findByIdNotDeleted(id)).thenReturn(Optional.of(pitch));

      Cycle cycle = new Cycle();
      cycle.setId(id);
      cycle.setProject(p);
      lenient().when(cycleRepository.findByIdWithProject(id)).thenReturn(Optional.of(cycle));

      BugReport bug = new BugReport();
      bug.setId(id);
      bug.setBugKey("BUG-" + id);
      bug.setProject(p);
      lenient().when(bugReportRepository.findById(id)).thenReturn(Optional.of(bug));
      lenient().when(bugReportRepository.findByBugKey("BUG-" + id)).thenReturn(Optional.of(bug));

      TestCase tc = new TestCase();
      tc.setId(id);
      tc.setProject(p);
      lenient().when(testCaseRepository.findByIdNotDeleted(id)).thenReturn(Optional.of(tc));

      Epic epic = new Epic();
      epic.setId(id);
      epic.setProject(p);
      lenient().when(epicRepository.findByIdNotDeleted(id)).thenReturn(Optional.of(epic));

      com.github.farzadsedaghatbin.shipflow.entity.Initiative initiative =
          new com.github.farzadsedaghatbin.shipflow.entity.Initiative();
      initiative.setId(id);
      initiative.setProject(p);
      lenient().when(initiativeRepository.findByIdNotDeleted(id)).thenReturn(Optional.of(initiative));

      UploadedDocument doc = new UploadedDocument();
      doc.setEntityType("BUG_REPORT");
      doc.setEntityId(id);
      lenient().when(documentRepository.findById(id)).thenReturn(Optional.of(doc));
    }
  }

  private void assertDenied(String tool, Map<String, Object> args) {
    assertThatThrownBy(() -> guard.authorize(tool, args, restricted))
        .hasMessageContaining("not found or not accessible");
  }

  private void assertAllowed(String tool, Map<String, Object> args) {
    assertThat(guard.authorize(tool, args, restricted)).isNotNull();
  }

  /** Every tool that takes one entity id: own is allowed, foreign and unknown ids are denied. */
  @Test
  void entityIdTools_allowOwnProject_denyForeignAndUnknown() {
    Map<String, String> toolToArg =
        Map.ofEntries(
            Map.entry("get_task", "taskId"),
            Map.entry("update_task", "taskId"),
            Map.entry("update_task_status", "taskId"),
            Map.entry("update_task_assignee", "taskId"),
            Map.entry("get_task_context", "taskId"),
            Map.entry("get_pitch_detail", "pitchId"),
            Map.entry("update_pitch", "pitchId"),
            Map.entry("update_pitch_status", "pitchId"),
            Map.entry("get_cycle", "cycleId"),
            Map.entry("get_test_case", "testCaseId"),
            Map.entry("get_test_runs", "testCaseId"),
            Map.entry("record_test_run", "testCaseId"),
            Map.entry("get_bug_report", "bugReportId"),
            Map.entry("get_bug_attachments", "bugReportId"),
            Map.entry("update_bug_status", "bugReportId"),
            Map.entry("update_bug_assignee", "bugReportId"),
            Map.entry("download_bug_attachment", "attachmentId"),
            Map.entry("wise_architecture_analyze", "pitchId"));
    toolToArg.forEach(
        (tool, arg) -> {
          assertAllowed(tool, Map.of(arg, 10));
          assertDenied(tool, Map.of(arg, 20));
          assertDenied(tool, Map.of(arg, 999));
        });
  }

  @Test
  void bugKey_isResolvedToItsProject() {
    assertAllowed("get_bug_report", Map.of("bugKey", "BUG-10"));
    assertDenied("get_bug_report", Map.of("bugKey", "BUG-20"));
    assertDenied("get_bug_report", Map.of("bugKey", "BUG-404"));
  }

  @Test
  void projectIdTools_requireTheRestrictedProject() {
    assertAllowed("get_project", Map.of("projectId", OWN));
    assertDenied("get_project", Map.of("projectId", FOREIGN));
    assertDenied("create_task", Map.of("projectId", FOREIGN, "title", "x"));
    assertDenied("create_bug", Map.of("projectId", FOREIGN, "title", "x"));
    assertDenied("create_task", Map.of("cycleId", 20, "title", "x"));
    assertDenied("create_task", Map.of("parentTaskId", 20, "projectId", OWN, "title", "x"));
    assertAllowed("create_task", Map.of("cycleId", 10, "title", "x"));
  }

  @Test
  void listTools_withoutProject_areNarrowedToTheRestrictedProject() {
    for (String tool :
        List.of("get_cycles", "get_tasks", "get_blockers", "get_pitches",
            "get_betting_candidates", "get_bug_reports")) {
      Map<String, Object> out = guard.authorize(tool, Map.of("mine", true), restricted);
      assertThat(out).containsEntry("projectId", OWN).containsEntry("mine", true);
      assertDenied(tool, Map.of("projectId", FOREIGN));
    }
  }

  @Test
  void listTools_withForeignCyclePitchOrTask_areDenied() {
    assertDenied("get_tasks", Map.of("cycleId", 20));
    assertDenied("get_tasks", Map.of("pitchId", 20));
    assertDenied("get_pitches", Map.of("cycleId", 20));
    assertDenied("get_bug_reports", Map.of("taskId", 20));
    assertDenied("get_test_cases", Map.of("taskId", 20));
    assertDenied("get_test_cases", Map.of("pitchId", 20));
    assertDenied("get_test_cases", Map.of("cycleId", 20));
    assertAllowed("get_test_cases", Map.of("taskId", 10));
  }

  @Test
  void comments_andWorklogs_checkTheirTarget() {
    assertAllowed("add_comment", Map.of("entityType", "TASK", "entityId", 10));
    assertDenied("add_comment", Map.of("entityType", "TASK", "entityId", 20));
    assertDenied("get_comments", Map.of("entityType", "BUG_REPORT", "entityId", 20));
    assertDenied("get_comments", Map.of("entityType", "BUG", "bugKey", "BUG-20"));
    assertAllowed("get_comments", Map.of("entityType", "BUG_REPORT", "bugKey", "BUG-10"));
    assertDenied("add_comment", Map.of("entityType", "WIKI_PAGE", "entityId", 10));
    assertDenied("log_work", Map.of("taskId", 20));
    assertDenied("log_work", Map.of("pitchId", 20));
    assertAllowed("log_work", Map.of("taskId", 10));
  }

  @Test
  void workContext_checksEveryScopeGiven() {
    assertDenied("get_work_context", Map.of("pitchId", 20));
    assertDenied("get_work_context", Map.of("cycleId", 20));
    assertDenied("get_work_context", Map.of("taskId", 20));
    assertAllowed("get_work_context", Map.of("pitchId", 10));
  }

  @Test
  void createPitch_requiresAnEpicInTheRestrictedProject() {
    assertDenied("create_pitch", Map.of("title", "x"));
    assertDenied("create_pitch", Map.of("title", "x", "epicId", 20));
    assertAllowed("create_pitch", Map.of("title", "x", "epicId", 10));
  }

  @Test
  void wiseArchitecture_isScopedByPitch_andConversationLookupIsRefused() {
    assertDenied("wise_architecture_list_analyses", Map.of());
    assertDenied("wise_architecture_list_analyses", Map.of("pitchId", 20));
    assertAllowed("wise_architecture_list_analyses", Map.of("pitchId", 10));
    assertDenied("wise_architecture_get_files", Map.of("conversationId", "abc"));
  }

  @Test
  void unknownTool_failsClosedForRestrictedKeys() {
    assertDenied("some_future_tool", Map.of());
  }

  @Test
  void whoami_andListProjects_areAlwaysAllowed() {
    assertAllowed("whoami", Map.of());
    assertAllowed("list_projects", Map.of());
  }

  @Test
  void unrestrictedOrAbsentKey_isUntouched() {
    Map<String, Object> args = Map.of("taskId", 20);
    assertThat(guard.authorize("get_task", args, unrestricted)).isSameAs(args);
    assertThat(guard.authorize("get_task", args, null)).isSameAs(args);
    assertThat(guard.authorize("some_future_tool", args, unrestricted)).isSameAs(args);
    assertThat(guard.authorize("get_tasks", Map.of("mine", true), unrestricted))
        .doesNotContainKey("projectId");
    org.mockito.Mockito.verifyNoInteractions(taskRepository);
  }

  @Test
  void taskWithoutDirectProject_resolvesThroughItsCycle() {
    Cycle cycle = new Cycle();
    cycle.setProject(project(FOREIGN));
    Task task = new Task();
    task.setId(30L);
    task.setCycle(cycle);
    lenient().when(taskRepository.findByIdNotDeleted(30L)).thenReturn(Optional.of(task));
    assertDenied("get_task", Map.of("taskId", 30));

    cycle.setProject(project(OWN));
    assertAllowed("get_task", Map.of("taskId", 30));
  }

  @Test
  void orphanTask_isDeniedForRestrictedKey() {
    Task task = new Task();
    task.setId(31L);
    lenient().when(taskRepository.findByIdNotDeleted(31L)).thenReturn(Optional.of(task));
    assertDenied("get_task", Map.of("taskId", 31));
  }

  @Test
  void filterResult_dropsRowsFromOtherProjects() {
    List<Object> rows =
        List.of(
            McpProjectDTO.builder().id(OWN).build(),
            McpProjectDTO.builder().id(FOREIGN).build());
    assertThat((List<?>) guard.filterResult(rows, restricted)).hasSize(1);
    assertThat((List<?>) guard.filterResult(rows, unrestricted)).hasSize(2);

    List<Object> tasks =
        List.of(
            McpTaskDTO.builder().projectId(OWN).build(),
            McpTaskDTO.builder().projectId(FOREIGN).build(),
            McpTaskDTO.builder().build());
    assertThat((List<?>) guard.filterResult(tasks, restricted)).hasSize(2);

    List<Object> pitches =
        List.of(
            McpPitchDTO.builder().projectId(FOREIGN).build(),
            McpPitchDTO.builder().projectId(OWN).build());
    assertThat((List<?>) guard.filterResult(pitches, restricted)).hasSize(1);
  }

  // ── Roadmap tools ─────────────────────────────────────────────────────────

  @Test
  void roadmapListTools_areNarrowedToTheRestrictedProject() {
    for (String tool : List.of("get_roadmap", "get_epics", "create_initiative")) {
      assertThat(guard.authorize(tool, Map.of(), restricted)).containsEntry("projectId", OWN);
      assertDenied(tool, Map.of("projectId", FOREIGN));
    }
  }

  @Test
  void createEpic_deniesAForeignProjectOrInitiative() {
    assertAllowed("create_epic", Map.of("projectId", OWN, "name", "x", "initiativeId", 10));
    assertDenied("create_epic", Map.of("projectId", FOREIGN, "name", "x"));
    assertDenied("create_epic", Map.of("projectId", OWN, "name", "x", "initiativeId", 20));
  }

  @Test
  void epicWriteTools_checkEveryEpicTaskAndInitiative() {
    assertAllowed("update_epic", Map.of("epicId", 10, "name", "x"));
    assertDenied("update_epic", Map.of("epicId", 20, "name", "x"));
    assertDenied("update_epic", Map.of("epicId", 10, "initiativeId", 20));
    assertDenied("update_epic", Map.of("name", "x"));

    assertAllowed("add_epic_dependency", Map.of("epicId", 10, "targetEpicId", 10));
    assertDenied("add_epic_dependency", Map.of("epicId", 10, "targetEpicId", 20));
    assertDenied("add_epic_dependency", Map.of("epicId", 20, "targetEpicId", 10));

    assertAllowed("link_task_to_epic", Map.of("taskId", 10, "epicId", 10));
    assertDenied("link_task_to_epic", Map.of("taskId", 10, "epicId", 20));
    assertDenied("link_task_to_epic", Map.of("taskId", 20, "epicId", 10));
  }

  /** The roadmap tools accept snake_case aliases; an alias must not bypass the checks. */
  @Test
  void roadmapTools_snakeCaseAliasesCannotBypassTheRestriction() {
    assertDenied("update_epic", Map.of("epic_id", 20, "name", "x"));
    assertDenied("add_epic_dependency", Map.of("epic_id", 10, "target_epic_id", 20));
    assertDenied("link_task_to_epic", Map.of("task_id", 10, "epic_id", 20));
    assertDenied("create_epic", Map.of("project_id", FOREIGN, "name", "x"));
    assertDenied("create_epic", Map.of("projectId", OWN, "name", "x", "initiative_id", 20));

    Map<String, Object> folded = guard.authorize("update_epic", Map.of("epic_id", 10), restricted);
    assertThat(folded).containsEntry("epicId", 10).doesNotContainKey("epic_id");
  }

  @Test
  void roadmapTools_unrestrictedKeyIsUntouched() {
    Map<String, Object> args = Map.of("epic_id", 20, "name", "x");
    assertThat(guard.authorize("update_epic", args, unrestricted)).isSameAs(args);
  }
}
