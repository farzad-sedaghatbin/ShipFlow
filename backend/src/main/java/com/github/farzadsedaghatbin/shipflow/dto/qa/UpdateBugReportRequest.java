package com.github.farzadsedaghatbin.shipflow.dto.qa;

import com.github.farzadsedaghatbin.shipflow.entity.enums.BugSeverity;
import com.github.farzadsedaghatbin.shipflow.entity.enums.BugStatus;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

/**
 * Request DTO for updating a bug report.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBugReportRequest {
    
    @Size(max = 255, message = "Title must be less than 255 characters")
    private String title;
    
    private String description;
    
    private String stepsToReproduce;
    
    private String expectedBehavior;
    
    private String actualBehavior;
    
    private String environment;
    
    private Long pitchId;
    
    private Long cycleId;
    
    private Long teamId;
    
    private Long scopeId;
    
    private Long taskId;
    
    private BugSeverity severity;
    
    private BugStatus status;
    
    private List<String> tags;
    
    private String attachments;
    
    private Long assigneeId;
    
    private String resolution;
}
