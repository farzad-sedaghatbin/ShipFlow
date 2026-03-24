package com.github.farzadsedaghatbin.shipflow.dto.mcp;

import com.github.farzadsedaghatbin.shipflow.dto.ProjectDTO;
import lombok.Builder;
import lombok.Data;

/** Compact project representation for MCP tool responses. */
@Data
@Builder
public class McpProjectDTO {

  private Long id;
  private String name;
  private String projectKey;
  private String description;
  private String projectType;
  private Boolean isActive;
  private Integer activeCycleCount;

  public static McpProjectDTO from(ProjectDTO dto) {
    return McpProjectDTO.builder()
        .id(dto.getId())
        .name(dto.getName())
        .projectKey(dto.getProjectKey())
        .description(dto.getDescription())
        .projectType(dto.getProjectType() != null ? dto.getProjectType().name() : null)
        .isActive(dto.getIsActive())
        .activeCycleCount(dto.getActiveCycleCount())
        .build();
  }
}
