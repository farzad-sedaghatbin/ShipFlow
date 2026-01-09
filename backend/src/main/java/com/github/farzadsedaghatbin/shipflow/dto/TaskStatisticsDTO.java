package com.github.farzadsedaghatbin.shipflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskStatisticsDTO {
    private Long cycleId;
    private String cycleName;
    
    private int totalTasks;
    private int backlogTasks;
    private int todoTasks;
    private int inProgressTasks;
    private int blockedTasks;
    private int inReviewTasks;
    private int doneTasks;
    private int cancelledTasks;
    
    private double completionPercentage;
    private double totalEstimateHours;
    private double totalActualHours;
    private double avgTasksPerPerson;
}
