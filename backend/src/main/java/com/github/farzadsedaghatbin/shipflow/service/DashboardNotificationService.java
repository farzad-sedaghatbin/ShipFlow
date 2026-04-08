package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.dashboard.DashboardNotificationDTO;
import com.github.farzadsedaghatbin.shipflow.dto.health.StagnationAlertDTO;
import com.github.farzadsedaghatbin.shipflow.entity.*;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CyclePhase;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskPriority;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskStatus;
import com.github.farzadsedaghatbin.shipflow.repository.*;
import com.github.farzadsedaghatbin.shipflow.service.IEmailNotificationService;
import com.github.farzadsedaghatbin.shipflow.service.notification.NotificationProvider;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing dashboard notifications. Generates notifications for
 * overdue tasks, blocked scopes, cycle deadlines, etc.
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
  private final List<NotificationProvider> notificationProviders;
  private final NotificationSseManager notificationSseManager;
  private final IEmailNotificationService emailService;

  @Autowired(required = false)
  private PitchHealthService pitchHealthService;

  /** Get all notifications for a user */
  @Transactional(readOnly = true)
  public List<DashboardNotificationDTO> getUserNotifications(Long userId) {
    List<DashboardNotification> notifications = notificationRepository.findActiveNotifications(userId,
        LocalDateTime.now());

    return notifications.stream().map(this::toDTO).collect(Collectors.toList());
  }

  /** Get unread notifications for a user */
  @Transactional(readOnly = true)
  public List<DashboardNotificationDTO> getUnreadNotifications(Long userId) {
    List<DashboardNotification> notifications = notificationRepository
        .findByUserIdAndIsReadOrderByCreatedAtDesc(userId, false);

    return notifications.stream().map(this::toDTO).collect(Collectors.toList());
  }

  /** Get unread notification count for a user */
  @Transactional(readOnly = true)
  public Long getUnreadCount(Long userId) {
    return notificationRepository.countByUserIdAndIsRead(userId, false);
  }

  /** Mark a notification as read */
  public DashboardNotificationDTO markAsRead(Long notificationId) {
    DashboardNotification notification = notificationRepository.findById(notificationId)
        .orElseThrow(() -> new RuntimeException("Notification not found with id: " + notificationId));

    notification.markAsRead();
    DashboardNotification saved = notificationRepository.save(notification);
    return toDTO(saved);
  }

  /** Mark all notifications as read for a user */
  public void markAllAsRead(Long userId) {
    List<DashboardNotification> notifications = notificationRepository
        .findByUserIdAndIsReadOrderByCreatedAtDesc(userId, false);

    notifications.forEach(DashboardNotification::markAsRead);
    notificationRepository.saveAll(notifications);
    log.info("Marked {} notifications as read for user {}", notifications.size(), userId);
  }

  /** Delete a notification */
  public void deleteNotification(Long notificationId) {
    notificationRepository.deleteById(notificationId);
  }

  /** Create notification when a task is assigned to someone */
  public void notifyTaskAssignment(Task task, User assignee) {
    if (assignee == null) {
      return;
    }

    createNotification(assignee, "TASK_ASSIGNED", "New Task Assigned: " + task.getTitle(),
        String.format("You have been assigned to task '%s'", task.getTitle()), "INFO", "/tasks/" + task.getId(),
        "TASK", task.getId());

    // Send external notifications (Slack, Teams, etc.)
    sendToConfiguredChannels("TASK_ASSIGNED",
        String.format("📋 *Task Assigned*\n*%s* has been assigned to *%s*\nTask: %s", task.getTitle(),
            assignee.getUsername(),
            task.getDescription() != null ? task.getDescription() : "No description"),
        "TASK", task.getId(), assignee.getPerson());

    // Send email notification
    if (assignee.getEmail() != null && !assignee.getEmail().isBlank()) {
      String recipientName = assignee.getPerson() != null && assignee.getPerson().getName() != null
          ? assignee.getPerson().getName()
          : assignee.getUsername();
      emailService.sendTaskAssigned(
          assignee.getEmail(), recipientName, task.getTitle(), "/tasks/" + task.getId());
    }

    log.info("Created task assignment notification for user {} on task {}", assignee.getId(), task.getId());
  }

  /** Create notification when a user is mentioned in a comment */
  public void notifyCommentMention(User mentionedUser, User author, String entityType, Long entityId,
      String commentPreview) {
    if (mentionedUser == null || author == null) {
      return;
    }

    // Don't notify if user mentioned themselves
    if (mentionedUser.getId().equals(author.getId())) {
      return;
    }

    String authorName = author.getPerson() != null ? author.getPerson().getName() : author.getUsername();

    String actionUrl = entityType.equals("TASK") ? "/tasks/" + entityId : "/bugs/" + entityId;

    createNotification(mentionedUser, "COMMENT_MENTION", "You were mentioned in a comment",
        String.format("%s mentioned you: \"%s\"", authorName,
            commentPreview.length() > 100 ? commentPreview.substring(0, 100) + "..." : commentPreview),
        "INFO", actionUrl, entityType, entityId);

    // Send external notifications (Slack, Teams, etc.)
    sendToConfiguredChannels("COMMENT_MENTION",
        String.format("💬 *Comment Mention*\n*%s* mentioned *%s* in a %s comment:\n\"%s\"", authorName,
            mentionedUser.getUsername(), entityType.toLowerCase(),
            commentPreview.length() > 100 ? commentPreview.substring(0, 100) + "..." : commentPreview),
        entityType, entityId, mentionedUser.getPerson());

    // Send email notification
    if (mentionedUser.getEmail() != null && !mentionedUser.getEmail().isBlank()) {
      String recipientName = mentionedUser.getPerson() != null && mentionedUser.getPerson().getName() != null
          ? mentionedUser.getPerson().getName()
          : mentionedUser.getUsername();
      emailService.sendMentionedInComment(
          mentionedUser.getEmail(), recipientName, entityType.toLowerCase(), actionUrl);
    }

    log.info("Created comment mention notification for user {} from author {} on {} {}", mentionedUser.getId(),
        author.getId(), entityType, entityId);
  }

  /** Create notification when a task is reassigned from one person to another */
  public void notifyTaskReassignment(Task task, User oldAssignee, User newAssignee) {
    // Notify the old assignee that the task was removed from them
    if (oldAssignee != null) {
      createNotification(oldAssignee, "TASK_REASSIGNED", "Task Reassigned: " + task.getTitle(),
          String.format("Task '%s' has been reassigned to someone else", task.getTitle()), "INFO",
          "/tasks/" + task.getId(), "TASK", task.getId());
    }

    // Notify the new assignee
    if (newAssignee != null) {
      createNotification(newAssignee, "TASK_ASSIGNED", "New Task Assigned: " + task.getTitle(),
          String.format("You have been assigned to task '%s'", task.getTitle()), "INFO",
          "/tasks/" + task.getId(), "TASK", task.getId());
    }

    log.info("Created task reassignment notifications for task {}", task.getId());
  }

  /** Create notification when a task status changes to blocked */
  public void notifyTaskStatusChange(Task task, TaskStatus oldStatus, TaskStatus newStatus) {
    if (task.getAssignee() == null || task.getAssignee().getUser() == null) {
      return;
    }

    User assignee = task.getAssignee().getUser();

    // Only notify for important status changes
    if (newStatus == TaskStatus.BLOCKED && oldStatus != TaskStatus.BLOCKED) {
      createNotification(assignee, "TASK_STATUS_CHANGED", "Task Blocked: " + task.getTitle(),
          String.format("Task '%s' status changed to BLOCKED", task.getTitle()), "WARNING",
          "/tasks/" + task.getId(), "TASK", task.getId());

      // Send external notifications (Slack, Teams, etc.)
      sendToConfiguredChannels("TASK_BLOCKED",
          String.format("⚠️ *Task Blocked*\n*%s* is now blocked\nAssigned to: %s", task.getTitle(),
              assignee.getUsername()),
          "TASK", task.getId(), assignee.getPerson());
    } else if (newStatus == TaskStatus.IN_PROGRESS && oldStatus == TaskStatus.BLOCKED) {
      createNotification(assignee, "TASK_STATUS_CHANGED", "Task Unblocked: " + task.getTitle(),
          String.format("Task '%s' is no longer blocked", task.getTitle()), "INFO", "/tasks/" + task.getId(),
          "TASK", task.getId());
    } else if (newStatus == TaskStatus.DONE) {
      // Send external notifications (Slack, Teams, etc.)
      sendToConfiguredChannels("TASK_COMPLETED",
          String.format("✅ *Task Completed*\n*%s* has been marked as done\nCompleted by: %s", task.getTitle(),
              assignee.getUsername()),
          "TASK", task.getId(), assignee.getPerson());
    }

    log.info("Created task status change notification for task {}", task.getId());
  }

  /** Create notification when a task priority changes to high */
  public void notifyTaskPriorityChange(Task task, TaskPriority newPriority) {
    if (task.getAssignee() == null || task.getAssignee().getUser() == null) {
      return;
    }

    if (newPriority == TaskPriority.HIGH || newPriority == TaskPriority.URGENT) {
      User assignee = task.getAssignee().getUser();

      createNotification(assignee, "TASK_PRIORITY_CHANGED", "High Priority Task: " + task.getTitle(),
          String.format("Task '%s' priority has been set to %s", task.getTitle(), newPriority), "WARNING",
          "/tasks/" + task.getId(), "TASK", task.getId());

      log.info("Created task priority change notification for task {}", task.getId());
    }
  }

  /**
   * Create notifications when a cycle phase changes Notifies all users associated
   * with the cycle (project members, team members, pitch assignees)
   */
  public void notifyCyclePhaseChange(Cycle cycle, CyclePhase oldPhase, CyclePhase newPhase) {
    if (oldPhase == newPhase) {
      return;
    }

    String phaseDescription = getPhaseDescription(newPhase);
    String message = String.format("Cycle '%s' has transitioned from %s to %s phase. %s", cycle.getName(),
        oldPhase.name(), newPhase.name(), phaseDescription);

    String severity = "INFO";

    // Notify all users involved in the cycle
    List<User> usersToNotify = getUsersInvolvedInCycle(cycle);

    for (User user : usersToNotify) {
      createNotification(user, "CYCLE_PHASE_CHANGED",
          String.format("Cycle Phase Changed: %s → %s", oldPhase.name(), newPhase.name()), message, severity,
          "/cycles/" + cycle.getId(), "CYCLE", cycle.getId());
    }

    // Send to configured external channels (Slack, Teams, or both)
    String externalMessage = String.format(
        "🔄 *Cycle Phase Changed*\n" + "*Cycle:* %s\n" + "*Phase Transition:* %s → %s\n" + "*Description:* %s",
        cycle.getName(), oldPhase.name(), newPhase.name(), phaseDescription);

    sendToConfiguredChannels("CYCLE_PHASE_CHANGED", externalMessage, "CYCLE", cycle.getId());

    log.info("Created cycle phase change notifications for cycle {} ({}→{}) - {} users notified", cycle.getId(),
        oldPhase, newPhase, usersToNotify.size());
  }

  /** Get all users involved in a cycle (team members through assignments) */
  private List<User> getUsersInvolvedInCycle(Cycle cycle) {
    List<User> users = new ArrayList<>();

    // Add users from team assignments (teams derived from pitches in this cycle)
    if (cycle.getPitches() != null) {
      cycle.getPitches().stream()
          .map(p -> p.getTeam())
          .filter(team -> team != null)
          .distinct()
          .forEach(team -> {
            if (team.getAssignments() != null) {
              team.getAssignments().forEach(assignment -> {
                if (assignment.getPerson() != null && assignment.getPerson().getUser() != null) {
                  users.add(assignment.getPerson().getUser());
                }
              });
            }
          });
    }

    // Add project owner if the cycle belongs to a project
    if (cycle.getProject() != null && cycle.getProject().getOwner() != null) {
      users.add(cycle.getProject().getOwner());
    }

    // Remove duplicates
    return users.stream().distinct().collect(Collectors.toList());
  }

  /** Get a description of what each phase means */
  private String getPhaseDescription(CyclePhase phase) {
    return switch (phase) {
      case SHAPING_BUILDING -> "Active phase - shaping ideas and building assigned pitches.";
      case BETTING_COOLDOWN -> "Reflection phase - evaluating pitches and planning the next cycle.";
    };
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
    generateStagnationNotifications();
    cleanupOldNotifications();

    log.info("Completed daily notification generation");
  }

  /** Generate notifications for stagnating pitches detected by PitchHealthService */
  private void generateStagnationNotifications() {
    if (pitchHealthService == null) {
      log.debug("PitchHealthService not available, skipping stagnation notifications");
      return;
    }

    List<StagnationAlertDTO> alerts = pitchHealthService.detectAllStagnatingPitches();
    
    for (StagnationAlertDTO alert : alerts) {
      // Only notify for WARNING and CRITICAL alerts
      if (alert.getSeverity() == StagnationAlertDTO.AlertSeverity.INFO) {
        continue;
      }

      Pitch pitch = pitchRepository.findById(alert.getPitchId()).orElse(null);
      if (pitch == null || pitch.getTeam() == null) {
        continue;
      }

      List<User> teamMembers = getTeamMembers(pitch.getTeam());
      String severity = alert.getSeverity() == StagnationAlertDTO.AlertSeverity.CRITICAL ? "ERROR" : "WARNING";
      String icon = alert.getSeverity() == StagnationAlertDTO.AlertSeverity.CRITICAL ? "🚨" : "⚠️";

      for (User user : teamMembers) {
        // Check if notification already sent in last 24 hours
        List<DashboardNotification> existing = notificationRepository
            .findByUserIdAndTypeOrderByCreatedAtDesc(user.getId(), "PITCH_STAGNATION");

        boolean alreadyNotified = existing.stream()
            .anyMatch(n -> n.getEntityId().equals(alert.getPitchId())
                && n.getCreatedAt().isAfter(LocalDateTime.now().minusDays(1)));

        if (!alreadyNotified) {
          String title = String.format("%s Pitch Stagnating: %s", icon, alert.getPitchTitle());
          String message = String.format("%s. %s", alert.getDescription(), 
              alert.getRecommendations().isEmpty() ? "" : "Recommendation: " + alert.getRecommendations().get(0));

          createNotification(user, "PITCH_STAGNATION", title, message, severity,
              "/pitches/" + alert.getPitchId(), "PITCH", alert.getPitchId());
        }
      }

      // Send external notifications for critical alerts
      if (alert.getSeverity() == StagnationAlertDTO.AlertSeverity.CRITICAL) {
        String externalMessage = String.format("🚨 *Pitch Stagnating*\n*%s* (%s)\n%s\n%s",
            alert.getPitchTitle(),
            alert.getCycleName(),
            alert.getDescription(),
            alert.getRecommendations().isEmpty() ? "" : "💡 " + String.join("\n💡 ", alert.getRecommendations()));
        
        sendToConfiguredChannels("PITCH_STAGNATION", externalMessage, "PITCH", alert.getPitchId());
      }
    }

    log.info("Generated stagnation notifications for {} pitches", alerts.size());
  }

  /** Generate notifications for overdue tasks */
  private void generateOverdueTaskNotifications() {
    List<Task> overdueTasks = taskRepository.findAll().stream()
        .filter(task -> task.getDueDate() != null && task.getDueDate().isBefore(LocalDate.now())
            && !task.getStatus().equals(TaskStatus.DONE) && !task.getStatus().equals(TaskStatus.CANCELLED))
        .collect(Collectors.toList());

    for (Task task : overdueTasks) {
      // Check if notification already exists
      List<DashboardNotification> existing = notificationRepository.findByUserIdAndTypeOrderByCreatedAtDesc(
          task.getAssignee() != null ? task.getAssignee().getId() : null, "OVERDUE_TASK");

      boolean alreadyNotified = existing.stream().anyMatch(n -> n.getEntityId().equals(task.getId())
          && n.getCreatedAt().isAfter(LocalDateTime.now().minusDays(1)));

      if (!alreadyNotified && task.getAssignee() != null && task.getAssignee().getUser() != null) {
        createNotification(task.getAssignee().getUser(), "OVERDUE_TASK", "Task Overdue: " + task.getTitle(),
            String.format("Task '%s' is overdue by %d days", task.getTitle(),
                LocalDate.now().toEpochDay() - task.getDueDate().toEpochDay()),
            "WARNING", "/tasks/" + task.getId(), "TASK", task.getId());
      }
    }

    log.info("Generated {} overdue task notifications", overdueTasks.size());
  }

  /** Generate notifications for blocked tasks */
  private void generateBlockedTaskNotifications() {
    List<Task> blockedTasks = taskRepository.findAll().stream()
        .filter(task -> task.getStatus().equals(TaskStatus.BLOCKED)).collect(Collectors.toList());

    for (Task task : blockedTasks) {
      if (task.getAssignee() != null && task.getAssignee().getUser() != null) {
        List<DashboardNotification> existing = notificationRepository
            .findByUserIdAndTypeOrderByCreatedAtDesc(task.getAssignee().getUser().getId(), "BLOCKED_TASK");

        boolean alreadyNotified = existing.stream().anyMatch(n -> n.getEntityId().equals(task.getId())
            && n.getCreatedAt().isAfter(LocalDateTime.now().minusDays(1)));

        if (!alreadyNotified) {
          createNotification(task.getAssignee().getUser(), "BLOCKED_TASK", "Task Blocked: " + task.getTitle(),
              String.format("Task '%s' is blocked and needs attention", task.getTitle()), "ERROR",
              "/tasks/" + task.getId(), "TASK", task.getId());
        }
      }
    }

    log.info("Generated {} blocked task notifications", blockedTasks.size());
  }

  /** Generate notifications for upcoming cycle deadlines */
  private void generateCycleDeadlineNotifications() {
    LocalDate warningDate = LocalDate.now().plusDays(7); // 7 days warning

    List<Cycle> upcomingDeadlines = cycleRepository.findAll().stream()
        .filter(cycle -> cycle.getEndDate() != null && cycle.getEndDate().isBefore(warningDate)
            && cycle.getEndDate().isAfter(LocalDate.now()) && cycle.getPhase() == CyclePhase.SHAPING_BUILDING)
        .collect(Collectors.toList());

    for (Cycle cycle : upcomingDeadlines) {
      // Notify all team members
      List<User> users = userRepository.findAll(); // In real scenario, find team members

      for (User user : users) {
        List<DashboardNotification> existing = notificationRepository
            .findByUserIdAndTypeOrderByCreatedAtDesc(user.getId(), "CYCLE_DEADLINE");

        boolean alreadyNotified = existing.stream().anyMatch(n -> n.getEntityId().equals(cycle.getId())
            && n.getCreatedAt().isAfter(LocalDateTime.now().minusDays(7)));

        if (!alreadyNotified) {
          long daysRemaining = cycle.getEndDate().toEpochDay() - LocalDate.now().toEpochDay();
          createNotification(user, "CYCLE_DEADLINE", "Cycle Ending Soon: " + cycle.getName(),
              String.format("Cycle '%s' ends in %d days", cycle.getName(), daysRemaining), "WARNING",
              "/cycles/" + cycle.getId(), "CYCLE", cycle.getId());
        }
      }
    }

    log.info("Generated cycle deadline notifications for {} cycles", upcomingDeadlines.size());
  }

  /** Generate notifications for stalled hill chart points */
  private void generateStalledHillChartNotifications() {
    LocalDateTime stalledThreshold = LocalDateTime.now().minusDays(14); // Not moved in 14 days

    List<HillChartPoint> stalledPoints = hillChartPointRepository
        .findAll().stream().filter(point -> point.getUpdatedAt() != null
            && point.getUpdatedAt().isBefore(stalledThreshold) && point.getPosition() < 100)
        .collect(Collectors.toList());

    for (HillChartPoint point : stalledPoints) {
      // Notify pitch owner and team
      if (point.getPitch() != null && point.getPitch().getTeam() != null) {
        List<User> teamMembers = userRepository.findAll(); // Find actual team members

        for (User user : teamMembers) {
          List<DashboardNotification> existing = notificationRepository
              .findByUserIdAndTypeOrderByCreatedAtDesc(user.getId(), "HILL_CHART_STALLED");

          boolean alreadyNotified = existing.stream().anyMatch(n -> n.getEntityId().equals(point.getId())
              && n.getCreatedAt().isAfter(LocalDateTime.now().minusDays(7)));

          if (!alreadyNotified) {
            createNotification(user, "HILL_CHART_STALLED", "Scope Not Moving: " + point.getScope(),
                String.format("Hill chart scope '%s' hasn't been updated in %d days", point.getScope(),
                    14),
                "WARNING", "/pitches/" + point.getPitch().getId() + "/hill-chart", "HILL_CHART",
                point.getId());
          }
        }
      }
    }

    log.info("Generated stalled hill chart notifications for {} points", stalledPoints.size());
  }

  /** Create a new notification and push it instantly to the user's SSE stream if connected. */
  private DashboardNotification createNotification(User user, String type, String title, String message,
      String severity, String actionUrl, String entityType, Long entityId) {

    DashboardNotification notification = DashboardNotification.builder().user(user).type(type).title(title)
        .message(message).severity(severity).actionUrl(actionUrl).entityType(entityType).entityId(entityId)
        .isRead(false).expiresAt(LocalDateTime.now().plusDays(30)) // Auto-expire after 30 days
        .build();

    DashboardNotification saved = notificationRepository.save(notification);

    // Push to the user's live SSE stream (no-op if the user has no active stream)
    notificationSseManager.sendToUser(user.getId(), toDTO(saved));

    return saved;
  }

  /** Cleanup old notifications */
  @Scheduled(cron = "0 0 2 * * *") // Run daily at 2 AM
  public void cleanupOldNotifications() {
    // Delete read notifications older than 30 days
    LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
    notificationRepository.deleteOldReadNotifications(cutoffDate);

    // Delete expired notifications
    List<DashboardNotification> expired = notificationRepository.findExpiredNotifications(LocalDateTime.now());
    notificationRepository.deleteAll(expired);

    log.info("Cleaned up {} expired notifications", expired.size());
  }

  /** Create notification when circuit breaker is triggered on a pitch */
  public void notifyCircuitBreakerTriggered(Pitch pitch) {
    List<User> teamMembers = Collections.emptyList();

    if (pitch.getTeam() != null) {
      teamMembers = getTeamMembers(pitch.getTeam());

      for (User user : teamMembers) {
        createNotification(user, "CIRCUIT_BREAKER_TRIGGERED", "⚠️ Circuit Breaker: " + pitch.getTitle(),
            String.format(
                "Pitch '%s' has exceeded its time budget (appetite: %d days). Shape Up safety valve triggered.",
                pitch.getTitle(), pitch.getAppetiteDays()),
            "CRITICAL", "/pitches/" + pitch.getId(), "PITCH", pitch.getId());
      }
    } else {
      log.warn("Pitch {} has no team assigned - sending external notifications only", pitch.getId());
    }

    // Send to configured external channels (Slack, Teams, or both)
    String externalMessage = String.format("⚠️ Circuit Breaker triggered for pitch '%s' - exceeded time budget",
        pitch.getTitle());
    sendToConfiguredChannels("CIRCUIT_BREAKER_TRIGGERED", externalMessage, "PITCH", pitch.getId());

    log.info("Created circuit breaker triggered notifications for pitch {} ({} team members, external channels sent)",
        pitch.getId(), teamMembers.size());
  }

  /** Create notification when a pitch is killed due to overflow */
  public void notifyPitchKilled(Pitch pitch, String reason) {
    List<User> teamMembers = Collections.emptyList();

    if (pitch.getTeam() != null) {
      teamMembers = getTeamMembers(pitch.getTeam());

      for (User user : teamMembers) {
        createNotification(user, "PITCH_KILLED", "🛑 Pitch Killed: " + pitch.getTitle(),
            String.format("Pitch '%s' has been permanently stopped. Reason: %s", pitch.getTitle(), reason),
            "CRITICAL", "/pitches/" + pitch.getId(), "PITCH", pitch.getId());
      }
    } else {
      log.warn("Pitch {} has no team assigned - sending external notifications only", pitch.getId());
    }

    // Send to configured external channels (Slack, Teams, or both)
    String externalMessage = String.format("🛑 Pitch '%s' has been permanently cancelled. Reason: %s",
        pitch.getTitle(), reason);
    sendToConfiguredChannels("PITCH_KILLED", externalMessage, "PITCH", pitch.getId());

    log.info("Created pitch killed notifications for pitch {} ({} team members, external channels sent)", pitch.getId(),
        teamMembers.size());
  }

  /** Helper method to safely get team members with null checks */
  private List<User> getTeamMembers(Team team) {
    if (team.getAssignments() == null) {
      return Collections.emptyList();
    }

    return team.getAssignments().stream()
        .filter(assignment -> assignment.getPerson() != null && assignment.getPerson().getUser() != null)
        .map(assignment -> assignment.getPerson().getUser()).toList();
  }

  /**
   * Send notification to all configured external channels (Slack, Teams, etc.).
   * Iterates over all registered {@link NotificationProvider} beans.
   */
  private void sendToConfiguredChannels(String notificationType, String message,
      String entityType, Long entityId) {
    sendToConfiguredChannels(notificationType, message, entityType, entityId, null);
  }

  /**
   * Send notification to all configured external channels with optional @mention.
   * If {@code mentionTarget} is non-null and the provider has a mapping for that
   * person, the provider-specific @mention is prepended to the message.
   */
  private void sendToConfiguredChannels(String notificationType, String message,
      String entityType, Long entityId, Person mentionTarget) {
    List<String> sentProviders = new ArrayList<>();

    for (NotificationProvider provider : notificationProviders) {
      try {
        if (provider.isActive()) {
          String finalMessage = message;
          if (mentionTarget != null) {
            String mention = provider.resolveUserMention(mentionTarget);
            if (mention != null) {
              finalMessage = mention + " " + message;
            }
          }
          provider.sendNotification(notificationType, finalMessage, null, entityType, entityId);
          sentProviders.add(provider.getProviderName());
          log.debug("Sent {} notification to {}", notificationType, provider.getProviderName());
        }
      } catch (Exception e) {
        log.warn("Failed to send {} notification to {}: {}", notificationType, provider.getProviderName(),
            e.getMessage());
      }
    }

    if (sentProviders.isEmpty()) {
      log.info("No external notification channels configured for {} - skipping", notificationType);
    } else {
      log.info("Sent {} notification to: {}", notificationType, String.join(", ", sentProviders));
    }
  }

  private DashboardNotificationDTO toDTO(DashboardNotification notification) {
    return DashboardNotificationDTO.builder().id(notification.getId()).userId(notification.getUser().getId())
        .type(notification.getType()).title(notification.getTitle()).message(notification.getMessage())
        .severity(notification.getSeverity()).actionUrl(notification.getActionUrl())
        .entityType(notification.getEntityType()).entityId(notification.getEntityId())
        .isRead(notification.getIsRead()).readAt(notification.getReadAt())
        .expiresAt(notification.getExpiresAt()).createdAt(notification.getCreatedAt()).build();
  }
}
