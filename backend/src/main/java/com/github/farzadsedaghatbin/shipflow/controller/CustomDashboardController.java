package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.customdashboard.*;
import com.github.farzadsedaghatbin.shipflow.entity.DashboardWidgetConfig;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import com.github.farzadsedaghatbin.shipflow.service.CustomDashboardService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/** REST controller for custom dashboard management */
@RestController
@RequestMapping("/api/dashboards/custom")
@RequiredArgsConstructor
@Slf4j
public class CustomDashboardController {

  private final CustomDashboardService customDashboardService;
  private final UserRepository userRepository;

  /** Create a new custom dashboard */
  @PostMapping
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<CustomDashboardDTO> createDashboard(@AuthenticationPrincipal UserDetails userDetails,
      @Valid @RequestBody CreateDashboardRequest request) {
    Long userId = getUserId(userDetails);
    log.info("Creating custom dashboard for user: {}", userId);
    CustomDashboardDTO dashboard = customDashboardService.createDashboard(userId, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(dashboard);
  }

  /** Get all dashboards for the current user */
  @GetMapping
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<List<CustomDashboardDTO>> getUserDashboards(
      @AuthenticationPrincipal UserDetails userDetails) {
    Long userId = getUserId(userDetails);
    log.info("Fetching dashboards for user: {}", userId);
    List<CustomDashboardDTO> dashboards = customDashboardService.getUserDashboards(userId);
    return ResponseEntity.ok(dashboards);
  }

  /** Get a specific dashboard by ID */
  @GetMapping("/{id}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<CustomDashboardDTO> getDashboard(@AuthenticationPrincipal UserDetails userDetails,
      @PathVariable Long id) {
    Long userId = getUserId(userDetails);
    log.info("Fetching dashboard {} for user: {}", id, userId);
    CustomDashboardDTO dashboard = customDashboardService.getDashboard(userId, id);
    return ResponseEntity.ok(dashboard);
  }

  /** Update an existing dashboard */
  @PutMapping("/{id}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<CustomDashboardDTO> updateDashboard(@AuthenticationPrincipal UserDetails userDetails,
      @PathVariable Long id, @Valid @RequestBody UpdateDashboardRequest request) {
    Long userId = getUserId(userDetails);
    log.info("Updating dashboard {} for user: {}", id, userId);
    CustomDashboardDTO dashboard = customDashboardService.updateDashboard(userId, id, request);
    return ResponseEntity.ok(dashboard);
  }

  /** Delete a dashboard */
  @DeleteMapping("/{id}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<Void> deleteDashboard(@AuthenticationPrincipal UserDetails userDetails,
      @PathVariable Long id) {
    Long userId = getUserId(userDetails);
    log.info("Deleting dashboard {} for user: {}", id, userId);
    customDashboardService.deleteDashboard(userId, id);
    return ResponseEntity.noContent().build();
  }

  /** Set a dashboard as the default */
  @PostMapping("/{id}/set-default")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<Void> setDefaultDashboard(@AuthenticationPrincipal UserDetails userDetails,
      @PathVariable Long id) {
    Long userId = getUserId(userDetails);
    log.info("Setting dashboard {} as default for user: {}", id, userId);
    customDashboardService.setDefaultDashboard(userId, id);
    return ResponseEntity.ok().build();
  }

  /**
   * Toggle user context filter for a dashboard When enabled, widgets show data
   * filtered to user's context (their cycles, teams, tasks) When disabled,
   * widgets show organization-wide data
   */
  @PutMapping("/{id}/toggle-context-filter")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<CustomDashboardDTO> toggleUserContextFilter(@AuthenticationPrincipal UserDetails userDetails,
      @PathVariable Long id) {
    Long userId = getUserId(userDetails);
    log.info("Toggling user context filter for dashboard {} (user: {})", id, userId);
    CustomDashboardDTO dashboard = customDashboardService.toggleUserContextFilter(userId, id);
    return ResponseEntity.ok(dashboard);
  }

  /** Get available dashboard templates */
  @GetMapping("/templates")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<List<CustomDashboardDTO>> getTemplates() {
    log.info("Fetching dashboard templates");
    List<CustomDashboardDTO> templates = customDashboardService.getTemplates();
    return ResponseEntity.ok(templates);
  }

  /** Add a widget to a dashboard */
  @PostMapping("/{id}/widgets")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<DashboardWidgetConfig> addWidget(@AuthenticationPrincipal UserDetails userDetails,
      @PathVariable Long id, @Valid @RequestBody AddWidgetRequest request) {
    Long userId = getUserId(userDetails);
    log.info("Adding widget to dashboard {} for user: {}", id, userId);
    DashboardWidgetConfig widget = customDashboardService.addWidget(userId, id, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(widget);
  }

  /** Get all widgets for a dashboard */
  @GetMapping("/{id}/widgets")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<List<DashboardWidgetConfig>> getDashboardWidgets(
      @AuthenticationPrincipal UserDetails userDetails, @PathVariable Long id) {
    Long userId = getUserId(userDetails);
    log.info("Fetching widgets for dashboard {} (user: {})", id, userId);
    List<DashboardWidgetConfig> widgets = customDashboardService.getDashboardWidgets(userId, id);
    return ResponseEntity.ok(widgets);
  }

  /** Update a widget configuration */
  @PutMapping("/{dashboardId}/widgets/{widgetId}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<DashboardWidgetConfig> updateWidget(@AuthenticationPrincipal UserDetails userDetails,
      @PathVariable Long dashboardId, @PathVariable Long widgetId,
      @Valid @RequestBody UpdateWidgetRequest request) {
    Long userId = getUserId(userDetails);
    log.info("Updating widget {} on dashboard {} for user: {}", widgetId, dashboardId, userId);
    DashboardWidgetConfig updated = customDashboardService.updateWidget(userId, dashboardId, widgetId, request);
    return ResponseEntity.ok(updated);
  }

  /** Remove a widget from a dashboard */
  @DeleteMapping("/{dashboardId}/widgets/{widgetId}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<Void> removeWidget(@AuthenticationPrincipal UserDetails userDetails,
      @PathVariable Long dashboardId, @PathVariable Long widgetId) {
    Long userId = getUserId(userDetails);
    log.info("Removing widget {} from dashboard {} for user: {}", widgetId, dashboardId, userId);
    customDashboardService.removeWidget(userId, dashboardId, widgetId);
    return ResponseEntity.noContent().build();
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException e) {
    log.error("Validation error: {}", e.getMessage());
    return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
  }

  @ExceptionHandler(SecurityException.class)
  public ResponseEntity<Map<String, String>> handleSecurityException(SecurityException e) {
    log.error("Security error: {}", e.getMessage());
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
  }

  private Long getUserId(UserDetails userDetails) {
    return userRepository.findByUsername(userDetails.getUsername()).map(User::getId)
        .orElseThrow(() -> new RuntimeException("User not found"));
  }
}
