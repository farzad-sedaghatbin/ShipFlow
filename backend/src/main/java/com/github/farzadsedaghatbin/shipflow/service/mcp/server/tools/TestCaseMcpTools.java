package com.github.farzadsedaghatbin.shipflow.service.mcp.server.tools;

import com.github.farzadsedaghatbin.shipflow.dto.mcp.McpTestCaseDTO;
import com.github.farzadsedaghatbin.shipflow.dto.mcp.McpTestRunDTO;
import com.github.farzadsedaghatbin.shipflow.dto.qa.BulkCreateTestCaseRequest;
import com.github.farzadsedaghatbin.shipflow.dto.qa.CreateTestCaseRequest;
import com.github.farzadsedaghatbin.shipflow.dto.qa.CreateTestRunRequest;
import com.github.farzadsedaghatbin.shipflow.dto.qa.TestRunDTO;
import com.github.farzadsedaghatbin.shipflow.dto.qa.UpdateTestCaseRequest;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TestCasePriority;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TestCaseStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TestCaseType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TestRunStatus;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import com.github.farzadsedaghatbin.shipflow.service.TestCaseService;
import com.github.farzadsedaghatbin.shipflow.service.TestRunService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * MCP tools for the QA domain — test cases and their runs.
 *
 * <p>Exposes the acceptance criteria attached to a task or pitch as MCP-readable data, and lets an
 * agent record the outcome of running a test. This is what makes "implement and verify against the
 * defined acceptance criteria" a first-class agent capability instead of guesswork.
 */
@Component
@RequiredArgsConstructor
public class TestCaseMcpTools {

  public static final String TOOL_GET_TEST_CASES = "get_test_cases";
  public static final String TOOL_GET_TEST_CASE = "get_test_case";
  public static final String TOOL_GET_TEST_RUNS = "get_test_runs";
  public static final String TOOL_RECORD_TEST_RUN = "record_test_run";
  public static final String TOOL_CREATE_TEST_CASE = "create_test_case";
  public static final String TOOL_CREATE_TEST_CASES = "create_test_cases";
  public static final String TOOL_UPDATE_TEST_CASE = "update_test_case";
  public static final String TOOL_DELETE_TEST_CASE = "delete_test_case";

  private final TestCaseService testCaseService;
  private final TestRunService testRunService;
  private final UserRepository userRepository;

  // ── Tool definitions ──────────────────────────────────────────────────────

  public static Map<String, Object> getTestCasesDefinition() {
    return Map.of(
        "name", TOOL_GET_TEST_CASES,
        "description",
            "List test cases (acceptance criteria) for a task, pitch, cycle, or a whole project. "
                + "Returns preconditions, steps, and expectedResult for each — the criteria an "
                + "implementation must satisfy. One scope is required: taskId, pitchId, cycleId, "
                + "or projectId. Optional status filter (DRAFT, READY, APPROVED, DEPRECATED, "
                + "ARCHIVED) applies to every scope.",
        "inputSchema",
            Map.of(
                "type", "object",
                "properties",
                    Map.of(
                        "taskId",
                        Map.of(
                            "type", "integer",
                            "description", "Test cases linked to this task"),
                        "pitchId",
                        Map.of(
                            "type", "integer",
                            "description", "Test cases linked to this pitch"),
                        "cycleId",
                        Map.of(
                            "type", "integer",
                            "description", "All test cases in this cycle"),
                        "projectId",
                        Map.of(
                            "type", "integer",
                            "description", "All test cases in this project"),
                        "status",
                        Map.of(
                            "type", "string",
                            "description", "Only test cases with this status",
                            "enum", enumNames(TestCaseStatus.values()))),
                "required", List.of()));
  }

  public static Map<String, Object> getTestCaseDefinition() {
    return Map.of(
        "name", TOOL_GET_TEST_CASE,
        "description",
            "Get a single test case by ID — full preconditions, steps, and expectedResult.",
        "inputSchema",
            Map.of(
                "type", "object",
                "properties",
                    Map.of(
                        "testCaseId",
                        Map.of(
                            "type", "integer",
                            "description", "Numeric test case ID")),
                "required", List.of("testCaseId")));
  }

  public static Map<String, Object> getTestRunsDefinition() {
    return Map.of(
        "name", TOOL_GET_TEST_RUNS,
        "description",
            "List the execution history of a test case — each run's status (PENDING, RUNNING, "
                + "PASSED, FAILED, BLOCKED, SKIPPED), notes, actualResult, and any linked bug "
                + "report.",
        "inputSchema",
            Map.of(
                "type", "object",
                "properties",
                    Map.of(
                        "testCaseId",
                        Map.of(
                            "type", "integer",
                            "description", "The test case whose runs to list")),
                "required", List.of("testCaseId")));
  }

  public static Map<String, Object> recordTestRunDefinition() {
    return Map.of(
        "name", TOOL_RECORD_TEST_RUN,
        "description",
            "Record the result of running a test case. Status must be PENDING, RUNNING, PASSED, "
                + "FAILED, BLOCKED, or SKIPPED. Optional notes capture observed behaviour, build "
                + "version, and environment. Use this after an agent executes or verifies a test "
                + "case against the implementation. Requires WRITE API key scope.",
        "inputSchema",
            Map.of(
                "type", "object",
                "properties",
                    Map.of(
                        "testCaseId",
                        Map.of("type", "integer", "description", "ID of the test case being run"),
                        "status",
                        Map.of(
                            "type", "string",
                            "description", "Run outcome",
                            "enum",
                                List.of(
                                    "PENDING",
                                    "RUNNING",
                                    "PASSED",
                                    "FAILED",
                                    "BLOCKED",
                                    "SKIPPED")),
                        "notes",
                        Map.of("type", "string", "description", "Optional observation notes"),
                        "actualResult",
                        Map.of("type", "string", "description", "Optional actual observed result"),
                        "buildVersion",
                        Map.of("type", "string", "description", "Optional build/version label"),
                        "environment",
                        Map.of(
                            "type", "string",
                            "description", "Optional environment label (dev, staging, prod, …)")),
                "required", List.of("testCaseId", "status")));
  }

  private static Map<String, Object> testCaseFields(boolean forCreate) {
    Map<String, Object> p = new HashMap<>();
    p.put("title", Map.of("type", "string", "description", "Short title (max 255 chars)"));
    p.put("description", Map.of("type", "string", "description", "What this test covers"));
    p.put("preconditions", Map.of("type", "string", "description", "State required before running"));
    p.put("steps", Map.of("type", "string", "description", "Steps to execute, one per line"));
    p.put("expectedResult", Map.of("type", "string", "description", "What must be true if it passes"));
    p.put("type", Map.of("type", "string", "description", "Kind of test",
        "enum", enumNames(TestCaseType.values())));
    p.put("priority", Map.of("type", "string", "description", "Importance",
        "enum", enumNames(TestCasePriority.values())));
    p.put("status", Map.of("type", "string", "description",
        "Lifecycle status" + (forCreate ? " (default DRAFT)" : ""),
        "enum", enumNames(TestCaseStatus.values())));
    p.put("tags", Map.of("type", "array", "items", Map.of("type", "string"),
        "description", "Free-form labels"));
    p.put("estimatedMinutes", Map.of("type", "integer", "description", "Estimated run time"));
    p.put("taskId", Map.of("type", "integer", "description", "Link to this task"));
    p.put("pitchId", Map.of("type", "integer", "description", "Link to this pitch"));
    p.put("cycleId", Map.of("type", "integer", "description", "Link to this cycle"));
    p.put("scopeId", Map.of("type", "integer", "description", "Link to this hill-chart scope"));
    p.put("teamId", Map.of("type", "integer", "description", "Owning team"));
    return p;
  }

  public static Map<String, Object> createTestCaseDefinition() {
    Map<String, Object> props = testCaseFields(true);
    props.put("projectId", Map.of("type", "integer", "description",
        "Project the test case belongs to. Inferred from the pitch or cycle when omitted."));
    return Map.of(
        "name", TOOL_CREATE_TEST_CASE,
        "description",
            "Create a test case (acceptance criterion). title, type and priority are required; "
                + "link it to a task/pitch/cycle/project so it shows up where the work is. "
                + "Requires WRITE API key scope.",
        "inputSchema",
            Map.of("type", "object", "properties", props,
                "required", List.of("title", "type", "priority")));
  }

  public static Map<String, Object> createTestCasesDefinition() {
    Map<String, Object> item = testCaseFields(true);
    item.put("projectId", Map.of("type", "integer", "description", "Project of this test case"));
    return Map.of(
        "name", TOOL_CREATE_TEST_CASES,
        "description",
            "Create several test cases atomically — if any one is invalid none are created. Each "
                + "item takes the same fields as create_test_case. Requires WRITE API key scope.",
        "inputSchema",
            Map.of("type", "object",
                "properties", Map.of("testCases", Map.of(
                    "type", "array",
                    "items", Map.of("type", "object", "properties", item,
                        "required", List.of("title", "type", "priority")),
                    "description", "Test cases to create")),
                "required", List.of("testCases")));
  }

  public static Map<String, Object> updateTestCaseDefinition() {
    Map<String, Object> props = testCaseFields(false);
    props.put("testCaseId", Map.of("type", "integer", "description", "Test case to change"));
    return Map.of(
        "name", TOOL_UPDATE_TEST_CASE,
        "description",
            "Update a test case. Only the fields you pass are changed. Use status to move it "
                + "through DRAFT → READY → APPROVED, or to DEPRECATED/ARCHIVED. Requires WRITE "
                + "API key scope.",
        "inputSchema",
            Map.of("type", "object", "properties", props, "required", List.of("testCaseId")));
  }

  public static Map<String, Object> deleteTestCaseDefinition() {
    return Map.of(
        "name", TOOL_DELETE_TEST_CASE,
        "description",
            "Delete a test case (soft delete — it disappears from lists but its history and "
                + "past runs are kept). Prefer update_test_case with status ARCHIVED if you only "
                + "want to retire it. Requires WRITE API key scope.",
        "inputSchema",
            Map.of("type", "object",
                "properties", Map.of("testCaseId",
                    Map.of("type", "integer", "description", "Test case to delete")),
                "required", List.of("testCaseId")));
  }

  // ── Implementations ──────────────────────────────────────────────────────

  public List<McpTestCaseDTO> getTestCases(Map<String, Object> args) {
    Object taskIdArg = args.get("taskId");
    Object pitchIdArg = args.get("pitchId");
    Object cycleIdArg = args.get("cycleId");
    Object projectIdArg = args.get("projectId");

    if (taskIdArg == null && pitchIdArg == null && cycleIdArg == null && projectIdArg == null) {
      throw new IllegalArgumentException(
          "One of 'taskId', 'pitchId', 'cycleId', or 'projectId' must be provided.");
    }
    TestCaseStatus status = enumArg(TestCaseStatus.class, args.get("status"), "status");

    List<com.github.farzadsedaghatbin.shipflow.dto.qa.TestCaseDTO> found;
    if (taskIdArg != null) {
      found = testCaseService.getTestCasesByTask(toLong(taskIdArg, "taskId"));
    } else if (pitchIdArg != null) {
      found = testCaseService.getTestCasesByPitch(toLong(pitchIdArg, "pitchId"));
    } else if (cycleIdArg != null) {
      found = testCaseService.getTestCasesByCycle(toLong(cycleIdArg, "cycleId"));
    } else {
      found = testCaseService.getTestCasesWithFilters(
          toLong(projectIdArg, "projectId"), null, null, null, null, null, null, null);
    }
    return found.stream()
        .filter(tc -> status == null || tc.getStatus() == status)
        .map(McpTestCaseDTO::from)
        .toList();
  }

  public McpTestCaseDTO getTestCase(Map<String, Object> args) {
    long id = toLong(args.get("testCaseId"), "testCaseId");
    return McpTestCaseDTO.from(testCaseService.getTestCaseById(id));
  }

  public List<McpTestRunDTO> getTestRuns(Map<String, Object> args) {
    long testCaseId = toLong(args.get("testCaseId"), "testCaseId");
    return testRunService.getTestRunsByTestCase(testCaseId).stream()
        .map(McpTestRunDTO::from).toList();
  }

  public McpTestRunDTO recordTestRun(Map<String, Object> args, Authentication auth) {
    long testCaseId = toLong(args.get("testCaseId"), "testCaseId");
    String statusStr = (String) args.get("status");
    if (statusStr == null || statusStr.isBlank()) {
      throw new IllegalArgumentException("Missing required argument: status");
    }
    TestRunStatus status;
    try {
      status = TestRunStatus.valueOf(statusStr.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(
          "Invalid status '" + statusStr + "'. Must be one of: PENDING, RUNNING, PASSED, FAILED, "
              + "BLOCKED, SKIPPED");
    }

    User caller = resolveUser(auth);

    CreateTestRunRequest request = new CreateTestRunRequest();
    request.setTestCaseId(testCaseId);
    request.setStatus(status);
    if (args.get("notes") != null) {
      request.setNotes(args.get("notes").toString());
    }
    if (args.get("actualResult") != null) {
      request.setActualResult(args.get("actualResult").toString());
    }
    if (args.get("buildVersion") != null) {
      request.setBuildVersion(args.get("buildVersion").toString());
    }
    if (args.get("environment") != null) {
      request.setEnvironment(args.get("environment").toString());
    }

    TestRunDTO created = testRunService.createTestRun(request, caller.getId());
    return McpTestRunDTO.from(created);
  }

  public McpTestCaseDTO createTestCase(Map<String, Object> args, Authentication auth) {
    CreateTestCaseRequest request = toCreateRequest(args);
    User caller = resolveUser(auth);
    return McpTestCaseDTO.from(testCaseService.createTestCase(request, caller.getId()));
  }

  @SuppressWarnings("unchecked")
  public List<McpTestCaseDTO> createTestCases(Map<String, Object> args, Authentication auth) {
    Object raw = args.get("testCases");
    if (!(raw instanceof List<?> items) || items.isEmpty()) {
      throw new IllegalArgumentException("'testCases' must be a non-empty array.");
    }
    List<CreateTestCaseRequest> requests = new ArrayList<>();
    for (Object item : items) {
      if (!(item instanceof Map<?, ?> m)) {
        throw new IllegalArgumentException("Each entry of 'testCases' must be an object.");
      }
      requests.add(toCreateRequest((Map<String, Object>) m));
    }
    User caller = resolveUser(auth);
    return testCaseService
        .createTestCasesBulk(new BulkCreateTestCaseRequest(requests), caller.getId())
        .stream().map(McpTestCaseDTO::from).toList();
  }

  public McpTestCaseDTO updateTestCase(Map<String, Object> args, Authentication auth) {
    long id = toLong(args.get("testCaseId"), "testCaseId");
    User caller = resolveUser(auth);
    UpdateTestCaseRequest r = new UpdateTestCaseRequest();
    if (args.get("title") != null) {
      r.setTitle(requireText(args.get("title"), "title"));
    }
    r.setDescription(text(args.get("description")));
    r.setPreconditions(text(args.get("preconditions")));
    r.setSteps(text(args.get("steps")));
    r.setExpectedResult(text(args.get("expectedResult")));
    r.setType(enumArg(TestCaseType.class, args.get("type"), "type"));
    r.setPriority(enumArg(TestCasePriority.class, args.get("priority"), "priority"));
    r.setStatus(enumArg(TestCaseStatus.class, args.get("status"), "status"));
    r.setTags(tags(args.get("tags")));
    r.setEstimatedMinutes(optInt(args.get("estimatedMinutes"), "estimatedMinutes"));
    r.setTaskId(optLong(args.get("taskId"), "taskId"));
    r.setPitchId(optLong(args.get("pitchId"), "pitchId"));
    r.setCycleId(optLong(args.get("cycleId"), "cycleId"));
    r.setScopeId(optLong(args.get("scopeId"), "scopeId"));
    r.setTeamId(optLong(args.get("teamId"), "teamId"));
    return McpTestCaseDTO.from(testCaseService.updateTestCase(id, r, caller.getId()));
  }

  public Map<String, Object> deleteTestCase(Map<String, Object> args) {
    long id = toLong(args.get("testCaseId"), "testCaseId");
    testCaseService.deleteTestCase(id);
    return Map.of("deleted", true, "testCaseId", id);
  }

  private static CreateTestCaseRequest toCreateRequest(Map<String, Object> a) {
    CreateTestCaseRequest r = new CreateTestCaseRequest();
    r.setTitle(requireText(a.get("title"), "title"));
    r.setDescription(text(a.get("description")));
    r.setPreconditions(text(a.get("preconditions")));
    r.setSteps(text(a.get("steps")));
    r.setExpectedResult(text(a.get("expectedResult")));
    TestCaseType type = enumArg(TestCaseType.class, a.get("type"), "type");
    TestCasePriority priority = enumArg(TestCasePriority.class, a.get("priority"), "priority");
    if (type == null || priority == null) {
      throw new IllegalArgumentException("'type' and 'priority' are required.");
    }
    r.setType(type);
    r.setPriority(priority);
    r.setStatus(enumArg(TestCaseStatus.class, a.get("status"), "status"));
    r.setTags(tags(a.get("tags")));
    r.setEstimatedMinutes(optInt(a.get("estimatedMinutes"), "estimatedMinutes"));
    r.setProjectId(optLong(a.get("projectId"), "projectId"));
    r.setTaskId(optLong(a.get("taskId"), "taskId"));
    r.setPitchId(optLong(a.get("pitchId"), "pitchId"));
    r.setCycleId(optLong(a.get("cycleId"), "cycleId"));
    r.setScopeId(optLong(a.get("scopeId"), "scopeId"));
    r.setTeamId(optLong(a.get("teamId"), "teamId"));
    return r;
  }

  // ── Helpers ──────────────────────────────────────────────────────────────

  private static List<String> enumNames(Enum<?>[] values) {
    return Arrays.stream(values).map(Enum::name).toList();
  }

  private static <E extends Enum<E>> E enumArg(Class<E> type, Object val, String argName) {
    if (val == null || val.toString().isBlank()) {
      return null;
    }
    try {
      return Enum.valueOf(type, val.toString().trim().toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(
          "Invalid " + argName + " '" + val + "'. Must be one of: "
              + String.join(", ", enumNames(type.getEnumConstants())));
    }
  }

  private static String text(Object val) {
    return val == null ? null : val.toString();
  }

  private static String requireText(Object val, String argName) {
    if (val == null || val.toString().isBlank()) {
      throw new IllegalArgumentException("Missing required argument: " + argName);
    }
    return val.toString();
  }

  private static List<String> tags(Object val) {
    if (val == null) {
      return null;
    }
    if (val instanceof List<?> l) {
      return l.stream().map(Object::toString).toList();
    }
    return Arrays.stream(val.toString().split(",")).map(String::trim)
        .filter(t -> !t.isEmpty()).toList();
  }

  private static Long optLong(Object val, String argName) {
    return val == null ? null : toLong(val, argName);
  }

  private static Integer optInt(Object val, String argName) {
    return val == null ? null : (int) toLong(val, argName);
  }

  private User resolveUser(Authentication auth) {
    if (auth == null || auth.getName() == null) {
      throw new SecurityException("No authenticated user in MCP session");
    }
    return userRepository.findByUsernameWithPerson(auth.getName())
        .orElseThrow(() -> new SecurityException("MCP user not found: " + auth.getName()));
  }

  private static long toLong(Object val, String argName) {
    if (val == null) {
      throw new IllegalArgumentException("Missing required argument: " + argName);
    }
    if (val instanceof Number n) {
      return n.longValue();
    }
    try {
      return Long.parseLong(val.toString());
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(
          "Argument '" + argName + "' must be a number, got: " + val);
    }
  }
}
