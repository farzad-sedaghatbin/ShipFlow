package com.github.farzadsedaghatbin.shipflow.dto.mcp;

import com.github.farzadsedaghatbin.shipflow.dto.TaskDTO;
import lombok.Builder;
import lombok.Data;

/** Compact task representation for MCP tool responses. */
@Data
@Builder
public class McpTaskDTO {

  private Long id;
  private String title;
  private String description;
  private String status;
  private String priority;
  private Long cycleId;
  private String cycleName;
  private Long projectId;
  private String pitchTitle;
  private String assigneeName;
  private Boolean isBlocked;
  private Integer blockedByCount;
  private String dueDate;

  public static McpTaskDTO from(TaskDTO dto) {
    return McpTaskDTO.builder()
        .id(dto.getId())
        .title(dto.getTitle())
        .description(dto.getDescription())
        .status(dto.getStatus() != null ? dto.getStatus().name() : null)
        .priority(dto.getPriority() != null ? dto.getPriority().name() : null)
        .cycleId(dto.getCycleId())
        .cycleName(dto.getCycleName())
        .projectId(dto.getProjectId())
        .pitchTitle(dto.getPitchTitle())
        .assigneeName(dto.getAssigneeName())
        .isBlocked(dto.getIsBlocked())
        .blockedByCount(dto.getBlockedByCount())
        .dueDate(dto.getDueDate() != null ? dto.getDueDate().toString() : null)
        .build();
  }
}
