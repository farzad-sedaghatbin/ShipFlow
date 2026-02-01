package com.github.farzadsedaghatbin.shipflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.customdashboard.*;
import com.github.farzadsedaghatbin.shipflow.entity.*;
import com.github.farzadsedaghatbin.shipflow.repository.*;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service for managing custom dashboards */
@Service
@Slf4j
@RequiredArgsConstructor
public class CustomDashboardService {

  private final CustomDashboardRepository customDashboardRepository;
  private final DashboardWidgetConfigRepository widgetConfigRepository;
  private final UserRepository userRepository;
  private final DashboardWidgetRepository dashboardWidgetRepository;
  private final CustomMetricRepository customMetricRepository;
  private final CycleRepository cycleRepository;
  private final PitchRepository pitchRepository;
  private final TeamRepository teamRepository;
  private final ObjectMapper objectMapper;
  private final jakarta.persistence.EntityManager entityManager;
  private final LocalizationService localizationService;
  private final MessageService messageService;

  /** Create a new custom dashboard */
  @Transactional
  public CustomDashboardDTO createDashboard(Long userId, CreateDashboardRequest request) {
    log.info("Creating custom dashboard '{}' for user {}", request.getName(), userId);

    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

    CustomDashboard dashboard;

    // Clone from template if requested
    if (request.getCloneFromTemplateId() != null) {
      CustomDashboard template =
          customDashboardRepository
              .findById(request.getCloneFromTemplateId())
              .orElseThrow(
                  () ->
                      new IllegalArgumentException(
                          "Template not found: " + request.getCloneFromTemplateId()));

      if (!template.getIsTemplate()) {
        throw new IllegalArgumentException(
            localizationService.getMessage("dashboard.not.template"));
      }

      // Always generate unique name when cloning from template
      String dashboardName = generateUniqueDashboardName(userId, request.getName());
      log.info("Cloning from template, using unique name: {}", dashboardName);

      dashboard = cloneFromTemplate(user, dashboardName, request.getDescription(), template);
    } else { // Check for duplicate name when creating new dashboard
      if (customDashboardRepository.existsByUserIdAndName(userId, request.getName())) {
        throw new IllegalArgumentException(
            localizationService.getMessage("dashboard.name.duplicate", request.getName()));
      }
      // Resolve scope entities if provided
      Cycle cycle = null;
      if (request.getCycleId() != null) {
        cycle =
            cycleRepository
                .findById(request.getCycleId())
                .orElseThrow(
                    () -> new IllegalArgumentException("Cycle not found: " + request.getCycleId()));
      }

      Pitch pitch = null;
      if (request.getPitchId() != null) {
        pitch =
            pitchRepository
                .findById(request.getPitchId())
                .orElseThrow(
                    () -> new IllegalArgumentException("Pitch not found: " + request.getPitchId()));
      }

      Team team = null;
      if (request.getTeamId() != null) {
        team =
            teamRepository
                .findById(request.getTeamId())
                .orElseThrow(
                    () -> new IllegalArgumentException("Team not found: " + request.getTeamId()));
      }

      // Create new empty dashboard
      dashboard =
          CustomDashboard.builder()
              .user(user)
              .cycle(cycle)
              .pitch(pitch)
              .team(team)
              .name(request.getName())
              .description(request.getDescription())
              .isDefault(false)
              .layoutConfig(
                  request.getLayoutConfig() != null
                      ? request.getLayoutConfig()
                      : "{\"columns\": 12, \"rowHeight\": 60, \"breakpoints\": {\"lg\": 1200, \"md\": 996, \"sm\": 768}}")
              .isTemplate(false)
              .build();

      dashboard = customDashboardRepository.save(dashboard);
    }

    // Set as default if requested
    if (Boolean.TRUE.equals(request.getSetAsDefault())) {
      setDefaultDashboard(userId, dashboard.getId());
    }

    log.info("Created custom dashboard with ID: {}", dashboard.getId());
    return toDTO(dashboard);
  }

  /** Update an existing dashboard */
  @Transactional
  public CustomDashboardDTO updateDashboard(
      Long userId, Long dashboardId, UpdateDashboardRequest request) {
    log.info("Updating dashboard {} for user {}", dashboardId, userId);

    CustomDashboard dashboard =
        customDashboardRepository
            .findById(dashboardId)
            .orElseThrow(() -> new IllegalArgumentException("Dashboard not found: " + dashboardId));

    // Verify ownership
    if (!dashboard.getUser().getId().equals(userId)) {
      throw new SecurityException(localizationService.getMessage("dashboard.unauthorized.update"));
    }

    // Cannot update system templates
    if (dashboard.getIsTemplate()) {
      throw new IllegalArgumentException(
          localizationService.getMessage("dashboard.system.template"));
    }

    if (request.getName() != null) {
      if (!request.getName().equals(dashboard.getName())
          && customDashboardRepository.existsByUserIdAndName(userId, request.getName())) {
        throw new IllegalArgumentException(
            localizationService.getMessage("dashboard.name.duplicate", request.getName()));
      }
      dashboard.setName(request.getName());
    }

    if (request.getDescription() != null) {
      dashboard.setDescription(request.getDescription());
    }

    if (request.getLayoutConfig() != null) {
      dashboard.setLayoutConfig(request.getLayoutConfig());
    }

    CustomDashboard updated = customDashboardRepository.save(dashboard);
    log.info("Updated dashboard: {}", updated.getId());

    return toDTO(updated);
  }

  /** Delete a dashboard */
  @Transactional
  public void deleteDashboard(Long userId, Long dashboardId) {
    log.info("Deleting dashboard {} for user {}", dashboardId, userId);

    CustomDashboard dashboard =
        customDashboardRepository
            .findById(dashboardId)
            .orElseThrow(() -> new IllegalArgumentException("Dashboard not found: " + dashboardId));

    // Verify ownership (allow deleting own dashboards, even if created from template)
    if (!dashboard.getUser().getId().equals(userId)) {
      throw new SecurityException(localizationService.getMessage("dashboard.unauthorized.delete"));
    }

    // Cannot delete system templates (templates with null user or marked as template)
    if (Boolean.TRUE.equals(dashboard.getIsTemplate())) {
      throw new IllegalArgumentException(
          localizationService.getMessage("dashboard.cannot.delete.template"));
    }

    customDashboardRepository.delete(dashboard);
    log.info("Deleted dashboard: {}", dashboardId);

    // If this was the default, find another dashboard to set as default
    if (dashboard.getIsDefault()) {
      List<CustomDashboard> remaining =
          customDashboardRepository.findByUserIdOrderByNameAsc(userId);
      if (!remaining.isEmpty()) {
        setDefaultDashboard(userId, remaining.get(0).getId());
      }
    }
  }

  /** Get all dashboards for a user */
  public List<CustomDashboardDTO> getUserDashboards(Long userId) {
    return customDashboardRepository.findByUserIdOrderByNameAsc(userId).stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
  }

  /** Get a specific dashboard */
  public CustomDashboardDTO getDashboard(Long userId, Long dashboardId) {
    CustomDashboard dashboard =
        customDashboardRepository
            .findById(dashboardId)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        messageService.getMessage("error.dashboard.not.found", dashboardId)));

    if (!dashboard.getUser().getId().equals(userId) && !dashboard.getIsTemplate()) {
      throw new SecurityException(messageService.getMessage("error.dashboard.unauthorized"));
    }

    return toDTO(dashboard);
  }

  /** Set a dashboard as the default */
  @Transactional
  public void setDefaultDashboard(Long userId, Long dashboardId) {
    log.info("Setting dashboard {} as default for user {}", dashboardId, userId);

    CustomDashboard dashboard =
        customDashboardRepository
            .findById(dashboardId)
            .orElseThrow(() -> new IllegalArgumentException("Dashboard not found: " + dashboardId));

    if (!dashboard.getUser().getId().equals(userId)) {
      throw new SecurityException(localizationService.getMessage("dashboard.unauthorized.modify"));
    }

    // Clear all defaults for this user
    customDashboardRepository.clearDefaultDashboards(userId);

    // Set this one as default
    dashboard.setIsDefault(true);
    customDashboardRepository.save(dashboard);

    log.info("Set dashboard {} as default", dashboardId);
  }

  /**
   * Toggle user context filter for a dashboard When enabled, widgets show data filtered to user's
   * context (their cycles, teams, tasks) When disabled, widgets show organization-wide data
   */
  @Transactional
  public CustomDashboardDTO toggleUserContextFilter(Long userId, Long dashboardId) {
    log.info("Toggling user context filter for dashboard {} (user: {})", dashboardId, userId);

    CustomDashboard dashboard =
        customDashboardRepository
            .findById(dashboardId)
            .orElseThrow(() -> new IllegalArgumentException("Dashboard not found: " + dashboardId));

    if (!dashboard.getUser().getId().equals(userId)) {
      throw new SecurityException(localizationService.getMessage("dashboard.unauthorized.modify"));
    }

    // Toggle the filter
    Boolean currentValue = dashboard.getUserContextFilter();
    dashboard.setUserContextFilter(currentValue == null || !currentValue);

    CustomDashboard updated = customDashboardRepository.save(dashboard);
    log.info(
        "Toggled user context filter to {} for dashboard {}",
        updated.getUserContextFilter(),
        dashboardId);

    return toDTO(updated);
  }

  /** Get all available templates */
  @Transactional(readOnly = true)
  public List<CustomDashboardDTO> getTemplates() {
    // Try multiple approaches to find the issue
    List<CustomDashboard> allDashboards = customDashboardRepository.findAll();
    log.info("Total dashboards found: {}", allDashboards.size());

    List<CustomDashboard> templates =
        allDashboards.stream()
            .filter(d -> Boolean.TRUE.equals(d.getIsTemplate()))
            .collect(Collectors.toList());

    log.info("Templates found by filtering: {}", templates.size());

    return templates.stream().map(this::toDTO).collect(Collectors.toList());
  }

  /** Add a widget to a dashboard */
  @Transactional
  public DashboardWidgetConfig addWidget(Long userId, Long dashboardId, AddWidgetRequest request) {
    log.info("Adding widget to dashboard {} for user {}", dashboardId, userId);

    CustomDashboard dashboard =
        customDashboardRepository
            .findById(dashboardId)
            .orElseThrow(() -> new IllegalArgumentException("Dashboard not found: " + dashboardId));

    if (!dashboard.getUser().getId().equals(userId)) {
      throw new SecurityException(localizationService.getMessage("dashboard.unauthorized.modify"));
    }

    // Validate widget reference if provided
    DashboardWidget widget = null;
    if (request.getWidgetId() != null) {
      widget =
          dashboardWidgetRepository
              .findById(request.getWidgetId())
              .orElseThrow(
                  () -> new IllegalArgumentException("Widget not found: " + request.getWidgetId()));
    }

    // Validate metric reference if provided
    CustomMetric metric = null;
    if (request.getMetricId() != null) {
      metric =
          customMetricRepository
              .findById(request.getMetricId())
              .orElseThrow(
                  () -> new IllegalArgumentException("Metric not found: " + request.getMetricId()));
    }

    int displayOrder = widgetConfigRepository.getMaxDisplayOrder(dashboardId) + 1;

    // Serialize config object to JSON if provided
    String settingsJson = request.getSettings();
    if (request.getConfig() != null) {
      try {
        settingsJson = objectMapper.writeValueAsString(request.getConfig());
      } catch (Exception e) {
        log.error("Error serializing widget config", e);
        throw new IllegalArgumentException(
            messageService.getMessage("error.dashboard.widget.config.invalid"));
      }
    }

    DashboardWidgetConfig config =
        DashboardWidgetConfig.builder()
            .dashboard(dashboard)
            .widget(widget)
            .widgetType(request.getWidgetType())
            .metric(metric)
            .positionX(request.getPositionX())
            .positionY(request.getPositionY())
            .width(request.getWidth())
            .height(request.getHeight())
            .dataFilters(request.getDataFilters())
            .chartConfig(request.getChartConfig())
            .refreshInterval(request.getRefreshInterval())
            .settings(settingsJson)
            .displayOrder(displayOrder)
            .build();

    config = widgetConfigRepository.save(config);
    log.info("Added widget to dashboard: {}", dashboardId);
    return config;
  }

  /** Update a widget configuration */
  @Transactional
  public DashboardWidgetConfig updateWidget(
      Long userId, Long dashboardId, Long widgetConfigId, UpdateWidgetRequest request) {
    log.info("Updating widget {} on dashboard {}", widgetConfigId, dashboardId);

    DashboardWidgetConfig config =
        widgetConfigRepository
            .findById(widgetConfigId)
            .orElseThrow(
                () -> new IllegalArgumentException("Widget config not found: " + widgetConfigId));

    if (!config.getDashboard().getId().equals(dashboardId)) {
      throw new IllegalArgumentException(
          localizationService.getMessage("dashboard.widget.not.belong"));
    }

    if (!config.getDashboard().getUser().getId().equals(userId)) {
      throw new SecurityException(localizationService.getMessage("dashboard.unauthorized.modify"));
    }

    // Update fields
    if (request.getWidgetType() != null) {
      config.setWidgetType(request.getWidgetType());
    }
    if (request.getWidgetId() != null) {
      DashboardWidget widget =
          dashboardWidgetRepository
              .findById(request.getWidgetId())
              .orElseThrow(
                  () -> new IllegalArgumentException("Widget not found: " + request.getWidgetId()));
      config.setWidget(widget);
    }
    if (request.getMetricId() != null) {
      CustomMetric metric =
          customMetricRepository
              .findById(request.getMetricId())
              .orElseThrow(
                  () -> new IllegalArgumentException("Metric not found: " + request.getMetricId()));
      config.setMetric(metric);
    }
    if (request.getPositionX() != null) {
      config.setPositionX(request.getPositionX());
    }
    if (request.getPositionY() != null) {
      config.setPositionY(request.getPositionY());
    }
    if (request.getWidth() != null) {
      config.setWidth(request.getWidth());
    }
    if (request.getHeight() != null) {
      config.setHeight(request.getHeight());
    }
    if (request.getDataFilters() != null) {
      config.setDataFilters(request.getDataFilters());
    }
    if (request.getChartConfig() != null) {
      config.setChartConfig(request.getChartConfig());
    }
    if (request.getRefreshInterval() != null) {
      config.setRefreshInterval(request.getRefreshInterval());
    }
    if (request.getSettings() != null) {
      config.setSettings(request.getSettings());
    }
    if (request.getConfig() != null) {
      try {
        String settingsJson = objectMapper.writeValueAsString(request.getConfig());
        config.setSettings(settingsJson);
      } catch (Exception e) {
        log.error("Error serializing widget config", e);
        throw new IllegalArgumentException(
            localizationService.getMessage("dashboard.widget.invalid"));
      }
    }

    config = widgetConfigRepository.save(config);
    log.info("Updated widget configuration");
    return config;
  }

  /** Remove a widget from a dashboard */
  @Transactional
  public void removeWidget(Long userId, Long dashboardId, Long widgetConfigId) {
    log.info("Removing widget {} from dashboard {}", widgetConfigId, dashboardId);

    DashboardWidgetConfig config =
        widgetConfigRepository
            .findById(widgetConfigId)
            .orElseThrow(
                () -> new IllegalArgumentException("Widget config not found: " + widgetConfigId));

    if (!config.getDashboard().getId().equals(dashboardId)) {
      throw new IllegalArgumentException(
          localizationService.getMessage("dashboard.widget.not.belong"));
    }

    if (!config.getDashboard().getUser().getId().equals(userId)) {
      throw new SecurityException(localizationService.getMessage("dashboard.unauthorized.modify"));
    }

    widgetConfigRepository.delete(config);
    log.info("Removed widget from dashboard");
  }

  /** Get all widget configurations for a dashboard */
  public List<DashboardWidgetConfig> getDashboardWidgets(Long userId, Long dashboardId) {
    CustomDashboard dashboard =
        customDashboardRepository
            .findById(dashboardId)
            .orElseThrow(() -> new IllegalArgumentException("Dashboard not found: " + dashboardId));

    if (!dashboard.getUser().getId().equals(userId) && !dashboard.getIsTemplate()) {
      throw new SecurityException(localizationService.getMessage("dashboard.unauthorized.view"));
    }

    return widgetConfigRepository.findByDashboardIdOrderByDisplayOrderAsc(dashboardId);
  }

  private CustomDashboard cloneFromTemplate(
      User user, String name, String description, CustomDashboard template) {
    log.info("Cloning dashboard from template: {}", template.getId());

    // Create new dashboard based on template
    CustomDashboard dashboard =
        CustomDashboard.builder()
            .user(user)
            .name(name != null ? name : template.getName() + " Copy")
            .description(description != null ? description : template.getDescription())
            .isDefault(false)
            .layoutConfig(template.getLayoutConfig())
            .isTemplate(false)
            .build();

    dashboard = customDashboardRepository.save(dashboard);

    // Clone all widget configurations
    List<DashboardWidgetConfig> templateWidgets =
        widgetConfigRepository.findByDashboardIdOrderByDisplayOrderAsc(template.getId());

    for (DashboardWidgetConfig templateWidget : templateWidgets) {
      DashboardWidgetConfig newWidget =
          DashboardWidgetConfig.builder()
              .dashboard(dashboard)
              .widget(templateWidget.getWidget())
              .widgetType(templateWidget.getWidgetType())
              .metric(templateWidget.getMetric())
              .positionX(templateWidget.getPositionX())
              .positionY(templateWidget.getPositionY())
              .width(templateWidget.getWidth())
              .height(templateWidget.getHeight())
              .dataFilters(templateWidget.getDataFilters())
              .chartConfig(templateWidget.getChartConfig())
              .refreshInterval(templateWidget.getRefreshInterval())
              .settings(templateWidget.getSettings())
              .displayOrder(templateWidget.getDisplayOrder())
              .build();

      widgetConfigRepository.save(newWidget);
    }

    log.info("Cloned {} widgets from template", templateWidgets.size());
    return dashboard;
  }

  /** Generate a unique dashboard name by appending a number */
  private String generateUniqueDashboardName(Long userId, String baseName) {
    // Flush any pending changes to ensure we check against the latest DB state
    entityManager.flush();
    entityManager.clear();

    String candidateName = baseName;
    int counter = 1;

    while (customDashboardRepository.existsByUserIdAndName(userId, candidateName)) {
      candidateName = baseName + " (" + counter + ")";
      counter++;
    }

    log.info("Generated unique dashboard name: {} for base name: {}", candidateName, baseName);
    return candidateName;
  }

  private CustomDashboardDTO toDTO(CustomDashboard dashboard) {
    long widgetCount = widgetConfigRepository.countByDashboardId(dashboard.getId());

    return CustomDashboardDTO.builder()
        .id(dashboard.getId())
        .name(dashboard.getName())
        .description(dashboard.getDescription())
        .isDefault(dashboard.getIsDefault())
        .layoutConfig(dashboard.getLayoutConfig())
        .isTemplate(dashboard.getIsTemplate())
        .userContextFilter(dashboard.getUserContextFilter())
        .templateCategory(dashboard.getTemplateCategory())
        .widgetCount(widgetCount)
        .cycleId(dashboard.getCycle() != null ? dashboard.getCycle().getId() : null)
        .cycleName(dashboard.getCycle() != null ? dashboard.getCycle().getName() : null)
        .pitchId(dashboard.getPitch() != null ? dashboard.getPitch().getId() : null)
        .pitchName(dashboard.getPitch() != null ? dashboard.getPitch().getTitle() : null)
        .teamId(dashboard.getTeam() != null ? dashboard.getTeam().getId() : null)
        .teamName(dashboard.getTeam() != null ? dashboard.getTeam().getName() : null)
        .createdAt(dashboard.getCreatedAt())
        .updatedAt(dashboard.getUpdatedAt())
        .build();
  }
}
