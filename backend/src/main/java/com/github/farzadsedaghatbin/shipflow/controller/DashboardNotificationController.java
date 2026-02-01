package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.dashboard.DashboardNotificationDTO;
import com.github.farzadsedaghatbin.shipflow.service.DashboardNotificationService;
import com.github.farzadsedaghatbin.shipflow.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/** REST controller for dashboard notifications. */
@RestController
@RequestMapping("/api/dashboard/notifications")
@RequiredArgsConstructor
@Tag(name = "Dashboard Notifications", description = "Manage dashboard notifications")
public class DashboardNotificationController {

  private final DashboardNotificationService notificationService;
  private final UserService userService;

  @GetMapping
  @Operation(
      summary = "Get all notifications",
      description = "Get all active notifications for the current user")
  public ResponseEntity<List<DashboardNotificationDTO>> getUserNotifications() {
    Long userId = getCurrentUserId();
    List<DashboardNotificationDTO> notifications = notificationService.getUserNotifications(userId);
    return ResponseEntity.ok(notifications);
  }

  @GetMapping("/unread")
  @Operation(
      summary = "Get unread notifications",
      description = "Get only unread notifications for the current user")
  public ResponseEntity<List<DashboardNotificationDTO>> getUnreadNotifications() {
    Long userId = getCurrentUserId();
    List<DashboardNotificationDTO> notifications =
        notificationService.getUnreadNotifications(userId);
    return ResponseEntity.ok(notifications);
  }

  @GetMapping("/unread/count")
  @Operation(summary = "Get unread count", description = "Get the count of unread notifications")
  public ResponseEntity<Map<String, Long>> getUnreadCount() {
    Long userId = getCurrentUserId();
    Long count = notificationService.getUnreadCount(userId);
    return ResponseEntity.ok(Map.of("count", count));
  }

  @PutMapping("/{id}/read")
  @Operation(summary = "Mark as read", description = "Mark a specific notification as read")
  public ResponseEntity<DashboardNotificationDTO> markAsRead(@PathVariable Long id) {
    DashboardNotificationDTO notification = notificationService.markAsRead(id);
    return ResponseEntity.ok(notification);
  }

  @PutMapping("/read-all")
  @Operation(
      summary = "Mark all as read",
      description = "Mark all notifications as read for the current user")
  public ResponseEntity<Void> markAllAsRead() {
    Long userId = getCurrentUserId();
    notificationService.markAllAsRead(userId);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Delete notification", description = "Delete a specific notification")
  public ResponseEntity<Void> deleteNotification(@PathVariable Long id) {
    notificationService.deleteNotification(id);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/generate")
  @Operation(
      summary = "Generate notifications",
      description = "Manually trigger notification generation (admin only)")
  public ResponseEntity<Void> generateNotifications() {
    notificationService.generateDailyNotifications();
    return ResponseEntity.noContent().build();
  }

  private Long getCurrentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String username = authentication.getName();
    return userService.findByUsername(username).getId();
  }
}
