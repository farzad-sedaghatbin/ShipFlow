package com.github.farzadsedaghatbin.shipflow.dto;

import com.github.farzadsedaghatbin.shipflow.entity.enums.CyclePhase;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ProjectType;
import java.time.LocalDate;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CycleDTO {
  private Long id;
  private Long projectId;
  private String projectName;
  private String projectKey;
  private String name;
  private LocalDate startDate;
  private LocalDate endDate;
  private CyclePhase phase;
  private Boolean isActive;
  private Integer pitchCount;
  private Integer teamCount;
  private ProjectType projectType;
  private String sprintGoal;
  private Integer velocityActual;
}
