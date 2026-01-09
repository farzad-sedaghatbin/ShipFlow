package com.github.farzadsedaghatbin.shipflow.dto.qa;

import com.github.farzadsedaghatbin.shipflow.entity.enums.TestRunStatus;
import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO for test run responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestRunDTO {
    private Long id;
    private Long testCaseId;
    private String testCaseKey;
    private String testCaseTitle;
    private Long cycleId;
    private String cycleName;
    private Long pitchId;
    private String pitchTitle;
    private TestRunStatus status;
    private Long executedById;
    private String executedByName;
    private LocalDateTime executedAt;
    private Integer durationSeconds;
    private String notes;
    private String actualResult;
    private String buildVersion;
    private String environment;
    private String attachments;
    private Long bugReportId;
    private String bugReportKey;
    private LocalDateTime createdAt;
}
