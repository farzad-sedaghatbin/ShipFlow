package com.github.farzadsedaghatbin.shipflow.dto;

import com.github.farzadsedaghatbin.shipflow.entity.enums.PitchStatus;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PitchDTO {
    private Long id;
    private String title;
    private String description;
    private Integer appetiteDays;
    private Long cycleId;
    private String cycleName;
    private Long projectId;
    private String projectName;
    private String projectKey;
    private Long teamId;
    private String teamName;
    private PitchStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Double totalHoursSpent;
    private Double appetiteHours;
    private Double progressPercentage;
    
    // Shape Up Methodology Fields
    private String problemStatement;
    private String solution;
    private String rabbitHoles;
    private String risks;
    private String noGos;
    private String wireframeLinks;
}
