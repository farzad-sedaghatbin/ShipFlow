package com.github.farzadsedaghatbin.shipflow.dto.mcp;

import com.github.farzadsedaghatbin.shipflow.dto.PitchDTO;
import lombok.Builder;
import lombok.Data;

/**
 * Compact pitch representation for MCP tool responses.
 *
 * <p>Includes Shape Up methodology fields (problem statement, solution, rabbit holes, risks,
 * no-gos) and wireframe links. Wireframe links often contain Figma URLs that AI assistants can
 * follow up with Figma MCP to retrieve the actual design context.
 */
@Data
@Builder
public class McpPitchDTO {

  private Long id;
  private String title;
  private String status;
  private Long cycleId;
  private String cycleName;
  private Long projectId;
  private String projectName;
  private String teamName;
  private Integer appetiteDays;

  // Shape Up methodology fields
  private String problemStatement;
  private String solution;
  private String rabbitHoles;
  private String risks;
  private String noGos;

  /**
   * Wireframe links — typically Figma URLs.
   *
   * <p>When present, AI assistants can pass these URLs to Figma MCP ({@code get_design_context})
   * to retrieve component structure, colors, and layout for implementation guidance.
   */
  private String wireframeLinks;

  // Epic / roadmap relationship
  private Long epicId;
  private String epicName;

  public static McpPitchDTO from(PitchDTO dto) {
    return McpPitchDTO.builder()
        .id(dto.getId())
        .title(dto.getTitle())
        .status(dto.getStatus() != null ? dto.getStatus().name() : null)
        .cycleId(dto.getCycleId())
        .cycleName(dto.getCycleName())
        .projectId(dto.getProjectId())
        .projectName(dto.getProjectName())
        .teamName(dto.getTeamName())
        .appetiteDays(dto.getAppetiteDays())
        .problemStatement(dto.getProblemStatement())
        .solution(dto.getSolution())
        .rabbitHoles(dto.getRabbitHoles())
        .risks(dto.getRisks())
        .noGos(dto.getNoGos())
        .wireframeLinks(dto.getWireframeLinks())
        .epicId(dto.getEpicId())
        .epicName(dto.getEpicName())
        .build();
  }
}
