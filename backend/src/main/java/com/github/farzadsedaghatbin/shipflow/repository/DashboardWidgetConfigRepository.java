package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.DashboardWidgetConfig;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DashboardWidgetConfigRepository
    extends JpaRepository<DashboardWidgetConfig, Long> {

  /** Find all widget configs for a dashboard */
  List<DashboardWidgetConfig> findByDashboardIdOrderByDisplayOrderAsc(Long dashboardId);

  /** Find widgets linked to a specific metric */
  List<DashboardWidgetConfig> findByMetricId(Long metricId);

  /** Find widgets of a specific type on a dashboard */
  List<DashboardWidgetConfig> findByDashboardIdAndWidgetType(Long dashboardId, String widgetType);

  /** Count widgets on a dashboard */
  long countByDashboardId(Long dashboardId);

  /** Delete all widgets for a dashboard (cascade handled by DB, but useful for manual cleanup) */
  void deleteByDashboardId(Long dashboardId);

  /** Get maximum display order for a dashboard (for appending new widgets) */
  @Query(
      "SELECT COALESCE(MAX(w.displayOrder), 0) FROM DashboardWidgetConfig w WHERE w.dashboard.id = :dashboardId")
  int getMaxDisplayOrder(@Param("dashboardId") Long dashboardId);
}
