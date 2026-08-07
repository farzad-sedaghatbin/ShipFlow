package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.AssignTaskCycleRequest;
import com.github.farzadsedaghatbin.shipflow.dto.BulkCreateTaskRequest;
import com.github.farzadsedaghatbin.shipflow.dto.BulkCreateTaskResult;
import com.github.farzadsedaghatbin.shipflow.dto.BulkTaskUpdateRequest;
import com.github.farzadsedaghatbin.shipflow.dto.BulkUpdateResult;
import com.github.farzadsedaghatbin.shipflow.dto.CreateTaskRequest;
import com.github.farzadsedaghatbin.shipflow.dto.ReorderRequest;
import com.github.farzadsedaghatbin.shipflow.dto.TaskAttachmentDTO;
import com.github.farzadsedaghatbin.shipflow.dto.TaskCycleHistoryDTO;
import com.github.farzadsedaghatbin.shipflow.dto.TaskDTO;
import com.github.farzadsedaghatbin.shipflow.dto.TaskStatisticsDTO;
import com.github.farzadsedaghatbin.shipflow.dto.UpdateStoryPointsRequest;
import com.github.farzadsedaghatbin.shipflow.dto.UpdateTaskAssigneeRequest;
import com.github.farzadsedaghatbin.shipflow.dto.audit.EntityHistoryDTO;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskCategory;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskPriority;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskStatus;
import com.github.farzadsedaghatbin.shipflow.service.AuditService;
import com.github.farzadsedaghatbin.shipflow.service.TaskAttachmentService;
import com.github.farzadsedaghatbin.shipflow.service.TaskCycleHistoryService;
import com.github.farzadsedaghatbin.shipflow.service.TaskService;
import com.github.farzadsedaghatbin.shipflow.service.TaskAttachmentService.DownloadResult;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
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
  private final TaskAttachmentService attachmentService;
  private final TaskCycleHistoryService taskCycleHistoryService;
  
  // Allowed fields for sorting to prevent runtime errors
  private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
      "id", "createdAt", "updatedAt", "title", "status", "priority", "category", "sortOrder"
  );
  
  /**
   * Validates and returns a safe sort field. Returns default if invalid.
   */
  private String validateSortField(String sortBy, String defaultField) {
    return ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : defaultField;
  }

  // ========== Current User's Tasks ==========

  @GetMapping("/my")
  @PreAuthorize("@permissionService.hasPermission('BACKLOG', 'READ')")
  @Operation(summary = "Get current user's tasks", description = "Returns all tasks assigned to the currently authenticated user (as assignee or pair) with pagination")
  @ApiResponses({@ApiResponse(responseCode = "200", description = "Tasks retrieved successfully"),
      @ApiResponse(responseCode = "401", description = "User not authenticated"),
      @ApiResponse(responseCode = "400", description = "User not linked to a person profile")})
  public ResponseEntity<Page<TaskDTO>> getMyTasks(@RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size, @RequestParam(defaultValue = "createdAt") String sortBy,
      @RequestParam(defaultValue = "desc") String sortOrder,
      @RequestParam(required = false) TaskCategory category) {
    String validSortBy = validateSortField(sortBy, "createdAt");
    Sort.Direction direction = sortOrder.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
    Pageable pageable = PageRequest.of(page, size, Sort.by(direction, validSortBy));
    return ResponseEntity.ok(taskService.getMyTasks(category, pageable));
  }

  @GetMapping("/my/cycle/{cycleId}")
  @PreAuthorize("@permissionService.hasPermission('BACKLOG', 'READ')")
  @Operation(summary = "Get current user's tasks by cycle", description = "Returns tasks for the current user filtered by cycle ID with pagination")
  public ResponseEntity<Page<TaskDTO>> getMyTasksByCycle(@PathVariable Long cycleId,
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "createdAt") String sortBy,
      @RequestParam(defaultValue = "desc") String sortOrder,
      @RequestParam(required = false) TaskCategory category) {
    String validSortBy = validateSortField(sortBy, "createdAt");
    Sort.Direction direction = sortOrder.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
    Pageable pageable = PageRequest.of(page, size, Sort.by(direction, validSortBy));
    return ResponseEntity.ok(taskService.getMyTasksByCycle(cycleId, category, pageable));
  }

  // ========== General Task Management ==========

  @GetMapping
  @PreAuthorize("@permissionService.hasPermission('BACKLOG', 'READ')")
  @Operation(summary = "Get all tasks", description = "Returns all tasks in the system with pagination and sorting")
  public ResponseEntity<Page<TaskDTO>> getAllTasks(@RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size, @RequestParam(defaultValue = "createdAt") String sortBy,
      @RequestParam(defaultValue = "desc") String sortOrder,
      @RequestParam(required = false) TaskCategory category) {
    String validSortBy = validateSortField(sortBy, "createdAt");
    Sort.Direction direction = sortOrder.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
    Pageable pageable = PageRequest.of(page, size, Sort.by(direction, validSortBy));
    return ResponseEntity.ok(taskService.getAllTasks(category, pageable));
  }

  @GetMapping("/search")
  @PreAuthorize("@permissionService.hasPermission('BACKLOG', 'READ')")
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
  @PreAuthorize("@permissionService.hasPermission('BACKLOG', 'READ')")
  @Operation(summary = "Get task by ID")
  public ResponseEntity<TaskDTO> getTaskById(@PathVariable Long id) {
    return ResponseEntity.ok(taskService.getTaskById(id));
  }

  @GetMapping("/{id}/history")
  @PreAuthorize("@permissionService.hasPermission('BACKLOG', 'READ')")
  public ResponseEntity<Page<EntityHistoryDTO>> getTaskHistory(@PathVariable Long id,
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    Pageable pageable = PageRequest.of(page, size);
    return ResponseEntity.ok(auditService.getTaskHistory(id, pageable));
  }

  @GetMapping("/{id}/cycle-history")
  @PreAuthorize("@permissionService.hasPermission('BACKLOG', 'READ')")
  @Operation(summary = "Get a task's cycle history", description = "Returns the cycle/status audit trail for a task — "
      + "one snapshot per creation, status change, or cycle (re)assignment — for reporting on which cycle a task "
      + "was in, and in which cycle it completed.")
  public ResponseEntity<List<TaskCycleHistoryDTO>> getTaskCycleHistory(@PathVariable Long id) {
    return ResponseEntity.ok(taskCycleHistoryService.getHistoryForTask(id));
  }

  @GetMapping("/cycle/{cycleId}/all")
  @PreAuthorize("@permissionService.hasPermission('BACKLOG', 'READ')")
  @Operation(summary = "Get all tasks by cycle ID (unpaginated)", description = "Returns all tasks for a sprint without pagination — used by Sprint Planning board")
  public ResponseEntity<List<TaskDTO>> getAllTasksByCycleId(@PathVariable Long cycleId) {
    return ResponseEntity.ok(taskService.getTasksByCycleId(cycleId));
  }

  @GetMapping("/cycle/{cycleId}")
  @PreAuthorize("@permissionService.hasPermission('BACKLOG', 'READ')")
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
  @PreAuthorize("@permissionService.hasPermission('BACKLOG', 'READ')")
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
  @PreAuthorize("@permissionService.hasPermission('BACKLOG', 'READ')")
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
  @PreAuthorize("@permissionService.hasPermission('BACKLOG', 'READ')")
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
  @PreAuthorize("@permissionService.hasPermission('BACKLOG', 'READ')")
  @Operation(summary = "Get tasks by assignee ID")
  public ResponseEntity<List<TaskDTO>> getTasksByAssigneeId(@PathVariable Long assigneeId) {
    return ResponseEntity.ok(taskService.getTasksByAssigneeId(assigneeId));
  }

  @GetMapping("/person/{personId}")
  @PreAuthorize("@permissionService.hasPermission('BACKLOG', 'READ')")
  @Operation(summary = "Get tasks by person ID (assignee or pair)")
  public ResponseEntity<List<TaskDTO>> getTasksByPersonId(@PathVariable Long personId) {
    return ResponseEntity.ok(taskService.getTasksByPersonId(personId));
  }

  @GetMapping("/pitch/{pitchId}")
  @PreAuthorize("@permissionService.hasPermission('BACKLOG', 'READ')")
  @Operation(summary = "Get tasks by pitch ID", description = "Returns all tasks linked to a specific pitch")
  public ResponseEntity<List<TaskDTO>> getTasksByPitchId(@PathVariable Long pitchId) {
    return ResponseEntity.ok(taskService.getTasksByPitchId(pitchId));
  }

  @GetMapping("/project/{projectId}")
  @PreAuthorize("@permissionService.hasPermission('BACKLOG', 'READ')")
  @Operation(summary = "Get tasks by project ID")
  public ResponseEntity<List<TaskDTO>> getTasksByProjectId(@PathVariable Long projectId) {
    return ResponseEntity.ok(taskService.getTasksByProjectId(projectId));
  }

  @GetMapping("/project/{projectId}/paged")
  @PreAuthorize("@permissionService.hasPermission('BACKLOG', 'READ')")
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
  @PreAuthorize("@permissionService.hasPermission('BACKLOG', 'READ')")
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
  @PreAuthorize("@permissionService.hasPermission('BACKLOG', 'READ')")
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

  @GetMapping("/project/{projectId}/backlog-tasks")
  @PreAuthorize("@permissionService.hasPermission('BACKLOG', 'READ')")
  @Operation(summary = "Get product backlog tasks for a project",
      description = "Returns top-level tasks not yet assigned to any sprint (cycle IS NULL). Scrum mode only.")
  public ResponseEntity<List<TaskDTO>> getProductBacklogTasks(@PathVariable Long projectId) {
    return ResponseEntity.ok(taskService.getProductBacklogTasks(projectId));
  }

  @GetMapping("/project/{projectId}/statistics")
  @PreAuthorize("@permissionService.hasPermission('BACKLOG', 'READ')")
  @Operation(summary = "Get task statistics for a project")
  public ResponseEntity<TaskStatisticsDTO> getTaskStatisticsByProjectId(@PathVariable Long projectId) {
    return ResponseEntity.ok(taskService.getTaskStatisticsByProjectId(projectId));
  }

  @GetMapping("/cycle/{cycleId}/statistics")
  @PreAuthorize("@permissionService.hasPermission('BACKLOG', 'READ')")
  @Operation(summary = "Get task statistics for a cycle", description = "Returns aggregated statistics about tasks in a cycle")
  public ResponseEntity<TaskStatisticsDTO> getTaskStatisticsByCycleId(@PathVariable Long cycleId) {
    return ResponseEntity.ok(taskService.getTaskStatisticsByCycleId(cycleId));
  }

  @PostMapping
  @PreAuthorize("@permissionService.hasPermission('BACKLOG', 'CREATE')")
  @Operation(summary = "Create a new task")
  @ApiResponses({@ApiResponse(responseCode = "201", description = "Task created successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid data")})
  public ResponseEntity<TaskDTO> createTask(@Valid @RequestBody CreateTaskRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(taskService.createTask(request));
  }

  @PutMapping("/{id}")
  @PreAuthorize("@permissionService.hasPermission('BACKLOG', 'UPDATE')")
  @Operation(summary = "Update a task")
  public ResponseEntity<TaskDTO> updateTask(@PathVariable Long id, @Valid @RequestBody CreateTaskRequest request) {
    return ResponseEntity.ok(taskService.updateTask(id, request));
  }

  @PatchMapping("/{id}/status")
  @PreAuthorize("@permissionService.hasPermission('BACKLOG', 'UPDATE')")
  @Operation(summary = "Update task status", description = "Quick update of task status only")
  public ResponseEntity<TaskDTO> updateTaskStatus(@PathVariable Long id,
      @RequestBody Map<String, String> statusUpdate) {
    TaskStatus status = TaskStatus.valueOf(statusUpdate.get("status"));
    return ResponseEntity.ok(taskService.updateTaskStatus(id, status));
  }

  @PatchMapping("/{id}/story-points")
  @PreAuthorize("@permissionService.hasPermission('BACKLOG', 'UPDATE')")
  @Operation(
      summary = "Update story points for a task",
      description =
          "Lightweight PATCH that only changes the storyPoints field. Pass null to clear the"
              + " estimate. Using a dedicated endpoint prevents accidental data loss from"
              + " full-update callers that do not include all task fields.")
  public ResponseEntity<TaskDTO> updateStoryPoints(
      @PathVariable Long id, @Valid @RequestBody UpdateStoryPointsRequest request) {
    return ResponseEntity.ok(taskService.updateStoryPoints(id, request.getStoryPoints()));
  }

  @PatchMapping("/{taskId}/cycle")
  @PreAuthorize("@permissionService.hasPermission('BACKLOG', 'UPDATE')")
  @Operation(
      summary = "Assign task to a cycle (sprint)",
      description =
          "Lightweight PATCH that only changes the cycleId. Pass null cycleId to move the task"
              + " to the product backlog. All other task fields are untouched.")
  public ResponseEntity<TaskDTO> assignTaskToCycle(
      @PathVariable Long taskId, @Valid @RequestBody AssignTaskCycleRequest request) {
    return ResponseEntity.ok(taskService.assignTaskToCycle(taskId, request.getCycleId()));
  }

  @PatchMapping("/{id}/priority")
  @PreAuthorize("@permissionService.hasPermission('BACKLOG', 'UPDATE')")
  @Operation(summary = "Update task priority", description = "Quick update of task priority only")
  public ResponseEntity<TaskDTO> updateTaskPriority(@PathVariable Long id,
      @RequestBody Map<String, String> priorityUpdate) {
    TaskPriority priority = TaskPriority.valueOf(priorityUpdate.get("priority"));
    return ResponseEntity.ok(taskService.updateTaskPriority(id, priority));
  }

  @PatchMapping("/{id}/release/{releaseId}")
  @PreAuthorize("@permissionService.hasPermission('BACKLOG', 'UPDATE')")
  @Operation(summary = "Set target release for a task")
  public ResponseEntity<TaskDTO> setTargetRelease(@PathVariable Long id, @PathVariable Long releaseId) {
    return ResponseEntity.ok(taskService.setTargetRelease(id, releaseId));
  }

  @PatchMapping("/{id}/unlink-release")
  @PreAuthorize("@permissionService.hasPermission('BACKLOG', 'UPDATE')")
  @Operation(summary = "Clear target release from a task")
  public ResponseEntity<TaskDTO> clearTargetRelease(@PathVariable Long id) {
    return ResponseEntity.ok(taskService.clearTargetRelease(id));
  }

  @PatchMapping("/{id}/assignee")
  @PreAuthorize("@permissionService.hasPermission('BACKLOG', 'UPDATE')")
  @Operation(
      summary = "Assign (or unassign) a task",
      description =
          "Lightweight PATCH that only changes the assignee. Pass a null assigneeId to unassign."
              + " All other task fields are untouched.")
  public ResponseEntity<TaskDTO> updateTaskAssignee(
      @PathVariable Long id, @RequestBody UpdateTaskAssigneeRequest request) {
    return ResponseEntity.ok(taskService.updateTaskAssignee(id, request.getAssigneeId()));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("@permissionService.hasPermission('BACKLOG', 'DELETE')")
  @Operation(summary = "Delete a task")
  @ApiResponses({@ApiResponse(responseCode = "204", description = "Task deleted successfully"),
      @ApiResponse(responseCode = "404", description = "Task not found")})
  public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
    taskService.deleteTask(id);
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/reorder")
  @PreAuthorize("@permissionService.hasPermission('BACKLOG', 'UPDATE')")
  @Operation(summary = "Reorder tasks (batch update sort order)",
      description = "Validates dependency constraints before persisting the new order. "
          + "Returns 400 if the proposed order would place a blocked task above its blocker.")
  @ApiResponses({@ApiResponse(responseCode = "200", description = "Tasks reordered successfully"),
      @ApiResponse(responseCode = "400", description = "Dependency constraint violation")})
  public ResponseEntity<Void> reorderTasks(@Valid @RequestBody ReorderRequest request) {
    taskService.reorderTasks(request);
    return ResponseEntity.ok().build();
  }

  // ========== Bulk Operations ==========

  // ========== CSV Export ==========

  @GetMapping("/export")
  @PreAuthorize("@permissionService.hasPermission('BACKLOG', 'READ')")
  @Operation(
      summary = "Export tasks as CSV",
      description =
          "Download all tasks for a project or cycle (optionally filtered by status, priority, "
              + "assignee and category) as a UTF-8 encoded CSV file. "
              + "Provide either projectId OR cycleId — not both.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "CSV file generated"),
      @ApiResponse(
          responseCode = "400",
          description = "Exactly one of projectId or cycleId must be provided")
  })
  public ResponseEntity<byte[]> exportTasksCsv(
      @RequestParam(required = false) Long projectId,
      @RequestParam(required = false) Long cycleId,
      @RequestParam(required = false) List<TaskStatus> statuses,
      @RequestParam(required = false) List<TaskPriority> priorities,
      @RequestParam(required = false) List<Long> assigneeIds,
      @RequestParam(required = false) TaskCategory category) {

    if ((projectId == null) == (cycleId == null)) {
      // both null or both provided
      return ResponseEntity.badRequest().build();
    }

    byte[] csv = taskService.exportTasksCsv(projectId, cycleId, statuses, priorities, assigneeIds, category);

    String filename = cycleId != null
        ? "tasks-cycle" + cycleId + "-" + java.time.LocalDate.now() + ".csv"
        : "tasks-" + projectId + "-" + java.time.LocalDate.now() + ".csv";

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
        .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
        .body(csv);
  }

  // ========== Bulk Operations ==========

  @PostMapping("/bulk-update")
  @PreAuthorize("@permissionService.hasPermission('BACKLOG', 'UPDATE')")
  @Operation(
      summary = "Bulk update tasks",
      description =
          "Apply a single action (ASSIGN, CHANGE_STATUS, CHANGE_PRIORITY, ADD_TAG, DELETE) to"
              + " multiple tasks. All task IDs must belong to the same project. Returns a result"
              + " with per-task error details so partial failures are visible to the caller.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Bulk operation completed (check successCount/failureCount)"),
      @ApiResponse(responseCode = "400", description = "Invalid request or tasks span multiple projects")
  })
  public ResponseEntity<BulkUpdateResult> bulkUpdateTasks(
      @Valid @RequestBody BulkTaskUpdateRequest request) {
    return ResponseEntity.ok(taskService.bulkUpdate(request));
  }

  @PostMapping("/bulk-create")
  @PreAuthorize("@permissionService.hasPermission('BACKLOG', 'CREATE')")
  @Operation(
      summary = "Bulk create tasks under a pitch",
      description =
          "Create multiple tasks under a single pitch and cycle in one transaction, e.g. from a"
              + " batch of accepted AI task suggestions. Returns a result with per-task error"
              + " details so partial failures are visible to the caller.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Bulk create completed (check successCount/failureCount)"),
      @ApiResponse(responseCode = "400", description = "Invalid request")
  })
  public ResponseEntity<BulkCreateTaskResult> bulkCreateTasks(
      @Valid @RequestBody BulkCreateTaskRequest request) {
    return ResponseEntity.ok(taskService.bulkCreate(request));
  }

  // ========== Sub-task Hierarchy Endpoints ==========

  @GetMapping("/{id}/subtasks")
  @PreAuthorize("@permissionService.hasPermission('BACKLOG', 'READ')")
  @Operation(summary = "Get sub-tasks", description = "Returns all direct children of the specified parent task")
  public ResponseEntity<List<TaskDTO>> getSubTasks(@PathVariable Long id) {
    return ResponseEntity.ok(taskService.getSubTasks(id));
  }

  @GetMapping("/cycle/{cycleId}/roots")
  @PreAuthorize("@permissionService.hasPermission('BACKLOG', 'READ')")
  @Operation(summary = "Get root tasks", description = "Returns all tasks in the cycle that have no parent (root level tasks)")
  public ResponseEntity<List<TaskDTO>> getRootTasks(@PathVariable Long cycleId) {
    return ResponseEntity.ok(taskService.getRootTasksByCycleId(cycleId));
  }

  @GetMapping("/cycle/{cycleId}/tree")
  @PreAuthorize("@permissionService.hasPermission('BACKLOG', 'READ')")
  @Operation(summary = "Get task tree", description = "Returns the complete task hierarchy for a cycle with nested children")
  public ResponseEntity<List<TaskDTO>> getTaskTree(@PathVariable Long cycleId) {
    return ResponseEntity.ok(taskService.getTaskTreeByCycleId(cycleId));
  }

  // ========== File Attachments ==========

  @PostMapping(value = "/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("@permissionService.hasPermission('BACKLOG', 'UPDATE')")
  @Operation(summary = "Upload attachment", description = "Upload a file attachment to a task (max 10 MB; images, PDF, DOCX, DOC, TXT, MD)")
  @ApiResponses({@ApiResponse(responseCode = "201", description = "Attachment uploaded"),
      @ApiResponse(responseCode = "400", description = "Invalid file or size exceeded"),
      @ApiResponse(responseCode = "404", description = "Task not found")})
  public ResponseEntity<TaskAttachmentDTO> uploadAttachment(
      @PathVariable Long id, @RequestParam("file") MultipartFile file) {
    return ResponseEntity.status(HttpStatus.CREATED).body(attachmentService.uploadAttachment(id, file));
  }

  @GetMapping("/{id}/attachments")
  @PreAuthorize("@permissionService.hasPermission('BACKLOG', 'READ')")
  @Operation(summary = "List attachments", description = "Returns all attachments for a task, newest first")
  public ResponseEntity<List<TaskAttachmentDTO>> listAttachments(@PathVariable Long id) {
    return ResponseEntity.ok(attachmentService.getAttachments(id));
  }

  @GetMapping("/{id}/attachments/{attachmentId}/download")
  @PreAuthorize("@permissionService.hasPermission('BACKLOG', 'READ')")
  @Operation(summary = "Download attachment", description = "Stream the raw file bytes for the given attachment")
  public ResponseEntity<Resource> downloadAttachment(
      @PathVariable Long id, @PathVariable Long attachmentId) {
    DownloadResult result = attachmentService.downloadAttachment(id, attachmentId);
    ContentDisposition disposition = ContentDisposition.attachment()
        .filename(result.originalFileName()).build();
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
        .contentType(MediaType.parseMediaType(result.contentType()))
        .body(result.resource());
  }

  @DeleteMapping("/{id}/attachments/{attachmentId}")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Delete attachment", description = "Delete a task attachment (uploader or ADMIN only)")
  @ApiResponses({@ApiResponse(responseCode = "204", description = "Attachment deleted"),
      @ApiResponse(responseCode = "403", description = "Not the uploader or ADMIN"),
      @ApiResponse(responseCode = "404", description = "Attachment not found")})
  public ResponseEntity<Void> deleteAttachment(
      @PathVariable Long id, @PathVariable Long attachmentId) {
    attachmentService.deleteAttachment(id, attachmentId);
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/{id}/move-to-project/{projectId}")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Move a task (and its subtasks) to a different project (admin only)")
  public ResponseEntity<TaskDTO> moveTaskToProject(@PathVariable Long id, @PathVariable Long projectId) {
    return ResponseEntity.ok(taskService.moveTaskToProject(id, projectId));
  }
}
