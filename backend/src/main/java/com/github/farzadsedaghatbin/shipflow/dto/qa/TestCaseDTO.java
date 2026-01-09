package com.github.farzadsedaghatbin.shipflow.dto.qa;

import com.github.farzadsedaghatbin.shipflow.entity.enums.TestCasePriority;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TestCaseStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TestCaseType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for test case responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestCaseDTO {
    private Long id;
    private String testCaseKey;
    private String title;
    private String description;
    private String preconditions;
    private String steps;
    private String expectedResult;
    private Long pitchId;
    private String pitchTitle;
    private Long cycleId;
    private String cycleName;
    private Long teamId;
    private String teamName;
    private TestCaseType type;
    private TestCasePriority priority;
    private TestCaseStatus status;
    private Boolean aiGenerated;
    private String tags;
    private List<String> tagList;
    private Integer estimatedMinutes;
    private Long createdById;
    private String createdByName;
    private Long updatedById;
    private String updatedByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Execution statistics
    private Long totalRuns;
    private Long passedRuns;
    private Long failedRuns;
    private Double passRate;
}
