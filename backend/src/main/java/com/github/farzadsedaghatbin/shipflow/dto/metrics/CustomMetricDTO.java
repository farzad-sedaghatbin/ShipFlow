package com.github.farzadsedaghatbin.shipflow.dto.metrics;

import com.github.farzadsedaghatbin.shipflow.entity.CustomMetric;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomMetricDTO {
  private Long id;
  private String name;
  private String description;
  private String formula;
  private CustomMetric.DataSource dataSource;
  private CustomMetric.AggregationType aggregationType;
  private String filters;
  private CustomMetric.DisplayFormat displayFormat;
  private Boolean isActive;

  // Scope information
  private Long cycleId;
  private String cycleName;
  private Long pitchId;
  private String pitchName;
  private Long teamId;
  private String teamName;

  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
