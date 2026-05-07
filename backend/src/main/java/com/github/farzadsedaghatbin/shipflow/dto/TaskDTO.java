package com.github.farzadsedaghatbin.shipflow.dto;

import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskCategory;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskPriority;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskDTO {
  private Long id;
  private String title;
  private String description;
  private TaskStatus status;
  private TaskPriority priority;
  private TaskCategory category;
  private BigDecimal estimateHours;
  private BigDecimal actualHours;

  private Long cycleId;
  private String cycleName;
  private Long projectId;
  private String projectName;
  private String projectKey;

  // Pitch relationship
  private Long pitchId;
  private String pitchTitle;

  // Scope relationship
  private Long scopeId;
  private String scopeName;

  // Auto-created scope for root tasks linked to pitch (Scope-Task Bridge)
  private Long autoCreatedScopeId;
  private Boolean showOnHillChart;

  private Long teamId;
  private String teamName;

  private Long assigneeId;
  private String assigneeName;
  private String assigneeAvatarUrl;

  private Long pairAssigneeId;
  private String pairAssigneeName;
  private String pairAssigneeAvatarUrl;

  private Long createdById;
  private String createdByName;

  private Long parentTaskId;
  private String parentTaskTitle;
  private java.util.List<TaskDTO> children;

  private LocalDate dueDate;
  private LocalDateTime completedAt;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  private String tags;

  // Target release
  private Long targetReleaseId;
  private String targetReleaseName;
  private String targetReleaseVersion;

  // Dependency information
  private java.util.List<TaskDependencyDTO> blockingTasks; // Tasks this task blocks
  private java.util.List<TaskDependencyDTO> blockedByTasks; // Tasks blocking this task
  private Integer blockedByCount; // Count of blocking tasks
  private Boolean isBlocked; // Quick check if task is blocked

  // Comment count
  private Integer commentCount;

  // Attachments
  private java.util.List<TaskAttachmentDTO> attachments;
}
