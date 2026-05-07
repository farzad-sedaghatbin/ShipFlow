package com.github.farzadsedaghatbin.shipflow.dto.metrics;

import com.github.farzadsedaghatbin.shipflow.entity.CustomMetric;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCustomMetricRequest {

  @NotBlank(message = "Metric name is required")
  @Size(max = 100, message = "Metric name must not exceed 100 characters")
  private String name;

  @Size(max = 500, message = "Description must not exceed 500 characters")
  private String description;

  @NotBlank(message = "Formula is required")
  private String formula;

  @NotNull(message = "Data source is required")
  private CustomMetric.DataSource dataSource;

  private CustomMetric.AggregationType aggregationType;

  private String filters;

  private CustomMetric.DisplayFormat displayFormat;

  private Boolean isActive;

  // Scope fields - all optional
  private Long cycleId;

  private Long pitchId;

  private Long teamId;
}
