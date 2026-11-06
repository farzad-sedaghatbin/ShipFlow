package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.TaskDependencyDTO;
import com.github.farzadsedaghatbin.shipflow.entity.TaskDependency;
import com.github.farzadsedaghatbin.shipflow.entity.enums.DependencyType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Interprets stored {@link TaskDependency} rows from the point of view of one task.
 *
 * <p>A row is stored once, directed {@code source -> target}, and its meaning depends on its
 * {@link DependencyType}:
 *
 * <ul>
 * <li>{@code BLOCKS}: the source blocks the target.
 * <li>{@code DEPENDS_ON}: the source waits for the target, so the <em>target</em> blocks the source.
 * <li>{@code RELATED_TO}: a plain, symmetric link; nobody blocks anybody.
 * </ul>
 *
 * Grouping rows by source/target alone (the old behaviour) is only right for {@code BLOCKS}. Every
 * "what blocks this task?" question must go through this class so the answer is the same in the task
 * page, the backlog badges, the Kanban board, MCP and the reorder validation.
 */
public final class TaskDependencyPerspective {

  private TaskDependencyPerspective() {
  }

  /** The dependencies of one task, split by what they mean for that task. */
  public record Grouped(List<TaskDependencyDTO> blocking, List<TaskDependencyDTO> blockedBy,
      List<TaskDependencyDTO> related) {
  }

  /** How a dependency relates to the task looking at it. */
  public enum Role {
    /** The task blocks the other one. */
    BLOCKS_OTHER,
    /** The other task blocks this one. */
    BLOCKED_BY_OTHER,
    /** Informational link only. */
    RELATED
  }

  /** The role {@code dep} plays for {@code focalTaskId}. */
  public static Role roleFor(TaskDependency dep, Long focalTaskId) {
    DependencyType type = dep.getDependencyType() != null ? dep.getDependencyType() : DependencyType.BLOCKS;
    if (type == DependencyType.RELATED_TO) {
      return Role.RELATED;
    }
    boolean focalIsSource = Objects.equals(dep.getSourceTask().getId(), focalTaskId);
    boolean sourceBlocks = type == DependencyType.BLOCKS;
    return focalIsSource == sourceBlocks ? Role.BLOCKS_OTHER : Role.BLOCKED_BY_OTHER;
  }

  /**
   * The task that must finish first for this edge, or {@code null} for a non-blocking link. Used for
   * cycle detection and ordering: {@code blocker -> blocked}.
   */
  public static Long blockerId(TaskDependency dep) {
    return switch (dep.getDependencyType() == null ? DependencyType.BLOCKS : dep.getDependencyType()) {
      case BLOCKS -> dep.getSourceTask().getId();
      case DEPENDS_ON -> dep.getTargetTask().getId();
      case RELATED_TO -> null;
    };
  }

  /** The task that waits for the other, or {@code null} for a non-blocking link. */
  public static Long blockedId(TaskDependency dep) {
    return switch (dep.getDependencyType() == null ? DependencyType.BLOCKS : dep.getDependencyType()) {
      case BLOCKS -> dep.getTargetTask().getId();
      case DEPENDS_ON -> dep.getSourceTask().getId();
      case RELATED_TO -> null;
    };
  }

  /** Split every dependency touching {@code focalTaskId} into blocking / blockedBy / related. */
  public static Grouped group(Long focalTaskId, Collection<TaskDependency> dependencies) {
    List<TaskDependencyDTO> blocking = new ArrayList<>();
    List<TaskDependencyDTO> blockedBy = new ArrayList<>();
    List<TaskDependencyDTO> related = new ArrayList<>();
    if (dependencies != null) {
      for (TaskDependency dep : dependencies) {
        TaskDependencyDTO dto = toDTO(dep, focalTaskId);
        switch (roleFor(dep, focalTaskId)) {
          case BLOCKS_OTHER -> blocking.add(dto);
          case BLOCKED_BY_OTHER -> blockedBy.add(dto);
          case RELATED -> related.add(dto);
        }
      }
    }
    return new Grouped(blocking, blockedBy, related);
  }

  /** Convert to a DTO that also names the "other end" relative to {@code focalTaskId}. */
  public static TaskDependencyDTO toDTO(TaskDependency dep, Long focalTaskId) {
    boolean focalIsSource = Objects.equals(dep.getSourceTask().getId(), focalTaskId);
    return TaskDependencyDTO.builder().id(dep.getId()).sourceTaskId(dep.getSourceTask().getId())
        .sourceTaskTitle(dep.getSourceTask().getTitle()).targetTaskId(dep.getTargetTask().getId())
        .targetTaskTitle(dep.getTargetTask().getTitle()).dependencyType(dep.getDependencyType())
        .otherTaskId(focalIsSource ? dep.getTargetTask().getId() : dep.getSourceTask().getId())
        .otherTaskTitle(focalIsSource ? dep.getTargetTask().getTitle() : dep.getSourceTask().getTitle())
        .createdAt(dep.getCreatedAt()).build();
  }
}
