package com.github.farzadsedaghatbin.shipflow.dto.customdashboard;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddWidgetRequest {

  @NotBlank(message = "Widget type is required")
  private String widgetType;

  private Long widgetId; // Existing widget reference

  private Long metricId; // For CUSTOM_KPI widgets

  @NotNull(message = "Position X is required")
  @Min(value = 0, message = "Position X must be non-negative")
  @Max(value = 11, message = "Position X must not exceed 11 (12-column grid)")
  private Integer positionX;

  @NotNull(message = "Position Y is required")
  @Min(value = 0, message = "Position Y must be non-negative")
  private Integer positionY;

  @NotNull(message = "Width is required")
  @Min(value = 1, message = "Width must be at least 1")
  @Max(value = 12, message = "Width must not exceed 12 (12-column grid)")
  private Integer width;

  @NotNull(message = "Height is required")
  @Min(value = 1, message = "Height must be at least 1")
  private Integer height;

  private String dataFilters;

  private String chartConfig;

  private Integer refreshInterval;

  private String settings;

  private Object config; // Widget configuration object (will be serialized to JSON)
}
