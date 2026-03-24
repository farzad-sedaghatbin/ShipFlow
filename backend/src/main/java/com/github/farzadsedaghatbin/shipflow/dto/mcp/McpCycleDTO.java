package com.github.farzadsedaghatbin.shipflow.dto.mcp;

import com.github.farzadsedaghatbin.shipflow.dto.CycleDTO;
import lombok.Builder;
import lombok.Data;

/** Compact cycle representation for MCP tool responses. */
@Data
@Builder
public class McpCycleDTO {

  private Long id;
  private Long projectId;
  private String projectName;
  private String name;
  private String startDate;
  private String endDate;
  private String phase;
  private Boolean isActive;
  private Integer pitchCount;

  public static McpCycleDTO from(CycleDTO dto) {
    return McpCycleDTO.builder()
        .id(dto.getId())
        .projectId(dto.getProjectId())
        .projectName(dto.getProjectName())
        .name(dto.getName())
        .startDate(dto.getStartDate() != null ? dto.getStartDate().toString() : null)
        .endDate(dto.getEndDate() != null ? dto.getEndDate().toString() : null)
        .phase(dto.getPhase() != null ? dto.getPhase().name() : null)
        .isActive(dto.getIsActive())
        .pitchCount(dto.getPitchCount())
        .build();
  }
}
