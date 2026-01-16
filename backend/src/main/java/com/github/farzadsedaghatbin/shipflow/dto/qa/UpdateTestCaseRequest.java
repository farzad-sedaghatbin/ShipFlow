package com.github.farzadsedaghatbin.shipflow.dto.qa;

import com.github.farzadsedaghatbin.shipflow.entity.enums.TestCasePriority;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TestCaseStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TestCaseType;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

/**
 * Request DTO for updating a test case.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTestCaseRequest {
    
    @Size(max = 255, message = "Title must be less than 255 characters")
    private String title;
    
    private String description;
    
    private String preconditions;
    
    private String steps;
    
    private String expectedResult;
    
    private Long pitchId;
    
    private Long cycleId;
    
    private Long teamId;
    
    private Long scopeId;
    
    private Long taskId;
    
    private TestCaseType type;
    
    private TestCasePriority priority;
    
    private TestCaseStatus status;
    
    private List<String> tags;
    
    private Integer estimatedMinutes;
}
