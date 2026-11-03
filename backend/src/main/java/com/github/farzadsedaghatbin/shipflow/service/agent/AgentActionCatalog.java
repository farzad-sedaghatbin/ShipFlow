package com.github.farzadsedaghatbin.shipflow.service.agent;

import com.github.farzadsedaghatbin.shipflow.service.mcp.server.tools.BugReportMcpTools;
import com.github.farzadsedaghatbin.shipflow.service.mcp.server.tools.PitchMcpTools;
import com.github.farzadsedaghatbin.shipflow.service.mcp.server.tools.TaskMcpTools;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * The MCP write tools the agent may <em>propose</em>, and how to describe them.
 *
 * <p>This is a deliberately small allow-list, not "every write tool": the agent works with tasks,
 * bugs and pitches in the project of the thread it was mentioned in. Analyses, roadmap edits, work
 * logging and anything not listed here can never be proposed, whatever the model emits. The tool
 * definitions are the live MCP ones, so the prompt can never drift from what the tools accept.
 */
@Component
public class AgentActionCatalog {

  private final Map<String, Map<String, Object>> tools = new LinkedHashMap<>();

  public AgentActionCatalog() {
    register(TaskMcpTools.createTaskDefinition());
    register(TaskMcpTools.updateTaskDefinition());
    register(TaskMcpTools.updateTaskStatusDefinition());
    register(TaskMcpTools.updateTaskAssigneeDefinition());
    register(BugReportMcpTools.createBugDefinition());
    register(BugReportMcpTools.updateBugStatusDefinition());
    register(BugReportMcpTools.updateBugAssigneeDefinition());
    register(PitchMcpTools.updatePitchDefinition());
    register(PitchMcpTools.updatePitchStatusDefinition());
  }

  private void register(Map<String, Object> definition) {
    tools.put((String) definition.get("name"), definition);
  }

  public boolean isAllowed(String tool) {
    return tool != null && tools.containsKey(tool);
  }

  public Set<String> toolNames() {
    return Set.copyOf(tools.keySet());
  }

  /** The tool list for the prompt: name, what it does, and its parameters. */
  @SuppressWarnings("unchecked")
  public String promptSection() {
    StringBuilder sb = new StringBuilder();
    for (Map<String, Object> def : tools.values()) {
      sb.append("- ").append(def.get("name")).append(": ").append(firstSentences(def)).append('\n');
      Map<String, Object> schema = (Map<String, Object>) def.get("inputSchema");
      Map<String, Object> props = (Map<String, Object>) schema.getOrDefault("properties", Map.of());
      List<String> required = (List<String>) schema.getOrDefault("required", List.of());
      sb.append("    params: ")
          .append(
              new TreeMap<>(props)
                  .entrySet().stream()
                      .map(e -> param(e.getKey(), (Map<String, Object>) e.getValue(), required))
                      .collect(Collectors.joining(", ")))
          .append('\n');
    }
    return sb.toString();
  }

  /**
   * A plain description of exactly what will run, built from the real tool name and arguments —
   * never from model text — so what a person confirms is what is executed.
   */
  public String describe(String tool, Map<String, Object> args) {
    String params =
        new TreeMap<>(args == null ? Map.<String, Object>of() : args)
            .entrySet().stream()
                .map(e -> e.getKey() + "=" + clip(String.valueOf(e.getValue()), 80))
                .collect(Collectors.joining(", "));
    return tool + "(" + params + ")";
  }

  @SuppressWarnings("unchecked")
  private static String param(String name, Map<String, Object> spec, List<String> required) {
    StringBuilder sb = new StringBuilder(name);
    sb.append(required.contains(name) ? " (required " : " (").append(spec.get("type"));
    Object values = spec.get("enum");
    if (values instanceof List<?> list) {
      sb.append(": ").append(list.stream().map(String::valueOf).collect(Collectors.joining("|")));
    }
    return sb.append(')').toString();
  }

  private static String firstSentences(Map<String, Object> def) {
    String d = String.valueOf(def.get("description"));
    int cut = d.indexOf("Requires WRITE");
    if (cut > 0) {
      d = d.substring(0, cut);
    }
    return clip(d.trim(), 220);
  }

  private static String clip(String s, int max) {
    return s.length() <= max ? s : s.substring(0, max) + "…";
  }
}
