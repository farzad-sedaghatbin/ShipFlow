package com.github.farzadsedaghatbin.shipflow.dto.mcp;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

/**
 * Aggregate response for the {@code get_task_context} MCP tool.
 *
 * <p>Given a single taskId, this bundles everything a coding agent needs to implement against a
 * task without making four follow-up calls: the task itself (with full dependency graph), its
 * parent pitch (with Shape Up fields and wireframe URLs), the parent cycle metadata, sibling tasks
 * under the same pitch, and a small set of human-readable {@code hints} that tell the agent how to
 * use the payload (e.g. "wireframeLinks present — fetch via Figma MCP").
 */
@Data
@Builder
public class McpTaskContextDTO {

  private McpTaskDetailDTO task;

  /** Parent pitch — null if the task isn't linked to a pitch. */
  private McpPitchDTO pitch;

  /** Parent cycle — null only if the task has no cycle, which would be unusual. */
  private McpCycleDTO cycle;

  /** Other tasks under the same pitch (or same cycle if no pitch). Excludes the focal task. */
  private List<McpTaskDTO> siblings;

  /**
   * Whether {@link #siblings} was truncated by the caller-provided {@code siblingLimit}. When true,
   * agents should know that more tasks exist under this pitch than were returned.
   */
  private Boolean siblingsTruncated;

  private Integer siblingTotalCount;

  /** Status -> count breakdown across {@link #siblings} (does not include the focal task). */
  private Map<String, Integer> siblingStatusCounts;

  /**
   * Self-describing hints generated server-side. Examples:
   * <ul>
   *   <li>"pitch.wireframeLinks is present — fetch design via Figma MCP before implementing"
   *   <li>"task is BLOCKED by 2 tasks — resolve dependencies before starting"
   *   <li>"pitch.solution is empty — context is thin"
   *   <li>"task has 3 subtasks — consider implementing those first"
   * </ul>
   */
  private List<String> hints;
}
