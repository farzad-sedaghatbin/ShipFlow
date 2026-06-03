package com.github.farzadsedaghatbin.shipflow.service.mcp.server.tools;

import com.github.farzadsedaghatbin.shipflow.dto.mcp.McpTestCaseDTO;
import com.github.farzadsedaghatbin.shipflow.dto.mcp.McpTestRunDTO;
import com.github.farzadsedaghatbin.shipflow.dto.qa.CreateTestRunRequest;
import com.github.farzadsedaghatbin.shipflow.dto.qa.TestRunDTO;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TestRunStatus;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import com.github.farzadsedaghatbin.shipflow.service.TestCaseService;
import com.github.farzadsedaghatbin.shipflow.service.TestRunService;
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

  private final TestCaseService testCaseService;
  private final TestRunService testRunService;
  private final UserRepository userRepository;

  // ── Tool definitions ──────────────────────────────────────────────────────

  public static Map<String, Object> getTestCasesDefinition() {
    return Map.of(
        "name", TOOL_GET_TEST_CASES,
        "description",
            "List test cases (acceptance criteria) for a task, pitch, or cycle. Returns "
                + "preconditions, steps, and expectedResult for each — the criteria an "
                + "implementation must satisfy. Exactly one scope is required: taskId, pitchId, "
                + "or cycleId.",
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
                            "description", "All test cases in this cycle")),
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

  // ── Implementations ──────────────────────────────────────────────────────

  public List<McpTestCaseDTO> getTestCases(Map<String, Object> args) {
    Object taskIdArg = args.get("taskId");
    Object pitchIdArg = args.get("pitchId");
    Object cycleIdArg = args.get("cycleId");

    if (taskIdArg == null && pitchIdArg == null && cycleIdArg == null) {
      throw new IllegalArgumentException(
          "One of 'taskId', 'pitchId', or 'cycleId' must be provided.");
    }

    if (taskIdArg != null) {
      return testCaseService.getTestCasesByTask(toLong(taskIdArg, "taskId")).stream()
          .map(McpTestCaseDTO::from).toList();
    }
    if (pitchIdArg != null) {
      return testCaseService.getTestCasesByPitch(toLong(pitchIdArg, "pitchId")).stream()
          .map(McpTestCaseDTO::from).toList();
    }
    return testCaseService.getTestCasesByCycle(toLong(cycleIdArg, "cycleId")).stream()
        .map(McpTestCaseDTO::from).toList();
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

  // ── Helpers ──────────────────────────────────────────────────────────────

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
