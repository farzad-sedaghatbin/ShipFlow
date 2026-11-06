package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.CreateTaskDependencyRequest;
import com.github.farzadsedaghatbin.shipflow.dto.TaskDependencyDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Task;
import com.github.farzadsedaghatbin.shipflow.entity.TaskDependency;
import com.github.farzadsedaghatbin.shipflow.entity.enums.DependencyType;
import com.github.farzadsedaghatbin.shipflow.exception.BadRequestException;
import com.github.farzadsedaghatbin.shipflow.repository.TaskDependencyRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TaskRepository;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing task dependencies. Handles creating, deleting, and
 * querying task dependencies with circular dependency detection.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class TaskDependencyService {

  private final TaskDependencyRepository taskDependencyRepository;
  private final TaskRepository taskRepository;
  private final LocalizationService localizationService;
  private final MessageService messageService;

  /**
   * Add a dependency between tasks. Validates that the dependency doesn't create
   * a circular reference.
   */
  public TaskDependencyDTO addDependency(Long sourceTaskId, CreateTaskDependencyRequest request) {
    // Validate both tasks exist
    Task sourceTask = taskRepository.findById(sourceTaskId)
        .orElseThrow(() -> new IllegalArgumentException("Source task not found with id: " + sourceTaskId));

    Task targetTask = taskRepository.findById(request.getTargetTaskId()).orElseThrow(
        () -> new IllegalArgumentException("Target task not found with id: " + request.getTargetTaskId()));

    // Prevent self-dependencies
    if (sourceTaskId.equals(request.getTargetTaskId())) {
      throw new BadRequestException(localizationService.getMessage("dependency.self.reference"));
    }

    // Tasks must share a cycle; tasks outside any cycle (backlog / debt work) must share a project
    if (!inSameScope(sourceTask, targetTask)) {
      throw new BadRequestException(localizationService.getMessage("dependency.same.cycle.required"));
    }

    DependencyType type = request.getDependencyType() != null ? request.getDependencyType()
        : DependencyType.BLOCKS;

    // Check for circular dependencies (only blocking links can form a cycle)
    if (wouldCreateCircularDependency(sourceTaskId, request.getTargetTaskId(), type)) {
      throw new BadRequestException(localizationService.getMessage("dependency.circular.reference"));
    }

    // One link per pair, in either direction: "A relates to B" and "B relates to A" are the same fact
    if (taskDependencyRepository.existsBetweenTasks(sourceTaskId, request.getTargetTaskId())) {
      throw new BadRequestException(localizationService.getMessage("dependency.already.exists"));
    }

    // Create the dependency
    TaskDependency dependency = TaskDependency.builder().sourceTask(sourceTask).targetTask(targetTask)
        .dependencyType(type).build();

    TaskDependency saved = taskDependencyRepository.save(dependency);
    return TaskDependencyPerspective.toDTO(saved, sourceTaskId);
  }

  /** Remove a dependency between tasks. */
  public void removeDependency(Long dependencyId) {
    if (!taskDependencyRepository.existsById(dependencyId)) {
      throw new IllegalArgumentException(localizationService.getMessage("dependency.not.found", dependencyId));
    }
    taskDependencyRepository.deleteById(dependencyId);
  }

  /**
   * Get all dependencies for a task, split by what they mean for that task: {@code blocking} (tasks
   * this one blocks), {@code blockedBy} (tasks that block this one) and {@code related} (plain links).
   * The type matters: a DEPENDS_ON row makes its target the blocker, and RELATED_TO blocks nobody.
   */
  @Transactional(readOnly = true)
  public Map<String, List<TaskDependencyDTO>> getDependenciesForTask(Long taskId) {
    TaskDependencyPerspective.Grouped grouped = loadGrouped(taskId);

    Map<String, List<TaskDependencyDTO>> result = new LinkedHashMap<>();
    result.put("blocking", grouped.blocking());
    result.put("blockedBy", grouped.blockedBy());
    result.put("related", grouped.related());
    return result;
  }

  /** Get all dependencies where the given task blocks another task. */
  @Transactional(readOnly = true)
  public List<TaskDependencyDTO> getBlockingDependencies(Long taskId) {
    return loadGrouped(taskId).blocking();
  }

  /** Get all dependencies where another task blocks the given task. */
  @Transactional(readOnly = true)
  public List<TaskDependencyDTO> getBlockedByDependencies(Long taskId) {
    return loadGrouped(taskId).blockedBy();
  }

  private TaskDependencyPerspective.Grouped loadGrouped(Long taskId) {
    if (!taskRepository.existsById(taskId)) {
      throw new IllegalArgumentException(messageService.getMessage("error.task.not.found", taskId));
    }
    return TaskDependencyPerspective.group(taskId,
        taskDependencyRepository.findBySourceTaskIdOrTargetTaskId(taskId, taskId));
  }

  private static boolean inSameScope(Task a, Task b) {
    Long cycleA = a.getCycle() != null ? a.getCycle().getId() : null;
    Long cycleB = b.getCycle() != null ? b.getCycle().getId() : null;
    if (cycleA != null || cycleB != null) {
      return Objects.equals(cycleA, cycleB);
    }
    Long projectA = a.getProject() != null ? a.getProject().getId() : null;
    Long projectB = b.getProject() != null ? b.getProject().getId() : null;
    return projectA != null && projectA.equals(projectB);
  }

  /**
   * Would adding this link make a task (transitively) wait for itself? Only the blocking direction
   * counts: BLOCKS is {@code source -> target}, DEPENDS_ON is {@code target -> source}, RELATED_TO is
   * not an edge at all.
   */
  private boolean wouldCreateCircularDependency(Long sourceTaskId, Long targetTaskId, DependencyType type) {
    Long blocker;
    Long blocked;
    switch (type) {
      case BLOCKS -> {
        blocker = sourceTaskId;
        blocked = targetTaskId;
      }
      case DEPENDS_ON -> {
        blocker = targetTaskId;
        blocked = sourceTaskId;
      }
      default -> {
        return false;
      }
    }
    // The new edge is blocker -> blocked; it closes a loop if blocked already (transitively) blocks blocker.
    return blocks(blocked, blocker);
  }

  /** True if {@code from} already blocks {@code to}, directly or through other tasks (iterative DFS). */
  private boolean blocks(Long from, Long to) {
    Deque<Long> stack = new ArrayDeque<>();
    Set<Long> visited = new HashSet<>();
    stack.push(from);
    while (!stack.isEmpty()) {
      Long current = stack.pop();
      if (current.equals(to)) {
        return true;
      }
      if (!visited.add(current)) {
        continue;
      }
      for (TaskDependency dep : taskDependencyRepository.findBySourceTaskIdOrTargetTaskId(current, current)) {
        Long blocker = TaskDependencyPerspective.blockerId(dep);
        if (blocker != null && blocker.equals(current)) {
          stack.push(TaskDependencyPerspective.blockedId(dep));
        }
      }
    }
    return false;
  }
}
