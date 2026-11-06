package com.github.farzadsedaghatbin.shipflow.dto.mcp;

import com.github.farzadsedaghatbin.shipflow.dto.TaskDependencyDTO;
import com.github.farzadsedaghatbin.shipflow.service.TaskDependencyPerspective;
import lombok.Builder;
import lombok.Data;

/**
 * One row of the task dependency graph as exposed to MCP clients.
 *
 * <p>{@link TaskDependencyDTO} carries both source and target task IDs because dependency edges are
 * symmetric in storage. From the perspective of a single task, only the "other end" of the edge
 * matters — so this DTO is constructed in the direction relevant to the caller (see {@link
 * #fromBlocking} and {@link #fromBlockedBy}).
 */
@Data
@Builder
public class McpTaskDependencyDTO {

  /** The other task in the dependency (not the focal task). */
  private Long taskId;

  private String title;

  /** Dependency type — e.g. BLOCKS, DEPENDS_ON, RELATED_TO. */
  private String dependencyType;

  /**
   * Build a dependency row for the {@code blockingTasks} list of focal task F.
   *
   * <p>F blocks the other task. The DTO must already have been built from F's point of view (see
   * {@link TaskDependencyPerspective}), so {@code otherTaskId} is the task F blocks.
   */
  public static McpTaskDependencyDTO fromBlocking(TaskDependencyDTO dep) {
    return McpTaskDependencyDTO.builder()
        .taskId(dep.getOtherTaskId())
        .title(dep.getOtherTaskTitle())
        .dependencyType(dep.getDependencyType() != null ? dep.getDependencyType().name() : null)
        .build();
  }

  /**
   * Build a dependency row for the {@code blockedByTasks} list of focal task F.
   *
   * <p>The other task blocks F. The DTO must already have been built from F's point of view (see
   * {@link TaskDependencyPerspective}), so {@code otherTaskId} is the blocker.
   */
  public static McpTaskDependencyDTO fromBlockedBy(TaskDependencyDTO dep) {
    return McpTaskDependencyDTO.builder()
        .taskId(dep.getOtherTaskId())
        .title(dep.getOtherTaskTitle())
        .dependencyType(dep.getDependencyType() != null ? dep.getDependencyType().name() : null)
        .build();
  }
}
