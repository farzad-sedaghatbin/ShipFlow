package com.github.farzadsedaghatbin.shipflow.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDashboardWidgetRequest {
  private Boolean isVisible;
  private Integer displayOrder;
  private String layoutConfig;
  private String settings;
}
