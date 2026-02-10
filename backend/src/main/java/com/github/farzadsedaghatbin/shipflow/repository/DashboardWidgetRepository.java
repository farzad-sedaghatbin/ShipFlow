package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.DashboardWidget;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

/** Repository for dashboard widget configurations. */
@Repository
public interface DashboardWidgetRepository extends JpaRepository<DashboardWidget, Long> {

  /** Find all widgets for a user, ordered by display order */
  List<DashboardWidget> findByUserIdOrderByDisplayOrderAsc(Long userId);

  /** Find a specific widget by user and type */
  Optional<DashboardWidget> findByUserIdAndWidgetType(Long userId, String widgetType);

  /** Find all visible widgets for a user */
  List<DashboardWidget> findByUserIdAndIsVisibleOrderByDisplayOrderAsc(Long userId, Boolean isVisible);

  /** Delete all widgets for a user */
  @Modifying
  void deleteByUserId(Long userId);
}
