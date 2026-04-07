package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.BulkTaskUpdateRequest;
import com.github.farzadsedaghatbin.shipflow.dto.BulkUpdateResult;
import com.github.farzadsedaghatbin.shipflow.dto.CreateTaskRequest;
import com.github.farzadsedaghatbin.shipflow.dto.TaskAttachmentDTO;
import com.github.farzadsedaghatbin.shipflow.dto.TaskDTO;
import com.github.farzadsedaghatbin.shipflow.dto.TaskDependencyDTO;
import com.github.farzadsedaghatbin.shipflow.dto.TaskStatisticsDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.HillChartPoint;
import com.github.farzadsedaghatbin.shipflow.entity.Person;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.Task;
import com.github.farzadsedaghatbin.shipflow.entity.Team;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.enums.BulkAction;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PermissionType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ResourceType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskCategory;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskPriority;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskStatus;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.repository.HillChartPointRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PersonRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TaskAttachmentRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TaskRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TeamRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import com.github.farzadsedaghatbin.shipflow.event.TaskStatusChangedEvent;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class TaskService {

  private final TaskRepository taskRepository;
  private final CycleRepository cycleRepository;
  private final PersonRepository personRepository;
  private final UserRepository userRepository;
  private final PitchRepository pitchRepository;
  private final HillChartPointRepository hillChartPointRepository;
  private final TaskAttachmentRepository attachmentRepository;
  private final TeamRepository teamRepository;
  private final DashboardNotificationService notificationService;
  private final MessageService messageService;
  private final ApplicationEventPublisher eventPublisher;
  private final PermissionService permissionService;

  public List<TaskDTO> getAllTasks() {
    return taskRepository.findAllNotDeleted().stream().map(this::toDTO).collect(Collectors.toList());
  }

  public Page<TaskDTO> getAllTasks(Pageable pageable) {
    return taskRepository.findAllNotDeleted(pageable).map(this::toDTO);
  }

  public TaskDTO getTaskById(Long id) {
    Task task = taskRepository.findByIdNotDeleted(id)
        .orElseThrow(() -> new IllegalArgumentException("Task not found with id: " + id));
    TaskDTO dto = toDTO(task);
    // Attachments are only loaded for the single-task detail view to avoid N+1 on list endpoints
    dto.setAttachments(attachmentRepository.findByTaskIdOrderByCreatedAtDesc(id).stream()
        .map(a -> TaskAttachmentDTO.builder()
            .id(a.getId()).taskId(a.getTask().getId()).fileName(a.getFileName())
            .fileSize(a.getFileSize()).contentType(a.getContentType())
            .uploadedById(a.getUploadedBy() != null ? a.getUploadedBy().getId() : null)
            .uploadedByUsername(a.getUploadedBy() != null ? a.getUploadedBy().getUsername() : null)
            .createdAt(a.getCreatedAt()).build())
        .collect(Collectors.toList()));
    return dto;
  }

  public Page<TaskDTO> getTasksByCycleId(Long cycleId, Pageable pageable) {
    return taskRepository.findByCycleId(cycleId, pageable).map(this::toDTO);
  }

  public List<TaskDTO> getTasksByCycleId(Long cycleId) {
    return taskRepository.findByCycleIdOrderByPriority(cycleId).stream().map(this::toDTO)
        .collect(Collectors.toList());
  }

  public Page<TaskDTO> getTasksByCycleIdAndStatus(Long cycleId, TaskStatus status, Pageable pageable) {
    return taskRepository.findByCycleIdAndStatus(cycleId, status, pageable).map(this::toDTO);
  }

  public List<TaskDTO> getTasksByCycleIdAndStatus(Long cycleId, TaskStatus status) {
    return taskRepository.findByCycleIdAndStatus(cycleId, status).stream().map(this::toDTO)
        .collect(Collectors.toList());
  }

  public List<TaskDTO> getTasksByAssigneeId(Long assigneeId) {
    return taskRepository.findByAssigneeId(assigneeId).stream().map(this::toDTO).collect(Collectors.toList());
  }

  public List<TaskDTO> getTasksByPersonId(Long personId) {
    return taskRepository.findByPersonId(personId).stream().map(this::toDTO).collect(Collectors.toList());
  }

  /**
   * Search tasks by title or description. Minimum 3 characters required to
   * prevent performance issues with large datasets.
   */
  public Page<TaskDTO> searchTasks(String query, Pageable pageable) {
    if (query == null || query.trim().length() < 3) {
      return Page.empty(pageable);
    }
    return taskRepository.searchTasks(query.trim(), pageable).map(this::toDTO);
  }

  public List<TaskDTO> getTasksByPitchId(Long pitchId) {
    return taskRepository.findByPitchId(pitchId).stream().map(this::toDTO).collect(Collectors.toList());
  }

  public List<TaskDTO> getTasksByProjectId(Long projectId) {
    return taskRepository.findByProjectId(projectId).stream().map(this::toDTO).collect(Collectors.toList());
  }

  public Page<TaskDTO> getTasksByProjectIdPaged(Long projectId, Pageable pageable) {
    return taskRepository.findByProjectIdPaged(projectId, pageable).map(this::toDTO);
  }

  public Page<TaskDTO> getTasksByProjectIdAndCategory(Long projectId, TaskCategory category, Pageable pageable) {
    return taskRepository.findByProjectIdAndCategory(projectId, category, pageable).map(this::toDTO);
  }

  public TaskStatisticsDTO getTaskStatisticsByProjectId(Long projectId) {
    List<Task> tasks = taskRepository.findByProjectId(projectId);

    long totalTasks = tasks.size();
    long backlogTasks = tasks.stream().filter(t -> t.getStatus() == TaskStatus.BACKLOG).count();
    long todoTasks = tasks.stream().filter(t -> t.getStatus() == TaskStatus.TODO).count();
    long inProgressTasks = tasks.stream().filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS).count();
    long blockedTasks = tasks.stream().filter(t -> t.getStatus() == TaskStatus.BLOCKED).count();
    long inReviewTasks = tasks.stream().filter(t -> t.getStatus() == TaskStatus.IN_REVIEW).count();
    long doneTasks = tasks.stream().filter(t -> t.getStatus() == TaskStatus.DONE).count();
    long cancelledTasks = tasks.stream().filter(t -> t.getStatus() == TaskStatus.CANCELLED).count();

    double completionPercentage = totalTasks > 0 ? (doneTasks * 100.0 / totalTasks) : 0;

    return TaskStatisticsDTO.builder().totalTasks((int) totalTasks).backlogTasks((int) backlogTasks)
        .todoTasks((int) todoTasks).inProgressTasks((int) inProgressTasks).blockedTasks((int) blockedTasks)
        .inReviewTasks((int) inReviewTasks).doneTasks((int) doneTasks).cancelledTasks((int) cancelledTasks)
        .completionPercentage(Math.round(completionPercentage * 100.0) / 100.0).build();
  }

  public TaskDTO createTask(CreateTaskRequest request) {
    Cycle cycle = cycleRepository.findById(request.getCycleId())
        .orElseThrow(() -> new IllegalArgumentException("Cycle not found with id: " + request.getCycleId()));

    // Validate parent task if provided
    Task parentTask = null;
    if (request.getParentTaskId() != null) {
      parentTask = taskRepository.findById(request.getParentTaskId()).orElseThrow(
          () -> new IllegalArgumentException("Parent task not found with id: " + request.getParentTaskId()));

      // Ensure parent task belongs to the same cycle
      if (!parentTask.getCycle().getId().equals(request.getCycleId())) {
        throw new IllegalArgumentException(messageService.getMessage("error.task.parent.different.cycle"));
      }
    }

    Task task = Task.builder().title(request.getTitle()).description(request.getDescription()).cycle(cycle)
        .parentTask(parentTask).status(request.getStatus() != null ? request.getStatus() : TaskStatus.BACKLOG)
        .priority(request.getPriority() != null ? request.getPriority() : TaskPriority.MEDIUM)
        .category(request.getCategory() != null ? request.getCategory() : TaskCategory.PITCH_SCOPE)
        .estimateHours(request.getEstimateHours()).actualHours(request.getActualHours())
        .dueDate(request.getDueDate()).tags(request.getTags()).build();

    Person assignee = null;
    if (request.getAssigneeId() != null) {
      assignee = personRepository.findById(request.getAssigneeId()).orElseThrow(
          () -> new IllegalArgumentException("Assignee not found with id: " + request.getAssigneeId()));
      task.setAssignee(assignee);
    }

    if (request.getPairAssigneeId() != null) {
      Person pairAssignee = personRepository.findById(request.getPairAssigneeId())
          .orElseThrow(() -> new IllegalArgumentException(
              "Pair assignee not found with id: " + request.getPairAssigneeId()));
      task.setPairAssignee(pairAssignee);
    }

    // Set pitch if provided
    if (request.getPitchId() != null) {
      Pitch pitch = pitchRepository.findById(request.getPitchId()).orElseThrow(
          () -> new IllegalArgumentException("Pitch not found with id: " + request.getPitchId()));
      task.setPitch(pitch);
    }

    // Set scope if provided
    if (request.getScopeId() != null) {
      HillChartPoint scope = hillChartPointRepository.findById(request.getScopeId()).orElseThrow(
          () -> new IllegalArgumentException("Scope not found with id: " + request.getScopeId()));
      task.setScope(scope);
    }

    // Set team if provided
    if (request.getTeamId() != null) {
      Team team = teamRepository.findById(request.getTeamId())
          .orElseThrow(
              () -> new IllegalArgumentException("Team not found with id: " + request.getTeamId()));
      task.setTeam(team);
    }

    // Set created by current user's person
    try {
      Person currentPerson = getCurrentUserPerson();
      task.setCreatedBy(currentPerson);
    } catch (Exception e) {
      // If user is not linked to a person, allow task creation without createdBy
    }

    Task saved = taskRepository.save(task);

    // Auto-create hill chart scope for root tasks with pitch (Scope-Task Bridge)
    if (shouldCreateScopeAutomatically(request, saved)) {
      createLinkedScope(saved, request.getInitialHillPosition());
    }

    // Send notification if task is assigned during creation
    if (assignee != null && assignee.getUser() != null) {
      try {
        notificationService.notifyTaskAssignment(saved, assignee.getUser());
      } catch (Exception e) {
        log.error("Failed to send task assignment notification for task {} to user {}", saved.getId(),
            assignee.getUser().getUsername(), e);
      }
    }

    return toDTO(saved);
  }

  /**
   * Check if a scope should be auto-created for this task.
   * Scope is created when:
   * - createScopeAutomatically flag is true (or null, default true)
   * - Task has a pitch association
   * - Task has no parent (is a root task)
   * - Task doesn't already have an explicit scope
   */
  private boolean shouldCreateScopeAutomatically(CreateTaskRequest request, Task task) {
    boolean createFlag = request.getCreateScopeAutomatically() == null || request.getCreateScopeAutomatically();
    boolean hasPitch = task.getPitch() != null;
    boolean isRootTask = task.getParentTask() == null;
    boolean noExistingScope = request.getScopeId() == null;

    return createFlag && hasPitch && isRootTask && noExistingScope;
  }

  /**
   * Create a hill chart scope linked to the given task.
   */
  private void createLinkedScope(Task task, Integer initialPosition) {
    int position = initialPosition != null ? initialPosition : 0;

    HillChartPoint scope = HillChartPoint.builder()
        .pitch(task.getPitch())
        .scope(task.getTitle())
        .description(task.getDescription() != null ? task.getDescription() : task.getTitle())
        .position(position)
        .linkedTask(task)
        .autoProgressEnabled(true)
        .build();

    HillChartPoint savedScope = hillChartPointRepository.save(scope);
    log.info("Auto-created hill chart scope {} for task {} (pitch {})",
        savedScope.getId(), task.getId(), task.getPitch() != null ? task.getPitch().getId() : "null");
  }

  public TaskDTO updateTask(Long id, CreateTaskRequest request) {
    Task task = taskRepository.findByIdNotDeleted(id)
        .orElseThrow(() -> new IllegalArgumentException("Task not found with id: " + id));

    // Track old values for notification purposes
    Person oldAssignee = task.getAssignee();
    TaskStatus oldStatus = task.getStatus();
    TaskPriority oldPriority = task.getPriority();

    // Validate and update parent task if changed
    if (request.getParentTaskId() != null) {
      if (!request.getParentTaskId().equals(task.getParentTask() != null ? task.getParentTask().getId() : null)) {
        Task parentTask = taskRepository.findById(request.getParentTaskId())
            .orElseThrow(() -> new IllegalArgumentException(
                "Parent task not found with id: " + request.getParentTaskId()));

        // Prevent circular references
        if (isCircularReference(task, parentTask)) {
          throw new IllegalArgumentException(messageService.getMessage("error.task.circular.reference"));
        }

        // Ensure parent task belongs to the same cycle
        if (!parentTask.getCycle().getId().equals(task.getCycle().getId())) {
          throw new IllegalArgumentException(messageService.getMessage("error.task.parent.different.cycle"));
        }

        task.setParentTask(parentTask);
      }
    } else {
      task.setParentTask(null);
    }

    task.setTitle(request.getTitle());
    task.setDescription(request.getDescription());

    if (request.getStatus() != null) {
      task.setStatus(request.getStatus());

      // Track completion time
      if (request.getStatus() == TaskStatus.DONE && oldStatus != TaskStatus.DONE) {
        task.setCompletedAt(LocalDateTime.now());
      } else if (request.getStatus() != TaskStatus.DONE) {
        task.setCompletedAt(null);
      }
    }

    if (request.getPriority() != null) {
      task.setPriority(request.getPriority());
    }

    if (request.getCategory() != null) {
      task.setCategory(request.getCategory());
    }

    task.setEstimateHours(request.getEstimateHours());
    task.setActualHours(request.getActualHours());
    task.setDueDate(request.getDueDate());
    task.setTags(request.getTags());

    // Handle assignee changes
    Person newAssignee = null;
    if (request.getAssigneeId() != null) {
      newAssignee = personRepository.findById(request.getAssigneeId()).orElseThrow(
          () -> new IllegalArgumentException("Assignee not found with id: " + request.getAssigneeId()));
      task.setAssignee(newAssignee);
    } else {
      task.setAssignee(null);
    }

    // Handle pitch changes
    if (request.getPitchId() != null) {
      Pitch pitch = pitchRepository.findById(request.getPitchId()).orElseThrow(
          () -> new IllegalArgumentException("Pitch not found with id: " + request.getPitchId()));
      task.setPitch(pitch);
    } else {
      task.setPitch(null);
    }

    // Handle scope changes
    if (request.getScopeId() != null) {
      HillChartPoint scope = hillChartPointRepository.findById(request.getScopeId()).orElseThrow(
          () -> new IllegalArgumentException("Scope not found with id: " + request.getScopeId()));
      task.setScope(scope);
    } else {
      task.setScope(null);
    }

    if (request.getPairAssigneeId() != null) {
      Person pairAssignee = personRepository.findById(request.getPairAssigneeId())
          .orElseThrow(() -> new IllegalArgumentException(
              "Pair assignee not found with id: " + request.getPairAssigneeId()));
      task.setPairAssignee(pairAssignee);
    } else {
      task.setPairAssignee(null);
    }

    // Handle team changes
    if (request.getTeamId() != null) {
      Team team = teamRepository.findById(request.getTeamId())
          .orElseThrow(
              () -> new IllegalArgumentException("Team not found with id: " + request.getTeamId()));
      task.setTeam(team);
    } else {
      task.setTeam(null);
    }

    // Handle cycle changes — persist if cycle was edited in task detail page
    if (request.getCycleId() != null && !request.getCycleId().equals(task.getCycle().getId())) {
      Cycle newCycle = cycleRepository.findById(request.getCycleId())
          .orElseThrow(() -> new IllegalArgumentException(
              "Cycle not found with id: " + request.getCycleId()));
      task.setCycle(newCycle);
      // Clear parent task if it now belongs to a different cycle
      if (task.getParentTask() != null
          && !task.getParentTask().getCycle().getId().equals(request.getCycleId())) {
        task.setParentTask(null);
      }
    }

    Task saved = taskRepository.save(task);

    // Send notifications after save
    try {
      // Check if assignee changed
      if (hasAssigneeChanged(oldAssignee, newAssignee)) {
        if (oldAssignee != null && newAssignee != null) {
          // Reassignment
          notificationService.notifyTaskReassignment(saved, oldAssignee.getUser(), newAssignee.getUser());
        } else if (newAssignee != null) {
          // New assignment
          notificationService.notifyTaskAssignment(saved, newAssignee.getUser());
        }
      }

      // Check if status changed
      if (request.getStatus() != null && !request.getStatus().equals(oldStatus)) {
        notificationService.notifyTaskStatusChange(saved, oldStatus, request.getStatus());

        // Publish task status changed event for scope progress sync
        publishTaskStatusChangedEvent(saved, oldStatus, request.getStatus());
      }

      // Check if priority changed to high
      if (request.getPriority() != null && !request.getPriority().equals(oldPriority)) {
        notificationService.notifyTaskPriorityChange(saved, request.getPriority());
      }
    } catch (Exception e) {
      log.error("Failed to send task update notifications for task {}", saved.getId(), e);
    }

    return toDTO(saved);
  }

  private boolean hasAssigneeChanged(Person oldAssignee, Person newAssignee) {
    if (oldAssignee == null && newAssignee == null) {
      return false;
    }
    if (oldAssignee == null || newAssignee == null) {
      return true;
    }
    return !oldAssignee.getId().equals(newAssignee.getId());
  }

  public TaskDTO updateTaskStatus(Long id, TaskStatus status) {
    return updateTaskStatus(id, status, null);
  }

  public TaskDTO updateTaskStatus(Long id, TaskStatus status, String comment) {
    Task task = taskRepository.findByIdNotDeleted(id)
        .orElseThrow(() -> new IllegalArgumentException("Task not found with id: " + id));

    TaskStatus oldStatus = task.getStatus();
    task.setStatus(status);

    if (status == TaskStatus.DONE && oldStatus != TaskStatus.DONE) {
      task.setCompletedAt(LocalDateTime.now());
    } else if (status != TaskStatus.DONE) {
      task.setCompletedAt(null);
    }

    Task saved = taskRepository.save(task);

    if (comment != null && !comment.isBlank()) {
      log.info("Task {} status set to {} via public API. Comment: {}", id, status, comment);
    }

    // Publish task status changed event for scope progress sync
    if (!status.equals(oldStatus)) {
      publishTaskStatusChangedEvent(saved, oldStatus, status);
    }

    return toDTO(saved);
  }

  /**
   * Publish a task status changed event for scope progress synchronization.
   */
  private void publishTaskStatusChangedEvent(Task task, TaskStatus oldStatus, TaskStatus newStatus) {
    TaskStatusChangedEvent event = new TaskStatusChangedEvent(
        this,
        task.getId(),
        task.getPitch() != null ? task.getPitch().getId() : null,
        task.getScope() != null ? task.getScope().getId() : null,
        task.getParentTask() != null ? task.getParentTask().getId() : null,
        oldStatus,
        newStatus
    );
    eventPublisher.publishEvent(event);
    log.debug("Published TaskStatusChangedEvent for task {} (status: {} -> {})",
        task.getId(), oldStatus, newStatus);
  }

  public TaskDTO updateTaskPriority(Long id, TaskPriority priority) {
    Task task = taskRepository.findByIdNotDeleted(id)
        .orElseThrow(() -> new IllegalArgumentException("Task not found with id: " + id));

    task.setPriority(priority);
    Task saved = taskRepository.save(task);
    return toDTO(saved);
  }

  // ========== Bulk operations ==========

  /**
   * Apply a single action to a list of tasks in one transaction.
   *
   * <p>All task IDs must belong to the same project; if they span multiple projects the whole
   * request is rejected. Task IDs that are not found or are already soft-deleted are reported as
   * failures. Per-task errors are collected so partial-success results are visible to the caller.
   *
   * <p>The DELETE action additionally requires {@code BACKLOG DELETE} permission because the
   * endpoint is protected with the weaker {@code BACKLOG UPDATE} permission only.
   */
  @Transactional
  public BulkUpdateResult bulkUpdate(BulkTaskUpdateRequest request) {
    // Fix #1: enforce DELETE permission when the action is DELETE
    if (request.getAction() == BulkAction.DELETE
        && !permissionService.hasPermission(ResourceType.BACKLOG, PermissionType.DELETE)) {
      throw new AccessDeniedException("BACKLOG DELETE permission is required to bulk-delete tasks");
    }

    // Load tasks and partition into found-and-active vs missing/deleted
    List<Task> allLoaded = taskRepository.findAllById(request.getTaskIds());
    Set<Long> loadedIds =
        allLoaded.stream().map(Task::getId).collect(Collectors.toSet());

    List<String> errors = new ArrayList<>();

    // Fix #2: report missing or deleted IDs explicitly rather than silently dropping them
    for (Long requestedId : request.getTaskIds()) {
      if (!loadedIds.contains(requestedId)) {
        errors.add("Task " + requestedId + ": not found");
      }
    }
    for (Task t : allLoaded) {
      if (t.getDeletedAt() != null) {
        errors.add("Task " + t.getId() + ": already deleted");
      }
    }

    List<Task> tasks =
        allLoaded.stream().filter(t -> t.getDeletedAt() == null).collect(Collectors.toList());

    if (tasks.isEmpty()) {
      return BulkUpdateResult.builder()
          .successCount(0)
          .failureCount(errors.size())
          .errors(errors)
          .build();
    }

    // Validate all active tasks belong to the same project
    Set<Long> projectIds =
        tasks.stream()
            .map(t -> t.getCycle().getProject().getId())
            .collect(Collectors.toSet());
    if (projectIds.size() > 1) {
      throw new IllegalArgumentException("All tasks must belong to the same project");
    }

    int successCount = 0;

    for (Task task : tasks) {
      try {
        applyBulkAction(task, request.getAction(), request.getValue());
        successCount++;
      } catch (Exception e) {
        errors.add("Task " + task.getId() + ": " + e.getMessage());
      }
    }

    return BulkUpdateResult.builder()
        .successCount(successCount)
        .failureCount(errors.size())
        .errors(errors)
        .build();
  }

  private void applyBulkAction(Task task, BulkAction action, String value) {
    switch (action) {
      case ASSIGN -> {
        if (value == null || value.isBlank()) {
          task.setAssignee(null);
        } else {
          Long assigneeId = Long.parseLong(value.trim());
          Person assignee =
              personRepository
                  .findById(assigneeId)
                  .orElseThrow(
                      () -> new IllegalArgumentException("Person not found: " + assigneeId));
          task.setAssignee(assignee);
        }
        taskRepository.save(task);
      }
      case CHANGE_STATUS -> {
        // Fix #4: validate value before Enum.valueOf to give a clear error message
        if (value == null || value.isBlank()) {
          throw new IllegalArgumentException("Status value must not be blank");
        }
        TaskStatus status = TaskStatus.valueOf(value.trim());
        TaskStatus oldStatus = task.getStatus();
        task.setStatus(status);
        if (status == TaskStatus.DONE && oldStatus != TaskStatus.DONE) {
          task.setCompletedAt(LocalDateTime.now());
        } else if (status != TaskStatus.DONE) {
          task.setCompletedAt(null);
        }
        taskRepository.save(task);
        if (!status.equals(oldStatus)) {
          publishTaskStatusChangedEvent(task, oldStatus, status);
        }
      }
      case CHANGE_PRIORITY -> {
        // Fix #4: validate value before Enum.valueOf to give a clear error message
        if (value == null || value.isBlank()) {
          throw new IllegalArgumentException("Priority value must not be blank");
        }
        task.setPriority(TaskPriority.valueOf(value.trim()));
        taskRepository.save(task);
      }
      case ADD_TAG -> {
        // Fix #3: reject tags that contain commas; use exact token match to prevent substring dupes
        if (value == null || value.isBlank()) {
          throw new IllegalArgumentException("Tag value must not be blank");
        }
        String tag = value.trim();
        if (tag.contains(",")) {
          throw new IllegalArgumentException("Tag must not contain commas: " + tag);
        }
        String existing = task.getTags();
        if (existing == null || existing.isBlank()) {
          task.setTags(tag);
        } else {
          boolean alreadyPresent =
              java.util.Arrays.stream(existing.split(","))
                  .map(String::trim)
                  .anyMatch(t -> t.equalsIgnoreCase(tag));
          if (!alreadyPresent) {
            task.setTags(existing + "," + tag);
          }
        }
        taskRepository.save(task);
      }
      case DELETE -> {
        User currentUser = getCurrentUser();
        task.setDeletedAt(LocalDateTime.now());
        task.setDeletedBy(currentUser);
        taskRepository.save(task);
      }
      default -> throw new IllegalArgumentException("Unknown bulk action: " + action);
    }
  }

  public void deleteTask(Long id) {
    Task task = taskRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));

    // Check if already deleted
    if (task.getDeletedAt() != null) {
      throw new IllegalStateException("Task is already deleted");
    }

    User currentUser = getCurrentUser();

    // Perform soft delete
    task.setDeletedAt(LocalDateTime.now());
    task.setDeletedBy(currentUser);
    taskRepository.save(task);
  }

  public TaskStatisticsDTO getTaskStatisticsByCycleId(Long cycleId) {
    Cycle cycle = cycleRepository.findById(cycleId)
        .orElseThrow(() -> new IllegalArgumentException("Cycle not found with id: " + cycleId));

    int totalTasks = taskRepository.countByCycleId(cycleId);
    int backlogTasks = taskRepository.countByCycleIdAndStatus(cycleId, TaskStatus.BACKLOG);
    int todoTasks = taskRepository.countByCycleIdAndStatus(cycleId, TaskStatus.TODO);
    int inProgressTasks = taskRepository.countByCycleIdAndStatus(cycleId, TaskStatus.IN_PROGRESS);
    int blockedTasks = taskRepository.countByCycleIdAndStatus(cycleId, TaskStatus.BLOCKED);
    int inReviewTasks = taskRepository.countByCycleIdAndStatus(cycleId, TaskStatus.IN_REVIEW);
    int doneTasks = taskRepository.countByCycleIdAndStatus(cycleId, TaskStatus.DONE);
    int cancelledTasks = taskRepository.countByCycleIdAndStatus(cycleId, TaskStatus.CANCELLED);

    Double totalEstimateHours = taskRepository.getTotalEstimateHoursByCycleId(cycleId);
    Double totalActualHours = taskRepository.getTotalActualHoursByCycleId(cycleId);
    int distinctAssignees = taskRepository.countDistinctAssigneesByCycleId(cycleId);

    double completionPercentage = totalTasks > 0 ? (double) doneTasks / totalTasks * 100 : 0.0;

    double avgTasksPerPerson = distinctAssignees > 0 ? (double) totalTasks / distinctAssignees : 0.0;

    return TaskStatisticsDTO.builder().cycleId(cycleId).cycleName(cycle.getName()).totalTasks(totalTasks)
        .backlogTasks(backlogTasks).todoTasks(todoTasks).inProgressTasks(inProgressTasks)
        .blockedTasks(blockedTasks).inReviewTasks(inReviewTasks).doneTasks(doneTasks)
        .cancelledTasks(cancelledTasks).completionPercentage(Math.round(completionPercentage * 100.0) / 100.0)
        .totalEstimateHours(totalEstimateHours != null ? totalEstimateHours : 0.0)
        .totalActualHours(totalActualHours != null ? totalActualHours : 0.0)
        .avgTasksPerPerson(Math.round(avgTasksPerPerson * 100.0) / 100.0).build();
  }

  // ========== Methods for current user's tasks ==========

  private Person getCurrentUserPerson() {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    User user = userRepository.findByUsernameWithPerson(username)
        .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

    if (user.getPerson() == null) {
      throw new IllegalArgumentException(messageService.getMessage("error.user.no.person.profile"));
    }

    return user.getPerson();
  }

  private User getCurrentUser() {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    return userRepository.findByUsername(username)
        .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
  }

  public List<TaskDTO> getMyTasks() {
    Person person = getCurrentUserPerson();
    return taskRepository.findByPersonId(person.getId()).stream().map(this::toDTO).collect(Collectors.toList());
  }

  public Page<TaskDTO> getMyTasks(Pageable pageable) {
    Person person = getCurrentUserPerson();
    Page<Task> tasks = taskRepository.findByPersonId(person.getId(), pageable);
    return tasks.map(this::toDTO);
  }

  public Page<TaskDTO> getMyTasksByCycle(Long cycleId, Pageable pageable) {
    Person person = getCurrentUserPerson();
    return taskRepository.findByCycleIdAndPersonId(cycleId, person.getId(), pageable).map(this::toDTO);
  }

  public List<TaskDTO> getMyTasksByCycle(Long cycleId) {
    Person person = getCurrentUserPerson();
    return taskRepository.findByCycleIdAndPersonId(cycleId, person.getId()).stream().map(this::toDTO)
        .collect(Collectors.toList());
  }

  // ========== Multi-filter methods ==========

  public Page<TaskDTO> getTasksWithFilters(Long cycleId, List<TaskStatus> statuses, List<TaskPriority> priorities,
      List<Long> assigneeIds, TaskCategory category, Boolean exclude, Pageable pageable) {

    // Convert empty lists to null for the query
    List<TaskStatus> statusList = (statuses != null && !statuses.isEmpty()) ? statuses : null;
    List<TaskPriority> priorityList = (priorities != null && !priorities.isEmpty()) ? priorities : null;
    List<Long> assigneeList = (assigneeIds != null && !assigneeIds.isEmpty()) ? assigneeIds : null;

    if (exclude != null && exclude) {
      return taskRepository
          .findByCycleIdWithExclusionFilters(cycleId, statusList, priorityList, assigneeList, pageable)
          .map(this::toDTO);
    } else {
      return taskRepository
          .findByCycleIdWithFilters(cycleId, statusList, priorityList, assigneeList, category, pageable)
          .map(this::toDTO);
    }
  }

  public Page<TaskDTO> getTasksByProjectIdWithFilters(Long projectId, List<TaskStatus> statuses, 
      List<TaskPriority> priorities, List<Long> assigneeIds, TaskCategory category, Boolean exclude, 
      Pageable pageable) {

    // Convert empty lists to null for the query
    List<TaskStatus> statusList = (statuses != null && !statuses.isEmpty()) ? statuses : null;
    List<TaskPriority> priorityList = (priorities != null && !priorities.isEmpty()) ? priorities : null;
    List<Long> assigneeList = (assigneeIds != null && !assigneeIds.isEmpty()) ? assigneeIds : null;

    if (exclude != null && exclude) {
      return taskRepository
          .findByProjectIdWithExclusionFilters(projectId, statusList, priorityList, assigneeList, pageable)
          .map(this::toDTO);
    } else {
      return taskRepository
          .findByProjectIdWithFilters(projectId, statusList, priorityList, assigneeList, category, pageable)
          .map(this::toDTO);
    }
  }

  // ========== CSV Export ==========

  @Transactional(readOnly = true)
  public byte[] exportTasksCsv(Long projectId, Long cycleId, List<TaskStatus> statuses,
      List<TaskPriority> priorities, List<Long> assigneeIds, TaskCategory category) {

    List<TaskStatus> statusList = (statuses != null && !statuses.isEmpty()) ? statuses : null;
    List<TaskPriority> priorityList = (priorities != null && !priorities.isEmpty()) ? priorities : null;
    List<Long> assigneeList = (assigneeIds != null && !assigneeIds.isEmpty()) ? assigneeIds : null;

    List<Task> tasks;
    if (cycleId != null) {
      tasks = taskRepository
          .findByCycleIdWithFilters(cycleId, statusList, priorityList, assigneeList, category,
              Pageable.unpaged())
          .getContent()
          .stream()
          .filter(t -> t.getDeletedAt() == null)
          .collect(Collectors.toList());
    } else {
      tasks = taskRepository
          .findByProjectIdWithFilters(projectId, statusList, priorityList, assigneeList, category,
              Pageable.unpaged())
          .getContent();
    }

    try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter writer =
            new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8))) {

      writer.println("ID,Title,Status,Priority,Assignee,Pitch,Cycle,Estimate(h),Actual(h),Tags,Created,Updated");

      DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
      for (Task t : tasks) {
        String tags = t.getTags() != null ? t.getTags() : "";
        writer.printf("%d,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
            t.getId(),
            csvEscape(t.getTitle()),
            t.getStatus() != null ? t.getStatus().name() : "",
            t.getPriority() != null ? t.getPriority().name() : "",
            t.getAssignee() != null ? csvEscape(t.getAssignee().getName()) : "",
            t.getPitch() != null ? csvEscape(t.getPitch().getTitle()) : "",
            t.getCycle() != null ? csvEscape(t.getCycle().getName()) : "",
            t.getEstimateHours() != null ? t.getEstimateHours() : "",
            t.getActualHours() != null ? t.getActualHours() : "",
            csvEscape(tags),
            t.getCreatedAt() != null ? t.getCreatedAt().format(fmt) : "",
            t.getUpdatedAt() != null ? t.getUpdatedAt().format(fmt) : "");
      }
      writer.flush();
      return baos.toByteArray();
    } catch (Exception e) {
      throw new RuntimeException("Failed to export tasks as CSV", e);
    }
  }

  private static String csvEscape(String val) {
    if (val == null || val.isEmpty()) return "";
    // Prefix formula-injection trigger characters so spreadsheet apps don't execute them
    String safe = val;
    if (!safe.isEmpty() && "=+-@\t\r".indexOf(safe.charAt(0)) >= 0) {
      safe = "'" + safe;
    }
    if (safe.contains(",") || safe.contains("\"") || safe.contains("\n") || safe.contains("\r")) {
      return "\"" + safe.replace("\"", "\"\"") + "\"";
    }
    return safe;
  }

  // ========== Category-based methods ==========

  public Page<TaskDTO> getTasksByCycleIdAndCategory(Long cycleId, TaskCategory category, Pageable pageable) {
    return taskRepository.findByCycleIdAndCategory(cycleId, category, pageable).map(this::toDTO);
  }

  public List<TaskDTO> getTasksByCycleIdAndCategory(Long cycleId, TaskCategory category) {
    return taskRepository.findByCycleIdAndCategory(cycleId, category).stream().map(this::toDTO)
        .collect(Collectors.toList());
  }

  public int countTasksByCycleIdAndCategory(Long cycleId, TaskCategory category) {
    return taskRepository.countByCycleIdAndCategory(cycleId, category);
  }

  // ========== Sub-task hierarchy methods ==========

  public List<TaskDTO> getSubTasks(Long parentTaskId) {
    return taskRepository.findByParentTaskId(parentTaskId).stream().map(this::toDTO).collect(Collectors.toList());
  }

  public List<TaskDTO> getRootTasksByCycleId(Long cycleId) {
    return taskRepository.findRootTasksByCycleId(cycleId).stream().map(this::toDTO).collect(Collectors.toList());
  }

  public Page<TaskDTO> getRootTasksByCycleId(Long cycleId, Pageable pageable) {
    return taskRepository.findByCycleIdAndParentTaskIdIsNull(cycleId, pageable).map(this::toDTO);
  }

  public List<TaskDTO> getTaskTreeByCycleId(Long cycleId) {
    List<Task> rootTasks = taskRepository.findRootTasksByCycleId(cycleId);
    return rootTasks.stream().map(this::toDTOWithChildren).collect(Collectors.toList());
  }

  /** Check if setting parentTask would create a circular reference */
  private boolean isCircularReference(Task task, Task proposedParent) {
    if (task.getId().equals(proposedParent.getId())) {
      return true; // Can't be its own parent
    }

    Task current = proposedParent;
    while (current != null) {
      if (current.getId().equals(task.getId())) {
        return true; // Found a cycle
      }
      current = current.getParentTask();
    }

    return false;
  }

  private TaskDTO toDTOWithChildren(Task task) {
    TaskDTO dto = toDTO(task);
    if (task.getChildren() != null && !task.getChildren().isEmpty()) {
      List<TaskDTO> childrenDTOs = task.getChildren().stream().map(this::toDTOWithChildren)
          .collect(Collectors.toList());
      dto.setChildren(childrenDTOs);
    }
    return dto;
  }

  private TaskDTO toDTO(Task task) {
    // Get dependency information with null safety
    List<TaskDependencyDTO> blocking = task.getOutgoingDependencies() != null
        ? task.getOutgoingDependencies().stream().map(dep -> TaskDependencyDTO.builder().id(dep.getId())
            .sourceTaskId(dep.getSourceTask().getId()).sourceTaskTitle(dep.getSourceTask().getTitle())
            .targetTaskId(dep.getTargetTask().getId()).targetTaskTitle(dep.getTargetTask().getTitle())
            .dependencyType(dep.getDependencyType()).createdAt(dep.getCreatedAt()).build())
            .collect(Collectors.toList())
        : new ArrayList<>();

    List<TaskDependencyDTO> blockedBy = task.getIncomingDependencies() != null
        ? task.getIncomingDependencies().stream().map(dep -> TaskDependencyDTO.builder().id(dep.getId())
            .sourceTaskId(dep.getSourceTask().getId()).sourceTaskTitle(dep.getSourceTask().getTitle())
            .targetTaskId(dep.getTargetTask().getId()).targetTaskTitle(dep.getTargetTask().getTitle())
            .dependencyType(dep.getDependencyType()).createdAt(dep.getCreatedAt()).build())
            .collect(Collectors.toList())
        : new ArrayList<>();

    // Map children (subtasks) - only include basic info to avoid deep recursion
    List<TaskDTO> children = task.getChildren() != null
        ? task.getChildren().stream().map(child -> TaskDTO.builder().id(child.getId()).title(child.getTitle())
            .description(child.getDescription()).status(child.getStatus()).priority(child.getPriority())
            .category(child.getCategory()).estimateHours(child.getEstimateHours())
            .actualHours(child.getActualHours()).cycleId(child.getCycle().getId())
            .assigneeId(child.getAssignee() != null ? child.getAssignee().getId() : null)
            .assigneeName(child.getAssignee() != null ? child.getAssignee().getName() : null)
            .parentTaskId(child.getParentTask().getId()).parentTaskTitle(child.getParentTask().getTitle())
            .dueDate(child.getDueDate()).createdAt(child.getCreatedAt()).updatedAt(child.getUpdatedAt())
            .build()).collect(Collectors.toList())
        : new ArrayList<>();

    return TaskDTO.builder().id(task.getId()).title(task.getTitle()).description(task.getDescription())
        .status(task.getStatus()).priority(task.getPriority()).category(task.getCategory())
        .estimateHours(task.getEstimateHours()).actualHours(task.getActualHours())
        .cycleId(task.getCycle().getId()).cycleName(task.getCycle().getName())
        .projectId(task.getCycle().getProject() != null ? task.getCycle().getProject().getId() : null)
        .projectName(task.getCycle().getProject() != null ? task.getCycle().getProject().getName() : null)
        .projectKey(task.getCycle().getProject() != null ? task.getCycle().getProject().getProjectKey() : null)
        .pitchId(task.getPitch() != null ? task.getPitch().getId() : null)
        .pitchTitle(task.getPitch() != null ? task.getPitch().getTitle() : null)
        .scopeId(task.getScope() != null ? task.getScope().getId() : null)
        .scopeName(task.getScope() != null ? task.getScope().getScope() : null)
        .autoCreatedScopeId(getAutoCreatedScopeId(task))
        .showOnHillChart(task.isRootScope() && getAutoCreatedScopeId(task) != null)
        .teamId(task.getTeam() != null ? task.getTeam().getId() : null)
        .teamName(task.getTeam() != null ? task.getTeam().getName() : null)
        .assigneeId(task.getAssignee() != null ? task.getAssignee().getId() : null)
        .assigneeName(task.getAssignee() != null ? task.getAssignee().getName() : null)
        .assigneeAvatarUrl(task.getAssignee() != null ? task.getAssignee().getAvatarUrl() : null)
        .pairAssigneeId(task.getPairAssignee() != null ? task.getPairAssignee().getId() : null)
        .pairAssigneeName(task.getPairAssignee() != null ? task.getPairAssignee().getName() : null)
        .pairAssigneeAvatarUrl(task.getPairAssignee() != null ? task.getPairAssignee().getAvatarUrl() : null)
        .createdById(task.getCreatedBy() != null ? task.getCreatedBy().getId() : null)
        .createdByName(task.getCreatedBy() != null ? task.getCreatedBy().getName() : null)
        .parentTaskId(task.getParentTask() != null ? task.getParentTask().getId() : null)
        .parentTaskTitle(task.getParentTask() != null ? task.getParentTask().getTitle() : null)
        .dueDate(task.getDueDate()).completedAt(task.getCompletedAt()).createdAt(task.getCreatedAt())
        .updatedAt(task.getUpdatedAt()).tags(task.getTags()).children(children).blockingTasks(blocking)
        .blockedByTasks(blockedBy).blockedByCount(blockedBy.size()).isBlocked(!blockedBy.isEmpty())
        .build();
  }

  /**
   * Get the auto-created scope ID for a task.
   * Uses repository query to avoid lazy loading issues.
   */
  private Long getAutoCreatedScopeId(Task task) {
    if (task.getPitch() == null || task.getParentTask() != null) {
      return null; // Not a root task with pitch
    }
    return hillChartPointRepository.findByLinkedTaskId(task.getId())
        .map(HillChartPoint::getId)
        .orElse(null);
  }
}
