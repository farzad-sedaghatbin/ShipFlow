package com.github.farzadsedaghatbin.shipflow.dto;

import com.github.farzadsedaghatbin.shipflow.entity.enums.PitchStatus;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PitchDTO {
    // Basic fields
    private Long id;
    private String title;
    private String description;
    private Integer appetiteDays;

    // Cycle information
    private Long cycleId;
    private String cycleName;

    // Project information
    private Long projectId;
    private String projectName;
    private String projectKey;

    // Team information
    private Long teamId;
    private String teamName;

    // Status and timestamps
    private PitchStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Progress metrics
    private Double totalHoursSpent;
    private Double appetiteHours;
    private Double progressPercentage;
    
    // Circuit Breaker - Shape Up safety valve
    private Boolean isCircuitBreakerTriggered;
    private String circuitBreakerReason;
    private LocalDateTime circuitBreakerDate;
    
    // Shape Up Methodology Fields
    private String problemStatement;
    private String solution;
    private String rabbitHoles;
    private String risks;
    private String noGos;
    private String wireframeLinks;
}
