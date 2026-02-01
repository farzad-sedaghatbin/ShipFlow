package com.github.farzadsedaghatbin.shipflow.dto;

import java.time.LocalDateTime;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HillChartPointDTO {
  private Long id;
  private Long pitchId;
  private String pitchTitle;
  private Long cycleId;
  private String cycleName;
  private Long projectId;
  private String projectName;
  private String projectKey;
  private String scope;
  private String description;
  private Integer position;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
