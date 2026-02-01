package com.github.farzadsedaghatbin.shipflow.dto;

import java.util.List;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamDTO {
  private Long id;
  private String name;
  private Long cycleId;
  private String cycleName;
  private Long projectId;
  private String projectName;
  private String projectKey;
  private List<TeamAssignmentDTO> assignments;
}
