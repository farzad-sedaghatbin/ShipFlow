package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.dashboard.CreateDashboardWidgetRequest;
import com.github.farzadsedaghatbin.shipflow.dto.dashboard.DashboardWidgetDTO;
import com.github.farzadsedaghatbin.shipflow.dto.dashboard.UpdateDashboardWidgetRequest;
import com.github.farzadsedaghatbin.shipflow.entity.DashboardWidget;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ProjectType;
import com.github.farzadsedaghatbin.shipflow.repository.DashboardWidgetRepository;
import com.github.farzadsedaghatbin.shipflow.repository.ProjectRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
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
  private final ProjectRepository projectRepository;
  private final EntityManager entityManager;

  // Task-based widgets meaningful for every project type.
  private static final List<String> GENERIC_WIDGETS = Arrays.asList(
      "OVERDUE_TASKS", "BLOCKED_TASKS", "UPCOMING_DEADLINES", "MY_TASKS",
      "TEAM_WORKLOAD", "RECENT_ACTIVITY");

  // Cycle-based widgets — apply to Shape Up and Scrum alike, not Kanban.
  // CYCLE_PROGRESS is included here (not Shape-Up-only): the frontend
  // widget computes it from Task data ("stories") for Scrum cycles and
  // Pitch data for Shape Up, see CycleProgressWidget.tsx.
  private static final List<String> CYCLE_WIDGETS = Arrays.asList(
      "CYCLE_PROGRESS", "CYCLE_SUMMARY", "CYCLE_SIGNALS", "AI_RISK_ADVISORY", "ACTIVE_CYCLES");

  // Pitch/hill-chart widgets — Shape Up only (Scrum has no pitches).
  private static final List<String> SHAPE_UP_ONLY_WIDGETS = Arrays.asList("HILL_CHART", "RECENT_PITCHES");

  // All default widgets combined — the full Shape Up set, mirrors
  // frontend/src/config/projectTypeCapabilities.ts's SHAPE_UP.defaultWidgetTypes.
  private static final List<String> DEFAULT_WIDGETS = concat(GENERIC_WIDGETS, CYCLE_WIDGETS, SHAPE_UP_ONLY_WIDGETS);

  private static List<String> concat(List<String>... lists) {
    List<String> result = new ArrayList<>();
    for (List<String> list : lists) {
      result.addAll(list);
    }
    return result;
  }

  /**
   * Widget types meaningful for the project types currently active in this
   * deployment — mirrors frontend/src/config/projectTypeCapabilities.ts's
   * resolveOrgCapabilities so a Kanban-only or Scrum-only deployment doesn't
   * seed new users with widgets (Hill Chart, AI Risk Advisory, etc.) that can
   * never render anything for them. Falls back to the generic baseline when
   * there are no active projects yet — never defaults to the full Shape Up
   * set just because nothing exists.
   */
  private List<String> resolveDefaultWidgetTypesForDeployment() {
    Set<ProjectType> activeTypes = EnumSet.noneOf(ProjectType.class);
    activeTypes.addAll(projectRepository.findDistinctActiveProjectTypes());

    if (activeTypes.isEmpty() || (activeTypes.size() == 1 && activeTypes.contains(ProjectType.KANBAN))) {
      return GENERIC_WIDGETS;
    }
    if (activeTypes.contains(ProjectType.SHAPE_UP)) {
      return DEFAULT_WIDGETS;
    }
    // Remaining case: SCRUM present, no SHAPE_UP — cycle widgets apply, hill chart/pitches don't.
    return concat(GENERIC_WIDGETS, CYCLE_WIDGETS);
  }

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

  /** Create default widgets for a new user, scoped to what's meaningful for the
   *  project types actually active in this deployment. Existing users' widget
   *  rows are never touched by this — only invoked when a user has none yet. */
  private List<DashboardWidgetDTO> createDefaultWidgets(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

    List<String> widgetTypes = resolveDefaultWidgetTypesForDeployment();
    List<DashboardWidget> defaultWidgets = new ArrayList<>();
    for (int i = 0; i < widgetTypes.size(); i++) {
      String type = widgetTypes.get(i);
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
