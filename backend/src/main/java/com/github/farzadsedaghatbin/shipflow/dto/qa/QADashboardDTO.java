package com.github.farzadsedaghatbin.shipflow.dto.qa;

import lombok.*;

/**
 * DTO for QA dashboard statistics for a cycle.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QADashboardDTO {
    private Long cycleId;
    private String cycleName;
    
    // Test case statistics
    private Long totalTestCases;
    private Long draftTestCases;
    private Long readyTestCases;
    private Long approvedTestCases;
    private Long deprecatedTestCases;
    private Long aiGeneratedTestCases;
    
    // Test run statistics
    private Long totalRuns;
    private Long passedRuns;
    private Long failedRuns;
    private Long blockedRuns;
    private Long skippedRuns;
    private Long pendingRuns;
    
    // Bug statistics
    private Long totalBugs;
    private Long openBugs;
    private Long inProgressBugs;
    private Long resolvedBugs;
    private Long verifiedBugs;
    private Long closedBugs;
    
    // Bug severity breakdown
    private Long trivialBugs;
    private Long minorBugs;
    private Long majorBugs;
    private Long criticalBugs;
    private Long blockerBugs;
    
    // Coverage metrics
    private Double overallPassRate;
    private Double overallCoverage;
    
    // Pitch coverage breakdown
    private java.util.List<TestCoverageDTO> pitchCoverage;
}
