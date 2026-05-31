package com.github.farzadsedaghatbin.shipflow.service.mcp.server.tools;

import com.github.farzadsedaghatbin.shipflow.dto.CreateWorkLogRequest;
import com.github.farzadsedaghatbin.shipflow.dto.WorkLogDTO;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import com.github.farzadsedaghatbin.shipflow.service.WorkLogService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/** MCP tool implementations for time tracking (write). */
@Component
@RequiredArgsConstructor
public class WorklogMcpTools {

  private final WorkLogService workLogService;
  private final UserRepository userRepository;

  // ── Tool definitions ──────────────────────────────────────────────────────

  public static final String TOOL_LOG_WORK = "log_work";

  public static Map<String, Object> logWorkDefinition() {
    return Map.of(
        "name",
        TOOL_LOG_WORK,
        "description",
            "Log time spent on a task or pitch as the authenticated MCP user. "
                + "Exactly one of taskId or pitchId must be provided. "
                + "hoursSpent must be at least 0.25 (15 minutes). "
                + "date defaults to today (ISO format YYYY-MM-DD). "
                + "Requires WRITE API key scope.",
        "inputSchema",
            Map.of(
                "type",
                "object",
                "properties",
                    Map.of(
                        "taskId",
                        Map.of(
                            "type", "integer",
                            "description", "ID of the task to log time against (mutually exclusive with pitchId)"),
                        "pitchId",
                        Map.of(
                            "type", "integer",
                            "description", "ID of the pitch to log time against (mutually exclusive with taskId)"),
                        "hoursSpent",
                        Map.of(
                            "type", "number",
                            "description",
                                "Hours spent (minimum 0.25 = 15 minutes). Decimals allowed, e.g. 1.5 = 1h 30m."),
                        "date",
                        Map.of(
                            "type", "string",
                            "description",
                                "Work date in ISO format (YYYY-MM-DD). Defaults to today if omitted."),
                        "note",
                        Map.of(
                            "type", "string",
                            "description", "Optional note describing what was done")),
                "required",
                List.of("hoursSpent")));
  }

  // ── Implementation ────────────────────────────────────────────────────────

  /**
   * Log time against a task or pitch as the authenticated MCP user.
   *
   * <p>The {@code auth} argument is passed explicitly from the dispatcher so that this method does
   * not rely on {@link org.springframework.security.core.context.SecurityContextHolder}, which is
   * unreliable when dispatch runs on an executor thread without security-context propagation.
   */
  public WorkLogDTO logWork(Map<String, Object> args, Authentication auth) {
    Object taskIdArg = args.get("taskId");
    Object pitchIdArg = args.get("pitchId");

    if (taskIdArg == null && pitchIdArg == null) {
      throw new IllegalArgumentException(
          "Exactly one of 'taskId' or 'pitchId' must be provided.");
    }
    if (taskIdArg != null && pitchIdArg != null) {
      throw new IllegalArgumentException(
          "Provide either 'taskId' or 'pitchId', not both.");
    }

    Object hoursArg = args.get("hoursSpent");
    if (hoursArg == null) {
      throw new IllegalArgumentException("Missing required argument: hoursSpent");
    }
    BigDecimal hoursSpent;
    try {
      hoursSpent = new BigDecimal(hoursArg.toString());
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("hoursSpent must be a number, got: " + hoursArg);
    }
    if (hoursSpent.compareTo(new BigDecimal("0.25")) < 0) {
      throw new IllegalArgumentException(
          "hoursSpent must be at least 0.25 (15 minutes), got: " + hoursSpent);
    }

    LocalDate date = LocalDate.now();
    Object dateArg = args.get("date");
    if (dateArg != null && !dateArg.toString().isBlank()) {
      try {
        date = LocalDate.parse(dateArg.toString().trim());
      } catch (DateTimeParseException e) {
        throw new IllegalArgumentException(
            "date must be in ISO format (YYYY-MM-DD), got: " + dateArg);
      }
    }

    User currentUser = resolveUser(auth);
    if (currentUser.getPerson() == null) {
      throw new IllegalStateException(
          "MCP user '" + currentUser.getUsername() + "' has no linked person profile. "
              + "Time logging requires a person profile — contact an admin.");
    }

    CreateWorkLogRequest request = CreateWorkLogRequest.builder()
        .personId(currentUser.getPerson().getId())
        .taskId(taskIdArg != null ? toLong(taskIdArg, "taskId") : null)
        .pitchId(pitchIdArg != null ? toLong(pitchIdArg, "pitchId") : null)
        .date(date)
        .hoursSpent(hoursSpent)
        .note(args.get("note") != null ? args.get("note").toString() : null)
        .build();

    return workLogService.createWorkLog(request);
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  private User resolveUser(Authentication auth) {
    if (auth == null || auth.getName() == null) {
      throw new SecurityException("No authenticated user in MCP session");
    }
    return userRepository.findByUsernameWithPerson(auth.getName())
        .orElseThrow(() -> new SecurityException("MCP user not found: " + auth.getName()));
  }

  private long toLong(Object val, String argName) {
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
