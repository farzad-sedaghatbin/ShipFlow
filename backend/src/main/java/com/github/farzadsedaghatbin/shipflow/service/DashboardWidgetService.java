package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.dashboard.CreateDashboardWidgetRequest;
import com.github.farzadsedaghatbin.shipflow.dto.dashboard.DashboardWidgetDTO;
import com.github.farzadsedaghatbin.shipflow.dto.dashboard.UpdateDashboardWidgetRequest;
import com.github.farzadsedaghatbin.shipflow.entity.DashboardWidget;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.repository.DashboardWidgetRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service for managing dashboard widget configurations. */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DashboardWidgetService {

  private final DashboardWidgetRepository dashboardWidgetRepository;
  private final UserRepository userRepository;
  private final EntityManager entityManager;

  // Default widget types - organized by tab
  // Overview tab widgets
  private static final List<String> OVERVIEW_WIDGETS = Arrays.asList(
      "ACTIVE_CYCLES", "HILL_CHART", "CYCLE_PROGRESS", "RECENT_PITCHES");
  
  // AI Insights tab widgets
  private static final List<String> AI_INSIGHTS_WIDGETS = Arrays.asList(
      "CYCLE_SUMMARY", "CYCLE_SIGNALS", "AI_RISK_ADVISORY");
  
  // Activity tab widgets
  private static final List<String> ACTIVITY_WIDGETS = Arrays.asList(
      "RECENT_ACTIVITY", "OVERDUE_TASKS", "BLOCKED_TASKS", 
      "UPCOMING_DEADLINES", "MY_TASKS", "TEAM_WORKLOAD");
  
  // All default widgets combined
  private static final List<String> DEFAULT_WIDGETS = Arrays.asList(
      "OVERDUE_TASKS", "BLOCKED_TASKS", "UPCOMING_DEADLINES", "MY_TASKS", 
      "TEAM_WORKLOAD", "CYCLE_PROGRESS", "RECENT_ACTIVITY",
      "CYCLE_SUMMARY", "CYCLE_SIGNALS", "AI_RISK_ADVISORY",
      "ACTIVE_CYCLES", "HILL_CHART", "RECENT_PITCHES");

  /** Get all widgets for a user. Create defaults if none exist. */
  @Transactional
  public List<DashboardWidgetDTO> getUserWidgets(Long userId) {
    List<DashboardWidget> widgets = dashboardWidgetRepository.findByUserIdOrderByDisplayOrderAsc(userId);

    // Create default widgets if none exist
    if (widgets.isEmpty()) {
      log.info("Creating default widgets for user {}", userId);
      return createDefaultWidgets(userId);
    }

    return widgets.stream().map(this::toDTO).collect(Collectors.toList());
  }

  /** Get only visible widgets for a user */
  @Transactional(readOnly = true)
  public List<DashboardWidgetDTO> getVisibleWidgets(Long userId) {
    List<DashboardWidget> widgets = dashboardWidgetRepository.findByUserIdAndIsVisibleOrderByDisplayOrderAsc(userId,
        true);

    return widgets.stream().map(this::toDTO).collect(Collectors.toList());
  }

  /** Create a new widget for a user */
  public DashboardWidgetDTO createWidget(Long userId, CreateDashboardWidgetRequest request) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

    DashboardWidget widget = DashboardWidget.builder().user(user).widgetType(request.getWidgetType())
        .isVisible(request.getIsVisible()).displayOrder(request.getDisplayOrder())
        .layoutConfig(request.getLayoutConfig()).settings(request.getSettings()).build();

    DashboardWidget saved = dashboardWidgetRepository.save(widget);
    log.info("Created widget {} for user {}", saved.getWidgetType(), userId);
    return toDTO(saved);
  }

  /** Update a widget */
  public DashboardWidgetDTO updateWidget(Long widgetId, UpdateDashboardWidgetRequest request) {
    DashboardWidget widget = dashboardWidgetRepository.findById(widgetId)
        .orElseThrow(() -> new RuntimeException("Widget not found with id: " + widgetId));

    if (request.getIsVisible() != null) {
      widget.setIsVisible(request.getIsVisible());
    }
    if (request.getDisplayOrder() != null) {
      widget.setDisplayOrder(request.getDisplayOrder());
    }
    if (request.getLayoutConfig() != null) {
      widget.setLayoutConfig(request.getLayoutConfig());
    }
    if (request.getSettings() != null) {
      widget.setSettings(request.getSettings());
    }

    DashboardWidget saved = dashboardWidgetRepository.save(widget);
    log.info("Updated widget {}", widgetId);
    return toDTO(saved);
  }

  /** Bulk update widget order and visibility */
  public List<DashboardWidgetDTO> bulkUpdateWidgets(Long userId, List<DashboardWidgetDTO> updates) {
    List<DashboardWidget> widgets = dashboardWidgetRepository.findByUserIdOrderByDisplayOrderAsc(userId);

    for (DashboardWidgetDTO update : updates) {
      widgets.stream().filter(w -> w.getId().equals(update.getId())).findFirst().ifPresent(widget -> {
        if (update.getIsVisible() != null) {
          widget.setIsVisible(update.getIsVisible());
        }
        if (update.getDisplayOrder() != null) {
          widget.setDisplayOrder(update.getDisplayOrder());
        }
        if (update.getLayoutConfig() != null) {
          widget.setLayoutConfig(update.getLayoutConfig());
        }
        if (update.getSettings() != null) {
          widget.setSettings(update.getSettings());
        }
      });
    }

    List<DashboardWidget> saved = dashboardWidgetRepository.saveAll(widgets);
    log.info("Bulk updated {} widgets for user {}", saved.size(), userId);
    return saved.stream().map(this::toDTO).collect(Collectors.toList());
  }

  /** Delete a widget */
  public void deleteWidget(Long widgetId) {
    dashboardWidgetRepository.deleteById(widgetId);
    log.info("Deleted widget {}", widgetId);
  }

  /** Reset to default widgets */
  public List<DashboardWidgetDTO> resetToDefaults(Long userId) {
    dashboardWidgetRepository.deleteByUserId(userId);
    entityManager.flush(); // Ensure deletions are committed before inserts
    log.info("Reset widgets to defaults for user {}", userId);
    return createDefaultWidgets(userId);
  }

  /** Create default widgets for a new user */
  private List<DashboardWidgetDTO> createDefaultWidgets(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

    List<DashboardWidget> defaultWidgets = new java.util.ArrayList<>();
    for (int i = 0; i < DEFAULT_WIDGETS.size(); i++) {
      String type = DEFAULT_WIDGETS.get(i);
      DashboardWidget widget = DashboardWidget.builder().user(user).widgetType(type).isVisible(true)
          .displayOrder(i).build();
      defaultWidgets.add(widget);
    }

    List<DashboardWidget> saved = dashboardWidgetRepository.saveAll(defaultWidgets);
    return saved.stream().map(this::toDTO).collect(Collectors.toList());
  }

  private DashboardWidgetDTO toDTO(DashboardWidget widget) {
    return DashboardWidgetDTO.builder().id(widget.getId()).userId(widget.getUser().getId())
        .widgetType(widget.getWidgetType()).isVisible(widget.getIsVisible())
        .displayOrder(widget.getDisplayOrder()).layoutConfig(widget.getLayoutConfig())
        .settings(widget.getSettings()).createdAt(widget.getCreatedAt()).updatedAt(widget.getUpdatedAt())
        .build();
  }
}
