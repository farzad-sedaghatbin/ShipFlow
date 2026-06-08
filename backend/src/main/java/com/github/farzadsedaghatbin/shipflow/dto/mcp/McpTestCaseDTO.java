package com.github.farzadsedaghatbin.shipflow.dto.mcp;

import com.github.farzadsedaghatbin.shipflow.dto.qa.TestCaseDTO;
import lombok.Builder;
import lombok.Data;

/**
 * Compact test-case representation for MCP tool responses.
 *
 * <p>Carries the fields a coding agent actually needs as acceptance criteria — preconditions,
 * steps, expected result — plus the task/pitch links that let an agent answer "what does done look
 * like for this task?". The full {@link TestCaseDTO} shape is preserved on the REST API; this DTO
 * is the trimmed view tailored to agent context.
 */
@Data
@Builder
public class McpTestCaseDTO {

  private Long id;
  private String testCaseKey;
  private String title;
  private String description;
  private String preconditions;
  private String steps;
  private String expectedResult;
  private String status;
  private String priority;
  private String type;
  private Boolean aiGenerated;
  private Long taskId;
  private String taskTitle;
  private Long pitchId;
  private String pitchTitle;
  private Long cycleId;
  private String cycleName;
  private Integer estimatedMinutes;

  public static McpTestCaseDTO from(TestCaseDTO dto) {
    return McpTestCaseDTO.builder()
        .id(dto.getId())
        .testCaseKey(dto.getTestCaseKey())
        .title(dto.getTitle())
        .description(dto.getDescription())
        .preconditions(dto.getPreconditions())
        .steps(dto.getSteps())
        .expectedResult(dto.getExpectedResult())
        .status(dto.getStatus() != null ? dto.getStatus().name() : null)
        .priority(dto.getPriority() != null ? dto.getPriority().name() : null)
        .type(dto.getType() != null ? dto.getType().name() : null)
        .aiGenerated(dto.getAiGenerated())
        .taskId(dto.getTaskId())
        .taskTitle(dto.getTaskTitle())
        .pitchId(dto.getPitchId())
        .pitchTitle(dto.getPitchTitle())
        .cycleId(dto.getCycleId())
        .cycleName(dto.getCycleName())
        .estimatedMinutes(dto.getEstimatedMinutes())
        .build();
  }
}
