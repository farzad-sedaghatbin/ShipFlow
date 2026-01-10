package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.dashboard.CreateDashboardWidgetRequest;
import com.github.farzadsedaghatbin.shipflow.dto.dashboard.DashboardWidgetDTO;
import com.github.farzadsedaghatbin.shipflow.dto.dashboard.UpdateDashboardWidgetRequest;
import com.github.farzadsedaghatbin.shipflow.service.DashboardWidgetService;
import com.github.farzadsedaghatbin.shipflow.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for dashboard widget management.
 */
@RestController
@RequestMapping("/api/dashboard/widgets")
@RequiredArgsConstructor
@Tag(name = "Dashboard Widgets", description = "Manage dashboard widget configurations")
public class DashboardWidgetController {

    private final DashboardWidgetService dashboardWidgetService;
    private final UserService userService;

    @GetMapping
    @Operation(summary = "Get user's dashboard widgets", 
               description = "Get all widget configurations for the current user")
    public ResponseEntity<List<DashboardWidgetDTO>> getUserWidgets() {
        Long userId = getCurrentUserId();
        List<DashboardWidgetDTO> widgets = dashboardWidgetService.getUserWidgets(userId);
        return ResponseEntity.ok(widgets);
    }

    @GetMapping("/visible")
    @Operation(summary = "Get visible widgets", 
               description = "Get only visible widgets for the current user")
    public ResponseEntity<List<DashboardWidgetDTO>> getVisibleWidgets() {
        Long userId = getCurrentUserId();
        List<DashboardWidgetDTO> widgets = dashboardWidgetService.getVisibleWidgets(userId);
        return ResponseEntity.ok(widgets);
    }

    @PostMapping
    @Operation(summary = "Create a new widget", 
               description = "Add a new widget to the user's dashboard")
    public ResponseEntity<DashboardWidgetDTO> createWidget(
            @Valid @RequestBody CreateDashboardWidgetRequest request) {
        Long userId = getCurrentUserId();
        DashboardWidgetDTO widget = dashboardWidgetService.createWidget(userId, request);
        return ResponseEntity.ok(widget);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a widget", 
               description = "Update widget visibility, order, or settings")
    public ResponseEntity<DashboardWidgetDTO> updateWidget(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDashboardWidgetRequest request) {
        DashboardWidgetDTO widget = dashboardWidgetService.updateWidget(id, request);
        return ResponseEntity.ok(widget);
    }

    @PutMapping("/bulk")
    @Operation(summary = "Bulk update widgets", 
               description = "Update multiple widgets at once (useful for reordering)")
    public ResponseEntity<List<DashboardWidgetDTO>> bulkUpdateWidgets(
            @Valid @RequestBody List<DashboardWidgetDTO> updates) {
        Long userId = getCurrentUserId();
        List<DashboardWidgetDTO> widgets = dashboardWidgetService.bulkUpdateWidgets(userId, updates);
        return ResponseEntity.ok(widgets);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a widget", 
               description = "Remove a widget from the dashboard")
    public ResponseEntity<Void> deleteWidget(@PathVariable Long id) {
        dashboardWidgetService.deleteWidget(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset")
    @Operation(summary = "Reset to defaults", 
               description = "Reset all widgets to default configuration")
    public ResponseEntity<List<DashboardWidgetDTO>> resetToDefaults() {
        Long userId = getCurrentUserId();
        List<DashboardWidgetDTO> widgets = dashboardWidgetService.resetToDefaults(userId);
        return ResponseEntity.ok(widgets);
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userService.findByUsername(username).getId();
    }
}
