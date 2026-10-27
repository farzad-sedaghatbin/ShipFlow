package com.github.farzadsedaghatbin.shipflow.service.mcp.server;

import com.github.farzadsedaghatbin.shipflow.dto.mcp.McpCycleDTO;
import com.github.farzadsedaghatbin.shipflow.dto.mcp.McpPitchDTO;
import com.github.farzadsedaghatbin.shipflow.dto.mcp.McpProjectDTO;
import com.github.farzadsedaghatbin.shipflow.dto.mcp.McpTaskDTO;
import com.github.farzadsedaghatbin.shipflow.entity.ApiKey;
import com.github.farzadsedaghatbin.shipflow.entity.BugReport;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Epic;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.Task;
import com.github.farzadsedaghatbin.shipflow.entity.TestCase;
import com.github.farzadsedaghatbin.shipflow.entity.UploadedDocument;
import com.github.farzadsedaghatbin.shipflow.repository.BugReportRepository;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.repository.EpicRepository;
import com.github.farzadsedaghatbin.shipflow.repository.InitiativeRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TaskRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TestCaseRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UploadedDocumentRepository;
import com.github.farzadsedaghatbin.shipflow.service.mcp.server.tools.BugReportMcpTools;
import com.github.farzadsedaghatbin.shipflow.service.mcp.server.tools.CommentMcpTools;
import com.github.farzadsedaghatbin.shipflow.service.mcp.server.tools.CycleMcpTools;
import com.github.farzadsedaghatbin.shipflow.service.mcp.server.tools.IdentityMcpTools;
import com.github.farzadsedaghatbin.shipflow.service.mcp.server.tools.PitchMcpTools;
import com.github.farzadsedaghatbin.shipflow.service.mcp.server.tools.ProjectMcpTools;
import com.github.farzadsedaghatbin.shipflow.service.mcp.server.tools.RoadmapMcpTools;
import com.github.farzadsedaghatbin.shipflow.service.mcp.server.tools.TaskContextMcpTools;
import com.github.farzadsedaghatbin.shipflow.service.mcp.server.tools.TaskMcpTools;
import com.github.farzadsedaghatbin.shipflow.service.mcp.server.tools.TestCaseMcpTools;
import com.github.farzadsedaghatbin.shipflow.service.mcp.server.tools.WiseArchitectureMcpTools;
import com.github.farzadsedaghatbin.shipflow.service.mcp.server.tools.WorkContextMcpTools;
import com.github.farzadsedaghatbin.shipflow.service.mcp.server.tools.WorklogMcpTools;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Applies an API key's single-project restriction ({@link ApiKey#getRestrictedToProjectId()}) to
 * MCP tool calls — the MCP counterpart of {@code PublicApiAuthorizationService}, which only covers
 * {@code /api/v1/public/**}.
 *
 * <p>{@link McpToolDispatcher} calls {@link #authorize} before every tool and {@link
 * #filterResult} on its output. Both are no-ops for an unrestricted (or absent) key, so existing
 * behaviour is unchanged. For a restricted key:
 *
 * <ul>
 *   <li><b>Entity ids</b> (task, pitch, cycle, bug, test case, epic, attachment) are resolved to
 *       their project and must belong to the restricted project.
 *   <li><b>List tools</b> called without a {@code projectId} are narrowed to the restricted
 *       project; a different {@code projectId} is refused.
 *   <li><b>Writes</b> carrying a foreign project/cycle/pitch/task reference are refused.
 *   <li><b>Unknown tools fail closed</b>: a tool with no rule here is refused for restricted keys,
 *       so a newly added tool can never silently bypass the restriction. Add a case to {@link
 *       #authorize} when adding a tool.
 * </ul>
 *
 * Every denial is the same generic "not found" error, whether or not the entity exists, so a
 * restricted key cannot probe for ids in other projects.
 */
@Component
@RequiredArgsConstructor
public class McpProjectScopeGuard {

  static final String DENIED = "Resource not found or not accessible with this API key";

  private final TaskRepository taskRepository;
  private final PitchRepository pitchRepository;
  private final CycleRepository cycleRepository;
  private final BugReportRepository bugReportRepository;
  private final TestCaseRepository testCaseRepository;
  private final EpicRepository epicRepository;
  private final UploadedDocumentRepository documentRepository;
  private final InitiativeRepository initiativeRepository;

  /** The project the key is restricted to, or {@code null} when unrestricted / no key. */
  public static Long restrictedProjectId(ApiKey apiKey) {
    return apiKey != null ? apiKey.getRestrictedToProjectId() : null;
  }

  /**
   * Validates (and where needed rewrites) the tool arguments for a restricted key.
   *
   * @return the arguments to run the tool with — the same map when nothing changed
   * @throws McpToolDispatcher.McpToolException if the call touches another project
   */
  @Transactional(readOnly = true)
  public Map<String, Object> authorize(String tool, Map<String, Object> args, ApiKey apiKey) {
    Long restricted = restrictedProjectId(apiKey);
    if (restricted == null) {
      return args;
    }
    Check c = new Check(restricted, args);
    switch (tool) {
      case IdentityMcpTools.TOOL_WHOAMI, ProjectMcpTools.TOOL_LIST_PROJECTS -> {
        // no project-scoped input; list_projects is narrowed in filterResult
      }
      case ProjectMcpTools.TOOL_GET_PROJECT -> c.project("projectId");
      case CycleMcpTools.TOOL_GET_CYCLES -> c.scopeList();
      case CycleMcpTools.TOOL_GET_CYCLE -> c.cycle("cycleId");
      case TaskMcpTools.TOOL_GET_TASKS, TaskMcpTools.TOOL_GET_BLOCKERS -> {
        c.cycle("cycleId");
        c.pitch("pitchId");
        c.scopeList();
      }
      case TaskMcpTools.TOOL_GET_TASK,
          TaskMcpTools.TOOL_UPDATE_TASK,
          TaskMcpTools.TOOL_UPDATE_TASK_STATUS,
          TaskMcpTools.TOOL_UPDATE_TASK_ASSIGNEE,
          TaskContextMcpTools.TOOL_GET_TASK_CONTEXT -> c.task("taskId");
      case TaskMcpTools.TOOL_CREATE_TASK -> {
        c.project("projectId");
        c.cycle("cycleId");
        c.pitch("pitchId");
        c.task("parentTaskId");
      }
      case PitchMcpTools.TOOL_GET_PITCHES -> {
        c.cycle("cycleId");
        c.scopeList();
      }
      case PitchMcpTools.TOOL_GET_BETTING_CANDIDATES -> c.scopeList();
      case PitchMcpTools.TOOL_GET_PITCH,
          PitchMcpTools.TOOL_UPDATE_PITCH,
          PitchMcpTools.TOOL_UPDATE_PITCH_STATUS -> c.pitch("pitchId");
      case PitchMcpTools.TOOL_CREATE_PITCH -> {
        // create_pitch has no projectId argument: the pitch's project comes from its epic, so a
        // restricted key must name an epic inside its own project.
        if (args.get("epicId") == null) {
          throw denied();
        }
        c.epic("epicId");
      }
      case CommentMcpTools.TOOL_ADD_COMMENT, CommentMcpTools.TOOL_GET_COMMENTS -> c.commentTarget();
      case WorklogMcpTools.TOOL_LOG_WORK -> {
        c.task("taskId");
        c.pitch("pitchId");
      }
      case WiseArchitectureMcpTools.TOOL_LIST_ANALYSES -> {
        // Without a pitchId this lists the user's analyses across every project.
        if (args.get("pitchId") == null) {
          throw denied();
        }
        c.pitch("pitchId");
      }
      case WiseArchitectureMcpTools.TOOL_ANALYZE -> c.pitch("pitchId");
      // An analysis conversation id can't be resolved to a project.
      case WiseArchitectureMcpTools.TOOL_GET_FILES -> throw denied();
      case WorkContextMcpTools.TOOL_GET_WORK_CONTEXT -> {
        c.pitch("pitchId");
        c.cycle("cycleId");
        c.task("taskId");
      }
      case TestCaseMcpTools.TOOL_GET_TEST_CASES -> {
        c.task("taskId");
        c.pitch("pitchId");
        c.cycle("cycleId");
      }
      case TestCaseMcpTools.TOOL_GET_TEST_CASE,
          TestCaseMcpTools.TOOL_GET_TEST_RUNS,
          TestCaseMcpTools.TOOL_RECORD_TEST_RUN -> c.testCase("testCaseId");
      case BugReportMcpTools.TOOL_GET_BUG_REPORTS -> {
        c.task("taskId");
        c.pitch("pitchId");
        c.cycle("cycleId");
        c.scopeList();
      }
      case BugReportMcpTools.TOOL_GET_BUG_REPORT,
          BugReportMcpTools.TOOL_GET_BUG_ATTACHMENTS,
          BugReportMcpTools.TOOL_UPDATE_BUG_STATUS,
          BugReportMcpTools.TOOL_UPDATE_BUG_ASSIGNEE -> c.bug(args.get("bugKey"), args.get("bugReportId"));
      case BugReportMcpTools.TOOL_DOWNLOAD_BUG_ATTACHMENT -> c.attachment("attachmentId");
      case BugReportMcpTools.TOOL_CREATE_BUG -> {
        c.task("taskId");
        c.pitch("pitchId");
        c.cycle("cycleId");
        c.scopeList();
      }
      // Roadmap tools also accept snake_case aliases (project_id, epic_id, ...); fold those into the
      // camelCase keys first so an alias can't slip past the checks below.
      case RoadmapMcpTools.TOOL_GET_ROADMAP,
          RoadmapMcpTools.TOOL_GET_EPICS,
          RoadmapMcpTools.TOOL_CREATE_INITIATIVE -> {
        c.foldAliases();
        c.scopeList();
      }
      case RoadmapMcpTools.TOOL_CREATE_EPIC -> {
        c.foldAliases();
        c.scopeList();
        c.initiative("initiativeId");
      }
      case RoadmapMcpTools.TOOL_UPDATE_EPIC -> {
        c.foldAliases();
        c.requirePresent("epicId");
        c.epic("epicId");
        c.initiative("initiativeId");
      }
      case RoadmapMcpTools.TOOL_ADD_EPIC_DEPENDENCY -> {
        c.foldAliases();
        c.requirePresent("epicId");
        c.requirePresent("targetEpicId");
        c.epic("epicId");
        c.epic("targetEpicId");
      }
      case RoadmapMcpTools.TOOL_LINK_TASK_TO_EPIC -> {
        c.foldAliases();
        c.requirePresent("taskId");
        c.task("taskId");
        c.epic("epicId");
      }
      default -> throw denied();
    }
    return c.args;
  }

  /**
   * Defence in depth for list results: drops rows whose own {@code projectId} names another
   * project. Rows with no resolvable project id are kept — {@link #authorize} already scoped the
   * query, this only catches anything that slipped through.
   */
  public Object filterResult(Object result, ApiKey apiKey) {
    Long restricted = restrictedProjectId(apiKey);
    if (restricted == null || !(result instanceof List<?> list)) {
      return result;
    }
    return list.stream().filter(row -> rowAllowed(row, restricted)).toList();
  }

  private static boolean rowAllowed(Object row, Long restricted) {
    Long projectId =
        switch (row) {
          case McpProjectDTO p -> p.getId();
          case McpCycleDTO cy -> cy.getProjectId();
          case McpTaskDTO t -> t.getProjectId();
          case McpPitchDTO p -> p.getProjectId();
          default -> null;
        };
    if (row instanceof McpProjectDTO) {
      return restricted.equals(projectId);
    }
    return projectId == null || restricted.equals(projectId);
  }

  private static McpToolDispatcher.McpToolException denied() {
    return new McpToolDispatcher.McpToolException(DENIED);
  }

  // ── Entity → project resolution ───────────────────────────────────────────

  private Long projectOfCycle(Cycle cycle) {
    return cycle != null && cycle.getProject() != null ? cycle.getProject().getId() : null;
  }

  private Long projectOfPitch(Pitch pitch) {
    if (pitch.getProject() != null) {
      return pitch.getProject().getId();
    }
    Long viaCycle = projectOfCycle(pitch.getCycle());
    if (viaCycle != null) {
      return viaCycle;
    }
    Epic epic = pitch.getEpic();
    return epic != null && epic.getProject() != null ? epic.getProject().getId() : null;
  }

  private Long projectOfTask(Task task) {
    if (task.getProject() != null) {
      return task.getProject().getId();
    }
    Long viaCycle = projectOfCycle(task.getCycle());
    if (viaCycle != null) {
      return viaCycle;
    }
    return task.getPitch() != null ? projectOfPitch(task.getPitch()) : null;
  }

  private Long projectOfBug(BugReport bug) {
    if (bug.getProject() != null) {
      return bug.getProject().getId();
    }
    Long viaCycle = projectOfCycle(bug.getCycle());
    if (viaCycle != null) {
      return viaCycle;
    }
    return bug.getPitch() != null ? projectOfPitch(bug.getPitch()) : null;
  }

  private Long projectOfTestCase(TestCase testCase) {
    if (testCase.getProject() != null) {
      return testCase.getProject().getId();
    }
    Long viaCycle = projectOfCycle(testCase.getCycle());
    if (viaCycle != null) {
      return viaCycle;
    }
    return testCase.getPitch() != null ? projectOfPitch(testCase.getPitch()) : null;
  }

  /** Per-call helper: verifies each argument that is present against the restricted project. */
  private final class Check {
    private final Long restricted;
    private Map<String, Object> args;

    Check(Long restricted, Map<String, Object> args) {
      this.restricted = restricted;
      this.args = args;
    }

    private void require(Long resourceProjectId) {
      if (resourceProjectId == null || !restricted.equals(resourceProjectId)) {
        throw denied();
      }
    }

    private <T> void entity(String key, Function<Long, java.util.Optional<T>> load,
        Function<T, Long> projectOf) {
      Long id = toLong(args.get(key));
      if (id == null) {
        return;
      }
      require(load.apply(id).map(projectOf).orElse(null));
    }

    void project(String key) {
      Long id = toLong(args.get(key));
      if (id != null) {
        require(id);
      }
    }

    void cycle(String key) {
      entity(key, cycleRepository::findByIdWithProject, McpProjectScopeGuard.this::projectOfCycle);
    }

    void pitch(String key) {
      entity(key, pitchRepository::findByIdNotDeleted, McpProjectScopeGuard.this::projectOfPitch);
    }

    void task(String key) {
      entity(key, taskRepository::findByIdNotDeleted, McpProjectScopeGuard.this::projectOfTask);
    }

    void testCase(String key) {
      entity(key, testCaseRepository::findByIdNotDeleted,
          McpProjectScopeGuard.this::projectOfTestCase);
    }

    void epic(String key) {
      entity(key, epicRepository::findByIdNotDeleted,
          e -> e.getProject() != null ? e.getProject().getId() : null);
    }

    void initiative(String key) {
      entity(key, initiativeRepository::findByIdNotDeleted,
          i -> i.getProject() != null ? i.getProject().getId() : null);
    }

    /** Refuses the call when an id the tool needs (and this guard must check) is missing. */
    void requirePresent(String key) {
      if (args.get(key) == null) {
        throw denied();
      }
    }

    /**
     * Copies each snake_case argument (e.g. {@code epic_id}) to its camelCase key and drops the
     * alias, so the checks and the tool see one value. A camelCase key, when present, wins — the
     * same precedence the roadmap tools use.
     */
    void foldAliases() {
      Map<String, Object> folded = new HashMap<>();
      args.forEach((k, v) -> {
        if (k.indexOf('_') >= 0) {
          folded.putIfAbsent(snakeToCamel(k), v);
        }
      });
      args.forEach((k, v) -> {
        if (k.indexOf('_') < 0) {
          folded.put(k, v);
        }
      });
      args = folded;
    }

    void attachment(String key) {
      Long id = toLong(args.get(key));
      if (id == null) {
        return;
      }
      UploadedDocument doc = documentRepository.findById(id).orElse(null);
      if (doc == null || !"BUG_REPORT".equals(doc.getEntityType()) || doc.getEntityId() == null) {
        throw denied();
      }
      require(bugReportRepository.findById(doc.getEntityId())
          .map(McpProjectScopeGuard.this::projectOfBug).orElse(null));
    }

    /** A bug addressed by {@code bugKey} or by a numeric (or key-shaped) {@code bugReportId}. */
    void bug(Object keyArg, Object idArg) {
      java.util.Optional<BugReport> bug;
      if (keyArg instanceof String key && !key.isBlank()) {
        bug = bugReportRepository.findByBugKey(key.trim());
      } else if (idArg instanceof String s && !s.isBlank() && toLongOrNull(s.trim()) == null) {
        bug = bugReportRepository.findByBugKey(s.trim());
      } else {
        Long id = toLong(idArg);
        bug = id == null ? java.util.Optional.empty() : bugReportRepository.findById(id);
      }
      require(bug.map(McpProjectScopeGuard.this::projectOfBug).orElse(null));
    }

    /** Comments target a task or a bug; wiki pages and anything else are refused. */
    void commentTarget() {
      String type = args.get("entityType") == null ? "" : args.get("entityType").toString().trim().toUpperCase();
      switch (type) {
        case "TASK" -> task("entityId");
        case "BUG_REPORT", "BUG" -> bug(args.get("bugKey"), args.get("entityId"));
        default -> throw denied();
      }
    }

    /**
     * For list tools: a supplied {@code projectId} must be the restricted one; an absent one is
     * filled in with it, so the query itself is narrowed.
     */
    void scopeList() {
      Long id = toLong(args.get("projectId"));
      if (id != null) {
        require(id);
        return;
      }
      Map<String, Object> narrowed = new HashMap<>(args);
      narrowed.put("projectId", restricted);
      args = narrowed;
    }
  }

  private static String snakeToCamel(String key) {
    StringBuilder sb = new StringBuilder(key.length());
    boolean upper = false;
    for (char ch : key.toCharArray()) {
      if (ch == '_') {
        upper = true;
      } else {
        sb.append(upper ? Character.toUpperCase(ch) : ch);
        upper = false;
      }
    }
    return sb.toString();
  }

  private static Long toLong(Object val) {
    if (val == null) {
      return null;
    }
    if (val instanceof Number n) {
      return n.longValue();
    }
    Long parsed = toLongOrNull(val.toString().trim());
    if (parsed == null) {
      throw new IllegalArgumentException("Argument must be a number, got: " + val);
    }
    return parsed;
  }

  private static Long toLongOrNull(String s) {
    try {
      return Long.parseLong(s);
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
