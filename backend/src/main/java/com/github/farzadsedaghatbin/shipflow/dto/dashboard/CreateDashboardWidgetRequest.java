package com.github.farzadsedaghatbin.shipflow.dto.dashboard;

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
public class CreateDashboardWidgetRequest {
  @NotBlank(message = "Widget type is required")
  private String widgetType;

  @NotNull(message = "Visibility is required")
  private Boolean isVisible;

  @NotNull(message = "Display order is required")
  private Integer displayOrder;

  private String layoutConfig;
  private String settings;
}
