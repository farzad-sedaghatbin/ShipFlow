package com.github.farzadsedaghatbin.shipflow.dto.mcp;

import com.github.farzadsedaghatbin.shipflow.dto.qa.BugReportDTO;
import lombok.Builder;
import lombok.Data;

/** Compact bug-report representation for MCP tool responses. */
@Data
@Builder
public class McpBugReportDTO {

  private Long id;
  private String bugKey;
  private String title;
  private String description;
  private String stepsToReproduce;
  private String expectedBehavior;
  private String actualBehavior;
  private String environment;
  private String status;
  private String severity;
  private Long taskId;
  private String taskTitle;
  private Long pitchId;
  private String pitchTitle;
  private Long cycleId;
  private String cycleName;
  private String reporterName;
  private String assigneeName;
  private String resolution;

  public static McpBugReportDTO from(BugReportDTO dto) {
    return McpBugReportDTO.builder()
        .id(dto.getId())
        .bugKey(dto.getBugKey())
        .title(dto.getTitle())
        .description(dto.getDescription())
        .stepsToReproduce(dto.getStepsToReproduce())
        .expectedBehavior(dto.getExpectedBehavior())
        .actualBehavior(dto.getActualBehavior())
        .environment(dto.getEnvironment())
        .status(dto.getStatus() != null ? dto.getStatus().name() : null)
        .severity(dto.getSeverity() != null ? dto.getSeverity().name() : null)
        .taskId(dto.getTaskId())
        .taskTitle(dto.getTaskTitle())
        .pitchId(dto.getPitchId())
        .pitchTitle(dto.getPitchTitle())
        .cycleId(dto.getCycleId())
        .cycleName(dto.getCycleName())
        .reporterName(dto.getReporterName())
        .assigneeName(dto.getAssigneeName())
        .resolution(dto.getResolution())
        .build();
  }
}
