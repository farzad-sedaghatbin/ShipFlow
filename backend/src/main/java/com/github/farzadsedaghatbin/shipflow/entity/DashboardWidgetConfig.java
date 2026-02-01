package com.github.farzadsedaghatbin.shipflow.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/**
 * Entity linking custom dashboards to widgets with position and configuration. Enables flexible
 * dashboard layouts with drag-and-drop positioning.
 */
@Entity
@Table(name = "dashboard_widget_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"dashboard", "widget", "metric", "hibernateLazyInitializer", "handler"})
public class DashboardWidgetConfig {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "dashboard_id", nullable = false)
  private CustomDashboard dashboard;

  /** Reference to existing widget (optional - for standard widgets) */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "widget_id")
  private DashboardWidget widget;

  /** Widget type identifier */
  @Column(name = "widget_type", nullable = false, length = 50)
  private String widgetType;

  /** Reference to custom metric (for CUSTOM_KPI widgets) */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "metric_id")
  private CustomMetric metric;

  /** Grid position X (column) */
  @Column(name = "position_x")
  private Integer positionX;

  /** Grid position Y (row) */
  @Column(name = "position_y")
  private Integer positionY;

  /** Widget width in grid units */
  @Column(name = "width")
  private Integer width;

  /** Widget height in grid units */
  @Column(name = "height")
  private Integer height;

  /**
   * Data filtering configuration (JSON) Example: {"teamIds": [1,2], "cycleIds": [3], "dateRange":
   * {"start": "2026-01-01", "end": "2026-01-31"}}
   */
  @Column(name = "data_filters", columnDefinition = "TEXT")
  private String dataFilters;

  /**
   * Chart configuration (JSON) Example: {"chartType": "BAR", "colors": ["#2563eb", "#7c3aed"],
   * "legend": {"position": "bottom"}}
   */
  @Column(name = "chart_config", columnDefinition = "TEXT")
  private String chartConfig;

  /** Auto-refresh interval in seconds (NULL = no auto-refresh) */
  @Column(name = "refresh_interval")
  private Integer refreshInterval;

  /** Advanced: custom query for data fetching */
  @Column(name = "custom_query", columnDefinition = "TEXT")
  private String customQuery;

  /** Additional widget-specific settings (JSON) */
  @Column(name = "settings", columnDefinition = "TEXT")
  private String settings;

  /** Display order within the dashboard */
  @Column(name = "display_order")
  private Integer displayOrder;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
    if (positionX == null) positionX = 0;
    if (positionY == null) positionY = 0;
    if (width == null) width = 1;
    if (height == null) height = 1;
    if (displayOrder == null) displayOrder = 0;
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }

  /** Get the parsed config object from settings JSON */
  @JsonProperty("config")
  public Object getConfig() {
    if (settings == null || settings.isBlank()) {
      return null;
    }
    try {
      ObjectMapper mapper = new ObjectMapper();
      return mapper.readValue(settings, Object.class);
    } catch (Exception e) {
      return null;
    }
  }
}
