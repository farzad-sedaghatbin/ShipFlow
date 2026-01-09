package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.CreateTaskRequest;
import com.github.farzadsedaghatbin.shipflow.dto.TaskDTO;
import com.github.farzadsedaghatbin.shipflow.dto.TaskStatisticsDTO;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskCategory;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskPriority;
import com.github.farzadsedaghatbin.shipflow.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "Task management APIs for tracking independent work during cycles")
public class TaskController {

    private final TaskService taskService;

    // ========== Current User's Tasks ==========

    @GetMapping("/my")
    @Operation(summary = "Get current user's tasks",
               description = "Returns all tasks assigned to the currently authenticated user (as assignee or pair)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tasks retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "User not authenticated"),
        @ApiResponse(responseCode = "400", description = "User not linked to a person profile")
    })
    public ResponseEntity<List<TaskDTO>> getMyTasks() {
        return ResponseEntity.ok(taskService.getMyTasks());
    }

    @GetMapping("/my/cycle/{cycleId}")
    @Operation(summary = "Get current user's tasks by cycle",
               description = "Returns tasks for the current user filtered by cycle ID with pagination")
    public ResponseEntity<Page<TaskDTO>> getMyTasksByCycle(
            @PathVariable Long cycleId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder) {
        Sort.Direction direction = sortOrder.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        return ResponseEntity.ok(taskService.getMyTasksByCycle(cycleId, pageable));
    }

    // ========== General Task Management ==========

    @GetMapping
    @Operation(summary = "Get all tasks",
               description = "Returns all tasks in the system")
    public ResponseEntity<List<TaskDTO>> getAllTasks() {
        return ResponseEntity.ok(taskService.getAllTasks());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get task by ID")
    public ResponseEntity<TaskDTO> getTaskById(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.getTaskById(id));
    }

    @GetMapping("/cycle/{cycleId}")
    @Operation(summary = "Get tasks by cycle ID",
               description = "Returns all tasks for a specific cycle with pagination and sorting")
    public ResponseEntity<Page<TaskDTO>> getTasksByCycleId(
            @PathVariable Long cycleId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder) {
        Sort.Direction direction = sortOrder.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        return ResponseEntity.ok(taskService.getTasksByCycleId(cycleId, pageable));
    }

    @GetMapping("/cycle/{cycleId}/status/{status}")
    @Operation(summary = "Get tasks by cycle ID and status")
    public ResponseEntity<Page<TaskDTO>> getTasksByCycleIdAndStatus(
            @PathVariable Long cycleId, 
            @PathVariable TaskStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder) {
        Sort.Direction direction = sortOrder.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        return ResponseEntity.ok(taskService.getTasksByCycleIdAndStatus(cycleId, status, pageable));
    }

    @GetMapping("/cycle/{cycleId}/filter")
    @Operation(summary = "Get tasks with multi-selection filters",
               description = "Filter tasks by multiple statuses, priorities, assignees, and category with optional exclusion")
    public ResponseEntity<Page<TaskDTO>> getTasksWithFilters(
            @PathVariable Long cycleId,
            @RequestParam(required = false) List<TaskStatus> statuses,
            @RequestParam(required = false) List<TaskPriority> priorities,
            @RequestParam(required = false) List<Long> assigneeIds,
            @RequestParam(required = false) TaskCategory category,
            @RequestParam(required = false, defaultValue = "false") Boolean exclude,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder) {
        Sort.Direction direction = sortOrder.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        return ResponseEntity.ok(taskService.getTasksWithFilters(cycleId, statuses, priorities, assigneeIds, category, exclude, pageable));
    }

    @GetMapping("/cycle/{cycleId}/category/{category}")
    @Operation(summary = "Get tasks by cycle ID and category",
               description = "Returns tasks for a cycle filtered by category (PITCH_SCOPE or DEBT_IMPROVEMENT)")
    public ResponseEntity<Page<TaskDTO>> getTasksByCycleIdAndCategory(
            @PathVariable Long cycleId,
            @PathVariable TaskCategory category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder) {
        Sort.Direction direction = sortOrder.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
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

    @GetMapping("/cycle/{cycleId}/statistics")
    @Operation(summary = "Get task statistics for a cycle",
               description = "Returns aggregated statistics about tasks in a cycle")
    public ResponseEntity<TaskStatisticsDTO> getTaskStatisticsByCycleId(@PathVariable Long cycleId) {
        return ResponseEntity.ok(taskService.getTaskStatisticsByCycleId(cycleId));
    }

    @PostMapping
    @Operation(summary = "Create a new task")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Task created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid data")
    })
    public ResponseEntity<TaskDTO> createTask(@Valid @RequestBody CreateTaskRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.createTask(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a task")
    public ResponseEntity<TaskDTO> updateTask(@PathVariable Long id, @Valid @RequestBody CreateTaskRequest request) {
        return ResponseEntity.ok(taskService.updateTask(id, request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update task status",
               description = "Quick update of task status only")
    public ResponseEntity<TaskDTO> updateTaskStatus(
            @PathVariable Long id, 
            @RequestBody Map<String, String> statusUpdate) {
        TaskStatus status = TaskStatus.valueOf(statusUpdate.get("status"));
        return ResponseEntity.ok(taskService.updateTaskStatus(id, status));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a task")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Task deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Task not found")
    })
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }
}
