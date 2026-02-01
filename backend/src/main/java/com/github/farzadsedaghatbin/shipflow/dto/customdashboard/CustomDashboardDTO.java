package com.github.farzadsedaghatbin.shipflow.dto.customdashboard;

import com.github.farzadsedaghatbin.shipflow.entity.CustomDashboard;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomDashboardDTO {
  private Long id;
  private String name;
  private String description;
  private Boolean isDefault;
  private String layoutConfig;
  private Boolean isTemplate;
  private Boolean userContextFilter;
  private CustomDashboard.TemplateCategory templateCategory;
  private Long widgetCount;

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
