package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.CreateTaskRequest;
import com.github.farzadsedaghatbin.shipflow.dto.TaskDTO;
import com.github.farzadsedaghatbin.shipflow.dto.TaskStatisticsDTO;
import com.github.farzadsedaghatbin.shipflow.dto.audit.EntityHistoryDTO;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskCategory;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskPriority;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskStatus;
import com.github.farzadsedaghatbin.shipflow.service.AuditService;
import com.github.farzadsedaghatbin.shipflow.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "Task management APIs for tracking independent work during cycles")
public class TaskController {

  private final TaskService taskService;
  private final AuditService auditService;
  
  // Allowed fields for sorting to prevent runtime errors
  private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
      "id", "createdAt", "updatedAt", "title", "status", "priority", "category"
  );
  
  /**
   * Validates and returns a safe sort field. Returns default if invalid.
   */
  private String validateSortField(String sortBy, String defaultField) {
    return ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : defaultField;
  }

  // ========== Current User's Tasks ==========

  @GetMapping("/my")
  @Operation(summary = "Get current user's tasks", description = "Returns all tasks assigned to the currently authenticated user (as assignee or pair) with pagination")
  @ApiResponses({@ApiResponse(responseCode = "200", description = "Tasks retrieved successfully"),
      @ApiResponse(responseCode = "401", description = "User not authenticated"),
      @ApiResponse(responseCode = "400", description = "User not linked to a person profile")})
  public ResponseEntity<Page<TaskDTO>> getMyTasks(@RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size, @RequestParam(defaultValue = "createdAt") String sortBy,
      @RequestParam(defaultValue = "desc") String sortOrder) {
    String validSortBy = validateSortField(sortBy, "createdAt");
    Sort.Direction direction = sortOrder.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
    Pageable pageable = PageRequest.of(page, size, Sort.by(direction, validSortBy));
    return ResponseEntity.ok(taskService.getMyTasks(pageable));
  }

  @GetMapping("/my/cycle/{cycleId}")
  @Operation(summary = "Get current user's tasks by cycle", description = "Returns tasks for the current user filtered by cycle ID with pagination")
  public ResponseEntity<Page<TaskDTO>> getMyTasksByCycle(@PathVariable Long cycleId,
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "createdAt") String sortBy,
      @RequestParam(defaultValue = "desc") String sortOrder) {
    String validSortBy = validateSortField(sortBy, "createdAt");
    Sort.Direction direction = sortOrder.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
    Pageable pageable = PageRequest.of(page, size, Sort.by(direction, validSortBy));
    return ResponseEntity.ok(taskService.getMyTasksByCycle(cycleId, pageable));
  }

  // ========== General Task Management ==========

  @GetMapping
  @Operation(summary = "Get all tasks", description = "Returns all tasks in the system with pagination and sorting")
  public ResponseEntity<Page<TaskDTO>> getAllTasks(@RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size, @RequestParam(defaultValue = "createdAt") String sortBy,
      @RequestParam(defaultValue = "desc") String sortOrder) {
    String validSortBy = validateSortField(sortBy, "createdAt");
    Sort.Direction direction = sortOrder.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
    Pageable pageable = PageRequest.of(page, size, Sort.by(direction, validSortBy));
    return ResponseEntity.ok(taskService.getAllTasks(pageable));
  }

  @GetMapping("/search")
  @Operation(summary = "Search tasks", description = "Search tasks by title or description. Minimum 3 characters required.")
  public ResponseEntity<?> searchTasks(@RequestParam String q, @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size, @RequestParam(defaultValue = "createdAt") String sortBy,
      @RequestParam(defaultValue = "desc") String sortOrder) {
    if (q == null || q.trim().length() < 3) {
      return ResponseEntity.badRequest().body(Map.of("error", "Search query must be at least 3 characters"));
    }
    String validSortBy = validateSortField(sortBy, "createdAt");
    Sort.Direction direction = sortOrder.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
    Pageable pageable = PageRequest.of(page, size, Sort.by(direction, validSortBy));
    return ResponseEntity.ok(taskService.searchTasks(q, pageable));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get task by ID")
  public ResponseEntity<TaskDTO> getTaskById(@PathVariable Long id) {
    return ResponseEntity.ok(taskService.getTaskById(id));
  }

  @GetMapping("/{id}/history")
  public ResponseEntity<Page<EntityHistoryDTO>> getTaskHistory(@PathVariable Long id,
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    Pageable pageable = PageRequest.of(page, size);
    return ResponseEntity.ok(auditService.getTaskHistory(id, pageable));
  }

  @GetMapping("/cycle/{cycleId}")
  @Operation(summary = "Get tasks by cycle ID", description = "Returns all tasks for a specific cycle with pagination and sorting")
  public ResponseEntity<Page<TaskDTO>> getTasksByCycleId(@PathVariable Long cycleId,
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "createdAt") String sortBy,
      @RequestParam(defaultValue = "desc") String sortOrder) {
    String validSortBy = validateSortField(sortBy, "createdAt");
    Sort.Direction direction = sortOrder.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
    Pageable pageable = PageRequest.of(page, size, Sort.by(direction, validSortBy));
    return ResponseEntity.ok(taskService.getTasksByCycleId(cycleId, pageable));
  }

  @GetMapping("/cycle/{cycleId}/status/{status}")
  @Operation(summary = "Get tasks by cycle ID and status")
  public ResponseEntity<Page<TaskDTO>> getTasksByCycleIdAndStatus(@PathVariable Long cycleId,
      @PathVariable TaskStatus status, @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size, @RequestParam(defaultValue = "createdAt") String sortBy,
      @RequestParam(defaultValue = "desc") String sortOrder) {
    String validSortBy = validateSortField(sortBy, "createdAt");
    Sort.Direction direction = sortOrder.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
    Pageable pageable = PageRequest.of(page, size, Sort.by(direction, validSortBy));
    return ResponseEntity.ok(taskService.getTasksByCycleIdAndStatus(cycleId, status, pageable));
  }

  @GetMapping("/cycle/{cycleId}/filter")
  @Operation(summary = "Get tasks with multi-selection filters", description = "Filter tasks by multiple statuses, priorities, assignees, and category with optional exclusion")
  public ResponseEntity<Page<TaskDTO>> getTasksWithFilters(@PathVariable Long cycleId,
      @RequestParam(required = false) List<TaskStatus> statuses,
      @RequestParam(required = false) List<TaskPriority> priorities,
      @RequestParam(required = false) List<Long> assigneeIds,
      @RequestParam(required = false) TaskCategory category,
      @RequestParam(required = false, defaultValue = "false") Boolean exclude,
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "createdAt") String sortBy,
      @RequestParam(defaultValue = "desc") String sortOrder) {
    String validSortBy = validateSortField(sortBy, "createdAt");
    Sort.Direction direction = sortOrder.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
    Pageable pageable = PageRequest.of(page, size, Sort.by(direction, validSortBy));
    return ResponseEntity.ok(taskService.getTasksWithFilters(cycleId, statuses, priorities, assigneeIds, category,
        exclude, pageable));
  }

  @GetMapping("/cycle/{cycleId}/category/{category}")
  @Operation(summary = "Get tasks by cycle ID and category", description = "Returns tasks for a cycle filtered by category (PITCH_SCOPE or DEBT_IMPROVEMENT)")
  public ResponseEntity<Page<TaskDTO>> getTasksByCycleIdAndCategory(@PathVariable Long cycleId,
      @PathVariable TaskCategory category, @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size, @RequestParam(defaultValue = "createdAt") String sortBy,
      @RequestParam(defaultValue = "desc") String sortOrder) {
    String validSortBy = validateSortField(sortBy, "createdAt");
    Sort.Direction direction = sortOrder.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
    Pageable pageable = PageRequest.of(page, size, Sort.by(direction, validSortBy));
    return ResponseEntity.ok(taskService.getTasksByCycleIdAndCategory(cycleId, category, pageable));
  }

  @GetMapping("/assignee/{assigneeId}")
  @Operation(summary = "Get tasks by assignee ID")
  public ResponseEntity<List<TaskDTO>> getTasksByAssigneeId(@PathVariable Long assigneeId) {
    return ResponseEntity.ok(taskService.getTasksByAssigneeId(assigneeId));
  }

  @GetMapping("/person/{personId}")
  @Operation(summary = "Get tasks by person ID (assignee or pair)")
  public ResponseEntity<List<TaskDTO>> getTasksByPersonId(@PathVariable Long personId) {
    return ResponseEntity.ok(taskService.getTasksByPersonId(personId));
  }

  @GetMapping("/project/{projectId}")
  @Operation(summary = "Get tasks by project ID")
  public ResponseEntity<List<TaskDTO>> getTasksByProjectId(@PathVariable Long projectId) {
    return ResponseEntity.ok(taskService.getTasksByProjectId(projectId));
  }

  @GetMapping("/project/{projectId}/paged")
  @Operation(summary = "Get tasks by project ID with pagination")
  public ResponseEntity<Page<TaskDTO>> getTasksByProjectIdPaged(@PathVariable Long projectId,
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "createdAt") String sortBy,
      @RequestParam(defaultValue = "desc") String sortOrder) {
    String validSortBy = validateSortField(sortBy, "createdAt");
    Sort sort = Sort.by(sortOrder.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, validSortBy);
    Pageable pageable = PageRequest.of(page, size, sort);
    return ResponseEntity.ok(taskService.getTasksByProjectIdPaged(projectId, pageable));
  }

  @GetMapping("/project/{projectId}/category/{category}")
  @Operation(summary = "Get tasks by project ID and category with pagination")
  public ResponseEntity<Page<TaskDTO>> getTasksByProjectIdAndCategory(@PathVariable Long projectId,
      @PathVariable TaskCategory category, @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size, @RequestParam(defaultValue = "createdAt") String sortBy,
      @RequestParam(defaultValue = "desc") String sortOrder) {
    String validSortBy = validateSortField(sortBy, "createdAt");
    Sort sort = Sort.by(sortOrder.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, validSortBy);
    Pageable pageable = PageRequest.of(page, size, sort);
    return ResponseEntity.ok(taskService.getTasksByProjectIdAndCategory(projectId, category, pageable));
  }

  @GetMapping("/project/{projectId}/filter")
  @Operation(summary = "Get tasks with multi-selection filters for a project", 
      description = "Filter tasks by multiple statuses, priorities, assignees, and category with optional exclusion")
  public ResponseEntity<Page<TaskDTO>> getTasksByProjectIdWithFilters(@PathVariable Long projectId,
      @RequestParam(required = false) List<TaskStatus> statuses,
      @RequestParam(required = false) List<TaskPriority> priorities,
      @RequestParam(required = false) List<Long> assigneeIds,
      @RequestParam(required = false) TaskCategory category,
      @RequestParam(required = false, defaultValue = "false") Boolean exclude,
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "createdAt") String sortBy,
      @RequestParam(defaultValue = "desc") String sortOrder) {
    String validSortBy = validateSortField(sortBy, "createdAt");
    Sort.Direction direction = sortOrder.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
    Pageable pageable = PageRequest.of(page, size, Sort.by(direction, validSortBy));
    return ResponseEntity.ok(taskService.getTasksByProjectIdWithFilters(projectId, statuses, priorities, 
        assigneeIds, category, exclude, pageable));
  }

  @GetMapping("/project/{projectId}/statistics")
  @Operation(summary = "Get task statistics for a project")
  public ResponseEntity<TaskStatisticsDTO> getTaskStatisticsByProjectId(@PathVariable Long projectId) {
    return ResponseEntity.ok(taskService.getTaskStatisticsByProjectId(projectId));
  }

  @GetMapping("/cycle/{cycleId}/statistics")
  @Operation(summary = "Get task statistics for a cycle", description = "Returns aggregated statistics about tasks in a cycle")
  public ResponseEntity<TaskStatisticsDTO> getTaskStatisticsByCycleId(@PathVariable Long cycleId) {
    return ResponseEntity.ok(taskService.getTaskStatisticsByCycleId(cycleId));
  }

  @PostMapping
  @Operation(summary = "Create a new task")
  @ApiResponses({@ApiResponse(responseCode = "201", description = "Task created successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid data")})
  public ResponseEntity<TaskDTO> createTask(@Valid @RequestBody CreateTaskRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(taskService.createTask(request));
  }

  @PutMapping("/{id}")
  @Operation(summary = "Update a task")
  public ResponseEntity<TaskDTO> updateTask(@PathVariable Long id, @Valid @RequestBody CreateTaskRequest request) {
    return ResponseEntity.ok(taskService.updateTask(id, request));
  }

  @PatchMapping("/{id}/status")
  @Operation(summary = "Update task status", description = "Quick update of task status only")
  public ResponseEntity<TaskDTO> updateTaskStatus(@PathVariable Long id,
      @RequestBody Map<String, String> statusUpdate) {
    TaskStatus status = TaskStatus.valueOf(statusUpdate.get("status"));
    return ResponseEntity.ok(taskService.updateTaskStatus(id, status));
  }

  @PatchMapping("/{id}/priority")
  @Operation(summary = "Update task priority", description = "Quick update of task priority only")
  public ResponseEntity<TaskDTO> updateTaskPriority(@PathVariable Long id,
      @RequestBody Map<String, String> priorityUpdate) {
    TaskPriority priority = TaskPriority.valueOf(priorityUpdate.get("priority"));
    return ResponseEntity.ok(taskService.updateTaskPriority(id, priority));
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Delete a task")
  @ApiResponses({@ApiResponse(responseCode = "204", description = "Task deleted successfully"),
      @ApiResponse(responseCode = "404", description = "Task not found")})
  public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
    taskService.deleteTask(id);
    return ResponseEntity.noContent().build();
  }

  // ========== Sub-task Hierarchy Endpoints ==========

  @GetMapping("/{id}/subtasks")
  @Operation(summary = "Get sub-tasks", description = "Returns all direct children of the specified parent task")
  public ResponseEntity<List<TaskDTO>> getSubTasks(@PathVariable Long id) {
    return ResponseEntity.ok(taskService.getSubTasks(id));
  }

  @GetMapping("/cycle/{cycleId}/roots")
  @Operation(summary = "Get root tasks", description = "Returns all tasks in the cycle that have no parent (root level tasks)")
  public ResponseEntity<List<TaskDTO>> getRootTasks(@PathVariable Long cycleId) {
    return ResponseEntity.ok(taskService.getRootTasksByCycleId(cycleId));
  }

  @GetMapping("/cycle/{cycleId}/tree")
  @Operation(summary = "Get task tree", description = "Returns the complete task hierarchy for a cycle with nested children")
  public ResponseEntity<List<TaskDTO>> getTaskTree(@PathVariable Long cycleId) {
    return ResponseEntity.ok(taskService.getTaskTreeByCycleId(cycleId));
  }
}
