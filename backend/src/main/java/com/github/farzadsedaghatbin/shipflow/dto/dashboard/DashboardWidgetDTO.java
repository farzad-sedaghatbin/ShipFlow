package com.github.farzadsedaghatbin.shipflow.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardWidgetDTO {
    private Long id;
    private Long userId;
    private String widgetType;
    private Boolean isVisible;
    private Integer displayOrder;
    private String layoutConfig;
    private String settings;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
