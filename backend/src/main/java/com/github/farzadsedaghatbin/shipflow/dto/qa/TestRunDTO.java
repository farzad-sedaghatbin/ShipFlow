package com.github.farzadsedaghatbin.shipflow.dto.qa;

import com.github.farzadsedaghatbin.shipflow.entity.enums.TestRunStatus;
import java.time.LocalDateTime;
import java.util.List;
import lombok.*;

/** DTO for test run responses. */
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
  /** Primary linked defect (first of {@link #linkedBugs}) — kept for backward compatibility. */
  private Long bugReportId;
  private String bugReportKey;
  /** All bug reports (defects) linked to this run. A failed execution may have several. */
  private List<LinkedBugReportDTO> linkedBugs;
  private LocalDateTime createdAt;
}
