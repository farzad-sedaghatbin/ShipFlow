package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.CreateTaskDependencyRequest;
import com.github.farzadsedaghatbin.shipflow.dto.TaskDependencyDTO;
import com.github.farzadsedaghatbin.shipflow.service.TaskDependencyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing task dependencies. Provides endpoints for creating, deleting, and
 * querying task dependencies.
 */
@RestController
@RequestMapping("/api/tasks/{taskId}/dependencies")
@RequiredArgsConstructor
@Tag(name = "Task Dependencies", description = "Lightweight task dependency tracking API")
public class TaskDependencyController {

  private final TaskDependencyService taskDependencyService;

  @PostMapping
  @Operation(
      summary = "Add a task dependency",
      description =
          "Create a dependency between the specified task and another task. Validates against circular dependencies.")
  public ResponseEntity<TaskDependencyDTO> addDependency(
      @PathVariable Long taskId, @Valid @RequestBody CreateTaskDependencyRequest request) {
    TaskDependencyDTO dependency = taskDependencyService.addDependency(taskId, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(dependency);
  }

  @GetMapping
  @Operation(
      summary = "Get all dependencies for a task",
      description = "Returns both blocking and blocked-by dependencies for the specified task")
  public ResponseEntity<Map<String, List<TaskDependencyDTO>>> getDependencies(
      @PathVariable Long taskId) {
    Map<String, List<TaskDependencyDTO>> dependencies =
        taskDependencyService.getDependenciesForTask(taskId);
    return ResponseEntity.ok(dependencies);
  }

  @GetMapping("/blocking")
  @Operation(
      summary = "Get tasks this task is blocking",
      description = "Returns all tasks that are blocked by this task")
  public ResponseEntity<List<TaskDependencyDTO>> getBlockingDependencies(
      @PathVariable Long taskId) {
    List<TaskDependencyDTO> dependencies = taskDependencyService.getBlockingDependencies(taskId);
    return ResponseEntity.ok(dependencies);
  }

  @GetMapping("/blocked-by")
  @Operation(
      summary = "Get tasks blocking this task",
      description = "Returns all tasks that are blocking this task (blockers)")
  public ResponseEntity<List<TaskDependencyDTO>> getBlockedByDependencies(
      @PathVariable Long taskId) {
    List<TaskDependencyDTO> dependencies = taskDependencyService.getBlockedByDependencies(taskId);
    return ResponseEntity.ok(dependencies);
  }

  @DeleteMapping("/{dependencyId}")
  @Operation(
      summary = "Remove a task dependency",
      description = "Delete a specific dependency relationship between tasks")
  public ResponseEntity<Void> removeDependency(
      @PathVariable Long taskId, @PathVariable Long dependencyId) {
    taskDependencyService.removeDependency(dependencyId);
    return ResponseEntity.noContent().build();
  }
}
