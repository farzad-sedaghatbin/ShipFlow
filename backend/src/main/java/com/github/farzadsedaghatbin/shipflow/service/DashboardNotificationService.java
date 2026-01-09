package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.dashboard.DashboardNotificationDTO;
import com.github.farzadsedaghatbin.shipflow.entity.*;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CyclePhase;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskPriority;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskStatus;
import com.github.farzadsedaghatbin.shipflow.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing dashboard notifications.
 * Generates notifications for overdue tasks, blocked scopes, cycle deadlines, etc.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DashboardNotificationService {

    private final DashboardNotificationRepository notificationRepository;
    private final TaskRepository taskRepository;
    private final CycleRepository cycleRepository;
    private final PitchRepository pitchRepository;
    private final HillChartPointRepository hillChartPointRepository;
    private final UserRepository userRepository;

    /**
     * Get all notifications for a user
     */
    @Transactional(readOnly = true)
    public List<DashboardNotificationDTO> getUserNotifications(Long userId) {
        List<DashboardNotification> notifications = notificationRepository
                .findActiveNotifications(userId, LocalDateTime.now());
        
        return notifications.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get unread notifications for a user
     */
    @Transactional(readOnly = true)
    public List<DashboardNotificationDTO> getUnreadNotifications(Long userId) {
        List<DashboardNotification> notifications = notificationRepository
                .findByUserIdAndIsReadOrderByCreatedAtDesc(userId, false);
        
        return notifications.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get unread notification count for a user
     */
    @Transactional(readOnly = true)
    public Long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsRead(userId, false);
    }

    /**
     * Mark a notification as read
     */
    public DashboardNotificationDTO markAsRead(Long notificationId) {
        DashboardNotification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found with id: " + notificationId));
        
        notification.markAsRead();
        DashboardNotification saved = notificationRepository.save(notification);
        return toDTO(saved);
    }

    /**
     * Mark all notifications as read for a user
     */
    public void markAllAsRead(Long userId) {
        List<DashboardNotification> notifications = notificationRepository
                .findByUserIdAndIsReadOrderByCreatedAtDesc(userId, false);
        
        notifications.forEach(DashboardNotification::markAsRead);
        notificationRepository.saveAll(notifications);
        log.info("Marked {} notifications as read for user {}", notifications.size(), userId);
    }

    /**
     * Delete a notification
     */
    public void deleteNotification(Long notificationId) {
        notificationRepository.deleteById(notificationId);
    }

    /**
     * Create notification when a task is assigned to someone
     */
    public void notifyTaskAssignment(Task task, User assignee) {
        if (assignee == null) {
            return;
        }

        createNotification(
            assignee,
            "TASK_ASSIGNED",
            "New Task Assigned: " + task.getTitle(),
            String.format("You have been assigned to task '%s'", task.getTitle()),
            "INFO",
            "/tasks/" + task.getId(),
            "TASK",
            task.getId()
        );
        
        log.info("Created task assignment notification for user {} on task {}", 
                assignee.getId(), task.getId());
    }

    /**
     * Create notification when a task is reassigned from one person to another
     */
    public void notifyTaskReassignment(Task task, User oldAssignee, User newAssignee) {
        // Notify the old assignee that the task was removed from them
        if (oldAssignee != null) {
            createNotification(
                oldAssignee,
                "TASK_REASSIGNED",
                "Task Reassigned: " + task.getTitle(),
                String.format("Task '%s' has been reassigned to someone else", task.getTitle()),
                "INFO",
                "/tasks/" + task.getId(),
                "TASK",
                task.getId()
            );
        }

        // Notify the new assignee
        if (newAssignee != null) {
            createNotification(
                newAssignee,
                "TASK_ASSIGNED",
                "New Task Assigned: " + task.getTitle(),
                String.format("You have been assigned to task '%s'", task.getTitle()),
                "INFO",
                "/tasks/" + task.getId(),
                "TASK",
                task.getId()
            );
        }
        
        log.info("Created task reassignment notifications for task {}", task.getId());
    }

    /**
     * Create notification when a task status changes to blocked
     */
    public void notifyTaskStatusChange(Task task, TaskStatus oldStatus, TaskStatus newStatus) {
        if (task.getAssignee() == null || task.getAssignee().getUser() == null) {
            return;
        }

        User assignee = task.getAssignee().getUser();

        // Only notify for important status changes
        if (newStatus == TaskStatus.BLOCKED && oldStatus != TaskStatus.BLOCKED) {
            createNotification(
                assignee,
                "TASK_STATUS_CHANGED",
                "Task Blocked: " + task.getTitle(),
                String.format("Task '%s' status changed to BLOCKED", task.getTitle()),
                "WARNING",
                "/tasks/" + task.getId(),
                "TASK",
                task.getId()
            );
        } else if (newStatus == TaskStatus.IN_PROGRESS && oldStatus == TaskStatus.BLOCKED) {
            createNotification(
                assignee,
                "TASK_STATUS_CHANGED",
                "Task Unblocked: " + task.getTitle(),
                String.format("Task '%s' is no longer blocked", task.getTitle()),
                "INFO",
                "/tasks/" + task.getId(),
                "TASK",
                task.getId()
            );
        }
        
        log.info("Created task status change notification for task {}", task.getId());
    }

    /**
     * Create notification when a task priority changes to high
     */
    public void notifyTaskPriorityChange(Task task, TaskPriority newPriority) {
        if (task.getAssignee() == null || task.getAssignee().getUser() == null) {
            return;
        }

        if (newPriority == TaskPriority.HIGH || newPriority == TaskPriority.URGENT) {
            User assignee = task.getAssignee().getUser();
            
            createNotification(
                assignee,
                "TASK_PRIORITY_CHANGED",
                "High Priority Task: " + task.getTitle(),
                String.format("Task '%s' priority has been set to %s", task.getTitle(), newPriority),
                "WARNING",
                "/tasks/" + task.getId(),
                "TASK",
                task.getId()
            );
            
            log.info("Created task priority change notification for task {}", task.getId());
        }
    }

    /**
     * Generate notifications for overdue tasks, blocked scopes, and cycle deadlines
     * This is called periodically via scheduled task
     */
    @Scheduled(cron = "0 0 8 * * *") // Run daily at 8 AM
    public void generateDailyNotifications() {
        log.info("Starting daily notification generation");
        
        generateOverdueTaskNotifications();
        generateBlockedTaskNotifications();
        generateCycleDeadlineNotifications();
        generateStalledHillChartNotifications();
        cleanupOldNotifications();
        
        log.info("Completed daily notification generation");
    }

    /**
     * Generate notifications for overdue tasks
     */
    private void generateOverdueTaskNotifications() {
        List<Task> overdueTasks = taskRepository.findAll().stream()
                .filter(task -> task.getDueDate() != null 
                        && task.getDueDate().isBefore(LocalDate.now())
                        && !task.getStatus().equals(TaskStatus.DONE)
                        && !task.getStatus().equals(TaskStatus.CANCELLED))
                .collect(Collectors.toList());

        for (Task task : overdueTasks) {
            // Check if notification already exists
            List<DashboardNotification> existing = notificationRepository
                    .findByUserIdAndTypeOrderByCreatedAtDesc(
                            task.getAssignee() != null ? task.getAssignee().getId() : null, 
                            "OVERDUE_TASK");
            
            boolean alreadyNotified = existing.stream()
                    .anyMatch(n -> n.getEntityId().equals(task.getId()) 
                            && n.getCreatedAt().isAfter(LocalDateTime.now().minusDays(1)));
            
            if (!alreadyNotified && task.getAssignee() != null && task.getAssignee().getUser() != null) {
                createNotification(
                    task.getAssignee().getUser(),
                    "OVERDUE_TASK",
                    "Task Overdue: " + task.getTitle(),
                    String.format("Task '%s' is overdue by %d days", 
                            task.getTitle(), 
                            LocalDate.now().toEpochDay() - task.getDueDate().toEpochDay()),
                    "WARNING",
                    "/tasks/" + task.getId(),
                    "TASK",
                    task.getId()
                );
            }
        }
        
        log.info("Generated {} overdue task notifications", overdueTasks.size());
    }

    /**
     * Generate notifications for blocked tasks
     */
    private void generateBlockedTaskNotifications() {
        List<Task> blockedTasks = taskRepository.findAll().stream()
                .filter(task -> task.getStatus().equals(TaskStatus.BLOCKED))
                .collect(Collectors.toList());

        for (Task task : blockedTasks) {
            if (task.getAssignee() != null && task.getAssignee().getUser() != null) {
                List<DashboardNotification> existing = notificationRepository
                        .findByUserIdAndTypeOrderByCreatedAtDesc(
                                task.getAssignee().getUser().getId(), 
                                "BLOCKED_TASK");
                
                boolean alreadyNotified = existing.stream()
                        .anyMatch(n -> n.getEntityId().equals(task.getId()) 
                                && n.getCreatedAt().isAfter(LocalDateTime.now().minusDays(1)));
                
                if (!alreadyNotified) {
                    createNotification(
                        task.getAssignee().getUser(),
                        "BLOCKED_TASK",
                        "Task Blocked: " + task.getTitle(),
                        String.format("Task '%s' is blocked and needs attention", task.getTitle()),
                        "ERROR",
                        "/tasks/" + task.getId(),
                        "TASK",
                        task.getId()
                    );
                }
            }
        }
        
        log.info("Generated {} blocked task notifications", blockedTasks.size());
    }

    /**
     * Generate notifications for upcoming cycle deadlines
     */
    private void generateCycleDeadlineNotifications() {
        LocalDate warningDate = LocalDate.now().plusDays(7); // 7 days warning
        
        List<Cycle> upcomingDeadlines = cycleRepository.findAll().stream()
                .filter(cycle -> cycle.getEndDate() != null 
                        && cycle.getEndDate().isBefore(warningDate)
                        && cycle.getEndDate().isAfter(LocalDate.now())
                        && cycle.getPhase() == CyclePhase.BUILD)
                .collect(Collectors.toList());

        for (Cycle cycle : upcomingDeadlines) {
            // Notify all team members
            List<User> users = userRepository.findAll(); // In real scenario, find team members
            
            for (User user : users) {
                List<DashboardNotification> existing = notificationRepository
                        .findByUserIdAndTypeOrderByCreatedAtDesc(user.getId(), "CYCLE_DEADLINE");
                
                boolean alreadyNotified = existing.stream()
                        .anyMatch(n -> n.getEntityId().equals(cycle.getId()) 
                                && n.getCreatedAt().isAfter(LocalDateTime.now().minusDays(7)));
                
                if (!alreadyNotified) {
                    long daysRemaining = cycle.getEndDate().toEpochDay() - LocalDate.now().toEpochDay();
                    createNotification(
                        user,
                        "CYCLE_DEADLINE",
                        "Cycle Ending Soon: " + cycle.getName(),
                        String.format("Cycle '%s' ends in %d days", cycle.getName(), daysRemaining),
                        "WARNING",
                        "/cycles/" + cycle.getId(),
                        "CYCLE",
                        cycle.getId()
                    );
                }
            }
        }
        
        log.info("Generated cycle deadline notifications for {} cycles", upcomingDeadlines.size());
    }

    /**
     * Generate notifications for stalled hill chart points
     */
    private void generateStalledHillChartNotifications() {
        LocalDateTime stalledThreshold = LocalDateTime.now().minusDays(14); // Not moved in 14 days
        
        List<HillChartPoint> stalledPoints = hillChartPointRepository.findAll().stream()
                .filter(point -> point.getUpdatedAt() != null 
                        && point.getUpdatedAt().isBefore(stalledThreshold)
                        && point.getPosition() < 100)
                .collect(Collectors.toList());

        for (HillChartPoint point : stalledPoints) {
            // Notify pitch owner and team
            if (point.getPitch() != null && point.getPitch().getTeam() != null) {
                List<User> teamMembers = userRepository.findAll(); // Find actual team members
                
                for (User user : teamMembers) {
                    List<DashboardNotification> existing = notificationRepository
                            .findByUserIdAndTypeOrderByCreatedAtDesc(user.getId(), "HILL_CHART_STALLED");
                    
                    boolean alreadyNotified = existing.stream()
                            .anyMatch(n -> n.getEntityId().equals(point.getId()) 
                                    && n.getCreatedAt().isAfter(LocalDateTime.now().minusDays(7)));
                    
                    if (!alreadyNotified) {
                        createNotification(
                            user,
                            "HILL_CHART_STALLED",
                            "Scope Not Moving: " + point.getScope(),
                            String.format("Hill chart scope '%s' hasn't been updated in %d days", 
                                    point.getScope(), 
                                    14),
                            "WARNING",
                            "/pitches/" + point.getPitch().getId() + "/hill-chart",
                            "HILL_CHART",
                            point.getId()
                        );
                    }
                }
            }
        }
        
        log.info("Generated stalled hill chart notifications for {} points", stalledPoints.size());
    }

    /**
     * Create a new notification
     */
    private DashboardNotification createNotification(
            User user,
            String type,
            String title,
            String message,
            String severity,
            String actionUrl,
            String entityType,
            Long entityId) {
        
        DashboardNotification notification = DashboardNotification.builder()
                .user(user)
                .type(type)
                .title(title)
                .message(message)
                .severity(severity)
                .actionUrl(actionUrl)
                .entityType(entityType)
                .entityId(entityId)
                .isRead(false)
                .expiresAt(LocalDateTime.now().plusDays(30)) // Auto-expire after 30 days
                .build();
        
        return notificationRepository.save(notification);
    }

    /**
     * Cleanup old notifications
     */
    @Scheduled(cron = "0 0 2 * * *") // Run daily at 2 AM
    public void cleanupOldNotifications() {
        // Delete read notifications older than 30 days
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        notificationRepository.deleteOldReadNotifications(cutoffDate);
        
        // Delete expired notifications
        List<DashboardNotification> expired = notificationRepository
                .findExpiredNotifications(LocalDateTime.now());
        notificationRepository.deleteAll(expired);
        
        log.info("Cleaned up {} expired notifications", expired.size());
    }

    private DashboardNotificationDTO toDTO(DashboardNotification notification) {
        return DashboardNotificationDTO.builder()
                .id(notification.getId())
                .userId(notification.getUser().getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .severity(notification.getSeverity())
                .actionUrl(notification.getActionUrl())
                .entityType(notification.getEntityType())
                .entityId(notification.getEntityId())
                .isRead(notification.getIsRead())
                .readAt(notification.getReadAt())
                .expiresAt(notification.getExpiresAt())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
