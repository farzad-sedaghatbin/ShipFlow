package com.github.farzadsedaghatbin.shipflow.controller.publicapi;

import com.github.farzadsedaghatbin.shipflow.dto.TaskDTO;
import com.github.farzadsedaghatbin.shipflow.dto.publicapi.PublicTaskDTO;
import com.github.farzadsedaghatbin.shipflow.dto.publicapi.UpdateTaskStatusRequest;
import com.github.farzadsedaghatbin.shipflow.entity.Task;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskStatus;
import com.github.farzadsedaghatbin.shipflow.repository.TaskRepository;
import com.github.farzadsedaghatbin.shipflow.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/public/tasks")
@RequiredArgsConstructor
@Tag(name = "Public API – Tasks", description = "Public API endpoints for task resources")
public class PublicTaskController {

  private final TaskRepository taskRepository;
  private final TaskService taskService;

  @GetMapping
  @Operation(summary = "List tasks with pagination")
  public ResponseEntity<Page<PublicTaskDTO>> listTasks(
      @RequestParam(required = false) Long cycleId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt"));
    Page<Task> tasks;
    if (cycleId != null) {
      tasks = taskRepository.findByCycleIdNotDeleted(cycleId, pageable);
    } else {
      tasks = taskRepository.findAllNotDeleted(pageable);
    }
    return ResponseEntity.ok(tasks.map(this::toDTO));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get a task by ID")
  public ResponseEntity<PublicTaskDTO> getTask(@PathVariable Long id) {
    return taskRepository.findByIdNotDeleted(id)
        .map(this::toDTO)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @PatchMapping("/{id}/status")
  @Operation(summary = "Update a task's status (e.g. from a GitHub Action)")
  public ResponseEntity<PublicTaskDTO> updateTaskStatus(
      @PathVariable Long id,
      @Valid @RequestBody UpdateTaskStatusRequest request) {
    TaskStatus newStatus;
    try {
      newStatus = TaskStatus.valueOf(request.getStatus().toUpperCase());
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().build();
    }
    TaskDTO updated = taskService.updateTaskStatus(id, newStatus);
    return taskRepository.findByIdNotDeleted(updated.getId())
        .map(this::toDTO)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  private PublicTaskDTO toDTO(Task t) {
    return PublicTaskDTO.builder()
        .id(t.getId())
        .title(t.getTitle())
        .description(t.getDescription())
        .status(t.getStatus() != null ? t.getStatus().name() : null)
        .priority(t.getPriority() != null ? t.getPriority().name() : null)
        .category(t.getCategory() != null ? t.getCategory().name() : null)
        .estimateHours(t.getEstimateHours())
        .actualHours(t.getActualHours())
        .cycleId(t.getCycle() != null ? t.getCycle().getId() : null)
        .pitchId(t.getPitch() != null ? t.getPitch().getId() : null)
        .assigneeId(t.getAssignee() != null ? t.getAssignee().getId() : null)
        .assigneeName(t.getAssignee() != null ? t.getAssignee().getName() : null)
        .parentTaskId(t.getParentTask() != null ? t.getParentTask().getId() : null)
        .targetReleaseId(t.getTargetRelease() != null ? t.getTargetRelease().getId() : null)
        .dueDate(t.getDueDate())
        .completedAt(t.getCompletedAt())
        .createdAt(t.getCreatedAt())
        .updatedAt(t.getUpdatedAt())
        .build();
  }
}
