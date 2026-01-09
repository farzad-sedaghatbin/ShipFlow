package com.github.farzadsedaghatbin.shipflow.dto.qa;

import com.github.farzadsedaghatbin.shipflow.entity.enums.BugSeverity;
import com.github.farzadsedaghatbin.shipflow.entity.enums.BugStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for bug report responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BugReportDTO {
    private Long id;
    private String bugKey;
    private String title;
    private String description;
    private String stepsToReproduce;
    private String expectedBehavior;
    private String actualBehavior;
    private String environment;
    private Long pitchId;
    private String pitchTitle;
    private Long cycleId;
    private String cycleName;
    private Long teamId;
    private String teamName;
    private Long testRunId;
    private BugSeverity severity;
    private BugStatus status;
    private String tags;
    private List<String> tagList;
    private String attachments;
    private Long reporterId;
    private String reporterName;
    private Long assigneeId;
    private String assigneeName;
    private String resolution;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
