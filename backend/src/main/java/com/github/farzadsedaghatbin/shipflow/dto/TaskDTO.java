package com.github.farzadsedaghatbin.shipflow.dto;

import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskCategory;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskPriority;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

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
}
