package com.github.farzadsedaghatbin.shipflow.dto.mcp;

import com.github.farzadsedaghatbin.shipflow.dto.TaskDTO;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * Extended task representation for the {@code get_task_context} aggregator.
 *
 * <p>Where {@link McpTaskDTO} is the compact list-friendly shape, this DTO adds the fields that a
 * coding agent needs to understand a single task in depth: parent/children tree, dependency edges
 * with the actual related task IDs and titles (not just a count), comment/attachment counts.
 */
@Data
@Builder
public class McpTaskDetailDTO {

  // ── Core (mirrors McpTaskDTO) ──
  private Long id;
  private String title;
  private String description;
  private String status;
  private String priority;
  private Long cycleId;
  private String cycleName;
  private Long projectId;
  private Long pitchId;
  private String pitchTitle;
  private String assigneeName;
  private Boolean isBlocked;
  private Integer blockedByCount;
  private String dueDate;

  // ── Dependency graph ──
  private List<McpTaskDependencyDTO> blockingTasks;
  private List<McpTaskDependencyDTO> blockedByTasks;

  // ── Subtask tree ──
  private Long parentTaskId;
  private String parentTaskTitle;
  private List<McpTaskDTO> children;

  // ── Extras ──
  private Integer commentCount;
  private Integer attachmentCount;

  /** Number of test cases (acceptance criteria) linked to this task. */
  private Integer testCaseCount;

  /** Number of bug reports linked to this task. */
  private Integer bugReportCount;

  public static McpTaskDetailDTO from(TaskDTO dto) {
    return McpTaskDetailDTO.builder()
        .id(dto.getId())
        .title(dto.getTitle())
        .description(dto.getDescription())
        .status(dto.getStatus() != null ? dto.getStatus().name() : null)
        .priority(dto.getPriority() != null ? dto.getPriority().name() : null)
        .cycleId(dto.getCycleId())
        .cycleName(dto.getCycleName())
        .projectId(dto.getProjectId())
        .pitchId(dto.getPitchId())
        .pitchTitle(dto.getPitchTitle())
        .assigneeName(dto.getAssigneeName())
        .isBlocked(dto.getIsBlocked())
        .blockedByCount(dto.getBlockedByCount())
        .dueDate(dto.getDueDate() != null ? dto.getDueDate().toString() : null)
        .blockingTasks(dto.getBlockingTasks() == null ? List.of()
            : dto.getBlockingTasks().stream().map(McpTaskDependencyDTO::fromBlocking).toList())
        .blockedByTasks(dto.getBlockedByTasks() == null ? List.of()
            : dto.getBlockedByTasks().stream().map(McpTaskDependencyDTO::fromBlockedBy).toList())
        .parentTaskId(dto.getParentTaskId())
        .parentTaskTitle(dto.getParentTaskTitle())
        .children(dto.getChildren() == null ? List.of()
            : dto.getChildren().stream().map(McpTaskDTO::from).toList())
        .commentCount(dto.getCommentCount())
        .attachmentCount(dto.getAttachments() == null ? 0 : dto.getAttachments().size())
        .build();
  }
}
