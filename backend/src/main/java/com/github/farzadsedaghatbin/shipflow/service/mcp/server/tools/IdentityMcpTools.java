package com.github.farzadsedaghatbin.shipflow.service.mcp.server.tools;

import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Identity-related MCP tools.
 *
 * <p>The MCP API key authenticates as a specific user, but until this tool existed no MCP call
 * surfaced that identity to the client. {@code whoami} closes that gap and unlocks the natural
 * "tasks assigned to me" phrasing — agents can call {@code whoami} once, capture the personId, and
 * pass it to {@code get_tasks(assigneeId: …)} (or use {@code get_tasks(mine: true)} for the
 * one-call variant).
 */
@Component
@RequiredArgsConstructor
public class IdentityMcpTools {

  public static final String TOOL_WHOAMI = "whoami";

  private final UserRepository userRepository;

  public static Map<String, Object> whoamiDefinition() {
    return Map.of(
        "name", TOOL_WHOAMI,
        "description",
            "Return the identity of the authenticated MCP caller — username, full name, email, "
                + "role, userId, and personId. The personId is the value to pass to filters like "
                + "get_tasks(assigneeId: ...) when targeting work by person. Use this once per "
                + "session to resolve 'my tasks' / 'my bugs' style requests.",
        "inputSchema",
            Map.of(
                "type", "object",
                "properties", Map.of(),
                "required", List.of()));
  }

  public Map<String, Object> whoami(Authentication auth) {
    if (auth == null || auth.getName() == null) {
      throw new SecurityException("No authenticated user in MCP session");
    }
    User user = userRepository.findByUsernameWithPerson(auth.getName())
        .orElseThrow(() -> new SecurityException("MCP user not found: " + auth.getName()));

    // Use a mutable map so we can preserve nulls explicitly (Map.of doesn't allow null values).
    Map<String, Object> result = new HashMap<>();
    result.put("userId", user.getId());
    result.put("username", user.getUsername());
    result.put("email", user.getEmail());
    result.put("role", user.getRole() != null ? user.getRole().name() : null);
    result.put("personId", user.getPerson() != null ? user.getPerson().getId() : null);
    result.put("fullName", user.getPerson() != null ? user.getPerson().getName() : null);
    return result;
  }
}
