package com.github.farzadsedaghatbin.shipflow.service.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Pulls the agent's proposed changes out of the model's raw reply.
 *
 * <p>The model is asked to append a fenced block after its answer:
 *
 * <pre>
 * ```shipflow-actions
 * [{"tool":"update_task_status","arguments":{"taskId":3,"status":"DONE"},"summary":"Mark done"}]
 * ```
 * </pre>
 *
 * Parsing is lenient about layout and strict about content: a malformed block simply yields no
 * actions (the answer still goes out), and the block is always removed from the visible text.
 * Nothing parsed here is trusted — {@code AgentActionService} still screens every item.
 */
@Component
@Slf4j
public class AgentActionParser {

  /** An opened {@code shipflow-actions} fence, up to its closing fence or the end of the text. */
  private static final Pattern BLOCK =
      Pattern.compile("```\\s*shipflow-actions\\s*(.*?)(?:```|\\z)", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

  private static final int MAX_SUMMARY = 300;

  private final ObjectMapper objectMapper;

  public AgentActionParser(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /** One change as the model wrote it — not yet validated. */
  public record ProposedAction(String tool, Map<String, Object> arguments, String summary) {}

  /** The reply with the block removed, plus whatever actions could be read from it. */
  public record Parsed(String visibleText, List<ProposedAction> actions) {}

  public Parsed parse(String modelOutput) {
    if (modelOutput == null) {
      return new Parsed("", List.of());
    }
    List<ProposedAction> actions = new ArrayList<>();
    Matcher m = BLOCK.matcher(modelOutput);
    while (m.find()) {
      actions.addAll(readActions(m.group(1)));
    }
    String visible = BLOCK.matcher(modelOutput).replaceAll("").trim();
    return new Parsed(visible, actions);
  }

  private List<ProposedAction> readActions(String json) {
    List<ProposedAction> out = new ArrayList<>();
    try {
      JsonNode root = objectMapper.readTree(json.trim());
      if (root == null) {
        return out;
      }
      Iterable<JsonNode> items = root.isArray() ? root : List.of(root);
      for (JsonNode item : items) {
        JsonNode tool = item.get("tool");
        JsonNode args = item.get("arguments");
        if (tool == null || !tool.isTextual() || args == null || !args.isObject()) {
          continue;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> argMap = objectMapper.convertValue(args, LinkedHashMap.class);
        JsonNode summary = item.get("summary");
        out.add(
            new ProposedAction(
                tool.asText().trim(),
                argMap,
                summary != null && summary.isTextual() ? clip(summary.asText().trim()) : ""));
      }
    } catch (Exception e) {
      log.debug("Ignoring unreadable shipflow-actions block: {}", e.getMessage());
    }
    return out;
  }

  private static String clip(String s) {
    return s.length() <= MAX_SUMMARY ? s : s.substring(0, MAX_SUMMARY) + "…";
  }
}
