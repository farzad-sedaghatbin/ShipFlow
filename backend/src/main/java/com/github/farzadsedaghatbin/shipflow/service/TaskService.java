package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.CreateTaskRequest;
import com.github.farzadsedaghatbin.shipflow.dto.TaskDTO;
import com.github.farzadsedaghatbin.shipflow.dto.TaskStatisticsDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Person;
import com.github.farzadsedaghatbin.shipflow.entity.Task;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskCategory;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskPriority;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskStatus;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PersonRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TaskRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;
    private final CycleRepository cycleRepository;
    private final PersonRepository personRepository;
    private final UserRepository userRepository;
    private final DashboardNotificationService notificationService;

    public List<TaskDTO> getAllTasks() {
        return taskRepository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public TaskDTO getTaskById(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Task not found with id: " + id));
        return toDTO(task);
    }

    public Page<TaskDTO> getTasksByCycleId(Long cycleId, Pageable pageable) {
        return taskRepository.findByCycleId(cycleId, pageable)
                .map(this::toDTO);
    }

    public List<TaskDTO> getTasksByCycleId(Long cycleId) {
        return taskRepository.findByCycleIdOrderByPriority(cycleId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public Page<TaskDTO> getTasksByCycleIdAndStatus(Long cycleId, TaskStatus status, Pageable pageable) {
        return taskRepository.findByCycleIdAndStatus(cycleId, status, pageable)
                .map(this::toDTO);
    }

    public List<TaskDTO> getTasksByCycleIdAndStatus(Long cycleId, TaskStatus status) {
        return taskRepository.findByCycleIdAndStatus(cycleId, status)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<TaskDTO> getTasksByAssigneeId(Long assigneeId) {
        return taskRepository.findByAssigneeId(assigneeId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<TaskDTO> getTasksByPersonId(Long personId) {
        return taskRepository.findByPersonId(personId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<TaskDTO> getTasksByProjectId(Long projectId) {
        return taskRepository.findByProjectId(projectId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public TaskDTO createTask(CreateTaskRequest request) {
        Cycle cycle = cycleRepository.findById(request.getCycleId())
                .orElseThrow(() -> new IllegalArgumentException("Cycle not found with id: " + request.getCycleId()));

        Task task = Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .cycle(cycle)
                .status(request.getStatus() != null ? request.getStatus() : TaskStatus.BACKLOG)
                .priority(request.getPriority() != null ? request.getPriority() : TaskPriority.MEDIUM)
                .category(request.getCategory() != null ? request.getCategory() : TaskCategory.PITCH_SCOPE)
                .estimateHours(request.getEstimateHours())
                .actualHours(request.getActualHours())
                .dueDate(request.getDueDate())
                .tags(request.getTags())
                .build();

        Person assignee = null;
        if (request.getAssigneeId() != null) {
            assignee = personRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new IllegalArgumentException("Assignee not found with id: " + request.getAssigneeId()));
            task.setAssignee(assignee);
        }

        if (request.getPairAssigneeId() != null) {
            Person pairAssignee = personRepository.findById(request.getPairAssigneeId())
                    .orElseThrow(() -> new IllegalArgumentException("Pair assignee not found with id: " + request.getPairAssigneeId()));
            task.setPairAssignee(pairAssignee);
        }

        // Set created by current user's person
        try {
            Person currentPerson = getCurrentUserPerson();
            task.setCreatedBy(currentPerson);
        } catch (Exception e) {
            // If user is not linked to a person, allow task creation without createdBy
        }

        Task saved = taskRepository.save(task);
        
        // Send notification if task is assigned during creation
        if (assignee != null && assignee.getUser() != null) {
            try {
                notificationService.notifyTaskAssignment(saved, assignee.getUser());
            } catch (Exception e) {
                // Log error but don't fail task creation
            }
        }
        
        return toDTO(saved);
    }

    public TaskDTO updateTask(Long id, CreateTaskRequest request) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Task not found with id: " + id));

        // Track old values for notification purposes
        Person oldAssignee = task.getAssignee();
        TaskStatus oldStatus = task.getStatus();
        TaskPriority oldPriority = task.getPriority();

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
            newAssignee = personRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new IllegalArgumentException("Assignee not found with id: " + request.getAssigneeId()));
            task.setAssignee(newAssignee);
        } else {
            task.setAssignee(null);
        }

        if (request.getPairAssigneeId() != null) {
            Person pairAssignee = personRepository.findById(request.getPairAssigneeId())
                    .orElseThrow(() -> new IllegalArgumentException("Pair assignee not found with id: " + request.getPairAssigneeId()));
            task.setPairAssignee(pairAssignee);
        } else {
            task.setPairAssignee(null);
        }

        Task saved = taskRepository.save(task);

        // Send notifications after save
        try {
            // Check if assignee changed
            if (hasAssigneeChanged(oldAssignee, newAssignee)) {
                if (oldAssignee != null && newAssignee != null) {
                    // Reassignment
                    notificationService.notifyTaskReassignment(
                        saved,
                        oldAssignee.getUser(),
                        newAssignee.getUser()
                    );
                } else if (newAssignee != null) {
                    // New assignment
                    notificationService.notifyTaskAssignment(saved, newAssignee.getUser());
                }
            }

            // Check if status changed
            if (request.getStatus() != null && !request.getStatus().equals(oldStatus)) {
                notificationService.notifyTaskStatusChange(saved, oldStatus, request.getStatus());
            }

            // Check if priority changed to high
            if (request.getPriority() != null && !request.getPriority().equals(oldPriority)) {
                notificationService.notifyTaskPriorityChange(saved, request.getPriority());
            }
        } catch (Exception e) {
            // Log but don't fail the update if notifications fail
            // TODO: Add proper logging
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
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Task not found with id: " + id));

        TaskStatus oldStatus = task.getStatus();
        task.setStatus(status);

        if (status == TaskStatus.DONE && oldStatus != TaskStatus.DONE) {
            task.setCompletedAt(LocalDateTime.now());
        } else if (status != TaskStatus.DONE) {
            task.setCompletedAt(null);
        }

        Task saved = taskRepository.save(task);
        return toDTO(saved);
    }

    public void deleteTask(Long id) {
        if (!taskRepository.existsById(id)) {
            throw new IllegalArgumentException("Task not found with id: " + id);
        }
        taskRepository.deleteById(id);
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

        double completionPercentage = totalTasks > 0 
                ? (double) doneTasks / totalTasks * 100 
                : 0.0;
        
        double avgTasksPerPerson = distinctAssignees > 0 
                ? (double) totalTasks / distinctAssignees 
                : 0.0;

        return TaskStatisticsDTO.builder()
                .cycleId(cycleId)
                .cycleName(cycle.getName())
                .totalTasks(totalTasks)
                .backlogTasks(backlogTasks)
                .todoTasks(todoTasks)
                .inProgressTasks(inProgressTasks)
                .blockedTasks(blockedTasks)
                .inReviewTasks(inReviewTasks)
                .doneTasks(doneTasks)
                .cancelledTasks(cancelledTasks)
                .completionPercentage(Math.round(completionPercentage * 100.0) / 100.0)
                .totalEstimateHours(totalEstimateHours != null ? totalEstimateHours : 0.0)
                .totalActualHours(totalActualHours != null ? totalActualHours : 0.0)
                .avgTasksPerPerson(Math.round(avgTasksPerPerson * 100.0) / 100.0)
                .build();
    }

    // ========== Methods for current user's tasks ==========

    private Person getCurrentUserPerson() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsernameWithPerson(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        if (user.getPerson() == null) {
            throw new IllegalArgumentException("Your account is not linked to a person profile. Please contact an administrator.");
        }

        return user.getPerson();
    }

    public List<TaskDTO> getMyTasks() {
        Person person = getCurrentUserPerson();
        return taskRepository.findByPersonId(person.getId())
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public Page<TaskDTO> getMyTasksByCycle(Long cycleId, Pageable pageable) {
        Person person = getCurrentUserPerson();
        return taskRepository.findByCycleIdAndPersonId(cycleId, person.getId(), pageable)
                .map(this::toDTO);
    }

    public List<TaskDTO> getMyTasksByCycle(Long cycleId) {
        Person person = getCurrentUserPerson();
        return taskRepository.findByCycleIdAndPersonId(cycleId, person.getId())
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ========== Multi-filter methods ==========

    public Page<TaskDTO> getTasksWithFilters(
            Long cycleId,
            List<TaskStatus> statuses,
            List<TaskPriority> priorities,
            List<Long> assigneeIds,
            TaskCategory category,
            Boolean exclude,
            Pageable pageable) {
        
        // Convert empty lists to null for the query
        List<TaskStatus> statusList = (statuses != null && !statuses.isEmpty()) ? statuses : null;
        List<TaskPriority> priorityList = (priorities != null && !priorities.isEmpty()) ? priorities : null;
        List<Long> assigneeList = (assigneeIds != null && !assigneeIds.isEmpty()) ? assigneeIds : null;

        if (exclude != null && exclude) {
            return taskRepository.findByCycleIdWithExclusionFilters(cycleId, statusList, priorityList, assigneeList, pageable)
                    .map(this::toDTO);
        } else {
            return taskRepository.findByCycleIdWithFilters(cycleId, statusList, priorityList, assigneeList, category, pageable)
                    .map(this::toDTO);
        }
    }

    // ========== Category-based methods ==========

    public Page<TaskDTO> getTasksByCycleIdAndCategory(Long cycleId, TaskCategory category, Pageable pageable) {
        return taskRepository.findByCycleIdAndCategory(cycleId, category, pageable)
                .map(this::toDTO);
    }

    public List<TaskDTO> getTasksByCycleIdAndCategory(Long cycleId, TaskCategory category) {
        return taskRepository.findByCycleIdAndCategory(cycleId, category)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public int countTasksByCycleIdAndCategory(Long cycleId, TaskCategory category) {
        return taskRepository.countByCycleIdAndCategory(cycleId, category);
    }

    private TaskDTO toDTO(Task task) {
        return TaskDTO.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .priority(task.getPriority())
                .category(task.getCategory())
                .estimateHours(task.getEstimateHours())
                .actualHours(task.getActualHours())
                .cycleId(task.getCycle().getId())
                .cycleName(task.getCycle().getName())
                .projectId(task.getCycle().getProject() != null ? task.getCycle().getProject().getId() : null)
                .projectName(task.getCycle().getProject() != null ? task.getCycle().getProject().getName() : null)
                .projectKey(task.getCycle().getProject() != null ? task.getCycle().getProject().getProjectKey() : null)
                .assigneeId(task.getAssignee() != null ? task.getAssignee().getId() : null)
                .assigneeName(task.getAssignee() != null ? task.getAssignee().getName() : null)
                .assigneeAvatarUrl(task.getAssignee() != null ? task.getAssignee().getAvatarUrl() : null)
                .pairAssigneeId(task.getPairAssignee() != null ? task.getPairAssignee().getId() : null)
                .pairAssigneeName(task.getPairAssignee() != null ? task.getPairAssignee().getName() : null)
                .pairAssigneeAvatarUrl(task.getPairAssignee() != null ? task.getPairAssignee().getAvatarUrl() : null)
                .createdById(task.getCreatedBy() != null ? task.getCreatedBy().getId() : null)
                .createdByName(task.getCreatedBy() != null ? task.getCreatedBy().getName() : null)
                .dueDate(task.getDueDate())
                .completedAt(task.getCompletedAt())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .tags(task.getTags())
                .build();
    }
}
