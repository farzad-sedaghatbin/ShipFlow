package com.github.farzadsedaghatbin.shipflow.dto.mcp;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

/**
 * Full relationship graph for a pitch or cycle, returned by the {@code get_work_context} MCP tool.
 *
 * <p>This is the "teamwork graph" equivalent for ShipFlow: one call yields everything an AI agent
 * needs to understand the current state of a unit of work — cycle metadata, pitch details, task
 * breakdown, hill-chart progress, and retrospective notes — without requiring multiple round trips.
 *
 * <p>Graph entry points:
 * <ul>
 *   <li>By {@code pitchId} — full graph scoped to that pitch (cycle inferred from the pitch)
 *   <li>By {@code cycleId} — full graph scoped to that cycle (all pitches, all tasks)
 * </ul>
 */
@Data
@Builder
public class McpWorkContextDTO {

  /** The cycle containing the requested work. */
  private McpCycleDTO cycle;

  /**
   * The specific pitch requested, or {@code null} when the context was fetched by cycleId with
   * multiple pitches (use {@link #pitches} in that case).
   */
  private McpPitchDTO pitch;

  /**
   * All pitches in the cycle. When the context was fetched by pitchId this list contains only
   * that single pitch; when fetched by cycleId it contains all pitches in the cycle.
   */
  private List<McpPitchDTO> pitches;

  /** All tasks in scope, sorted by status then priority. */
  private List<McpTaskDTO> tasks;

  /** Counts of tasks grouped by status — quick health snapshot for the AI. */
  private Map<String, Integer> taskStatusCounts;

  /** Tasks that are currently blocked (subset of {@link #tasks}). */
  private List<McpTaskDTO> blockers;

  /** Hill-chart scopes showing problem/solution-space progress per pitch scope (0–100). */
  private List<McpHillChartScopeDTO> hillChartScopes;

  /** Summaries of retrospectives associated with this cycle. */
  private List<McpRetroSummaryDTO> retrospectives;

  // ── Nested compact types ──────────────────────────────────────────────────

  @Data
  @Builder
  public static class McpHillChartScopeDTO {
    private Long id;
    private String scope;
    private String description;
    /** 0 = just started, 50 = top of the hill (problem understood), 100 = shipped. */
    private Integer position;
    private Long pitchId;
    private String pitchTitle;
    private Long linkedTaskId;
    private String linkedTaskTitle;
  }

  @Data
  @Builder
  public static class McpRetroSummaryDTO {
    private Long id;
    private String title;
    private String status;
    private Integer itemCount;
    private Long cycleId;
    private String cycleName;
  }
}
