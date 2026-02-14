package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.cooldown.*;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CooldownActivityStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CooldownActivityType;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import com.github.farzadsedaghatbin.shipflow.service.CooldownActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/cooldown-activities", "/cooldown-activities"})
@RequiredArgsConstructor
@Tag(name = "Cooldown Activities", description = "Manage cooldown phase activities")
public class CooldownActivityController {

  private final CooldownActivityService cooldownActivityService;
  private final UserRepository userRepository;

  @PostMapping
  @Operation(summary = "Create a cooldown activity", description = "Create a new activity for tracking during cooldown phase")
  public ResponseEntity<CooldownActivityDTO> createActivity(
      @Valid @RequestBody CreateCooldownActivityRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    Long userId = getUserId(userDetails);
    CooldownActivityDTO created = cooldownActivityService.createActivity(request, userId);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get activity by ID", description = "Retrieve a cooldown activity by its ID")
  public ResponseEntity<CooldownActivityDTO> getActivity(@PathVariable Long id) {
    return ResponseEntity.ok(cooldownActivityService.getActivity(id));
  }

  @GetMapping("/cycle/{cycleId}")
  @Operation(summary = "Get activities by cycle", description = "Retrieve all cooldown activities for a cycle")
  public ResponseEntity<List<CooldownActivityDTO>> getActivitiesByCycle(@PathVariable Long cycleId) {
    return ResponseEntity.ok(cooldownActivityService.getActivitiesByCycle(cycleId));
  }

  @GetMapping("/cycle/{cycleId}/summary")
  @Operation(summary = "Get cycle cooldown summary", description = "Retrieve comprehensive summary of cooldown activities for a cycle")
  public ResponseEntity<CooldownSummaryDTO> getCycleSummary(@PathVariable Long cycleId) {
    return ResponseEntity.ok(cooldownActivityService.getCycleSummary(cycleId));
  }

  @GetMapping("/cycle/{cycleId}/status/{status}")
  @Operation(summary = "Get activities by status", description = "Retrieve cooldown activities filtered by status")
  public ResponseEntity<List<CooldownActivityDTO>> getActivitiesByStatus(
      @PathVariable Long cycleId,
      @PathVariable CooldownActivityStatus status) {
    return ResponseEntity.ok(cooldownActivityService.getActivitiesByStatus(cycleId, status));
  }

  @GetMapping("/cycle/{cycleId}/type/{type}")
  @Operation(summary = "Get activities by type", description = "Retrieve cooldown activities filtered by type")
  public ResponseEntity<List<CooldownActivityDTO>> getActivitiesByType(
      @PathVariable Long cycleId,
      @PathVariable CooldownActivityType type) {
    return ResponseEntity.ok(cooldownActivityService.getActivitiesByType(cycleId, type));
  }

  @GetMapping("/assignee/{userId}")
  @Operation(summary = "Get activities by assignee", description = "Retrieve all cooldown activities assigned to a user")
  public ResponseEntity<List<CooldownActivityDTO>> getActivitiesByAssignee(@PathVariable Long userId) {
    return ResponseEntity.ok(cooldownActivityService.getActivitiesByAssignee(userId));
  }

  @PutMapping("/{id}")
  @Operation(summary = "Update activity", description = "Update cooldown activity details (partial updates supported)")
  public ResponseEntity<CooldownActivityDTO> updateActivity(
      @PathVariable Long id,
      @Valid @RequestBody UpdateCooldownActivityRequest request) {
    return ResponseEntity.ok(cooldownActivityService.updateActivity(id, request));
  }

  @PatchMapping("/{id}/status")
  @Operation(summary = "Update activity status", description = "Update the status of a cooldown activity")
  public ResponseEntity<CooldownActivityDTO> updateStatus(
      @PathVariable Long id,
      @RequestParam CooldownActivityStatus status,
      @RequestParam(required = false) Integer actualHours) {
    return ResponseEntity.ok(cooldownActivityService.updateStatus(id, status, actualHours));
  }

  @PatchMapping("/{id}/start")
  @Operation(summary = "Start activity", description = "Mark activity as in-progress")
  public ResponseEntity<CooldownActivityDTO> startActivity(@PathVariable Long id) {
    return ResponseEntity.ok(cooldownActivityService.updateStatus(id, CooldownActivityStatus.IN_PROGRESS, null));
  }

  @PatchMapping("/{id}/complete")
  @Operation(summary = "Complete activity", description = "Mark activity as completed with optional actual hours")
  public ResponseEntity<CooldownActivityDTO> completeActivity(
      @PathVariable Long id,
      @RequestParam(required = false) Integer actualHours) {
    return ResponseEntity.ok(cooldownActivityService.updateStatus(id, CooldownActivityStatus.COMPLETED, actualHours));
  }

  @PatchMapping("/{id}/assign")
  @Operation(summary = "Assign activity", description = "Assign activity to a user")
  public ResponseEntity<CooldownActivityDTO> assignActivity(
      @PathVariable Long id,
      @RequestParam(required = false) Long userId) {
    return ResponseEntity.ok(cooldownActivityService.assignToUser(id, userId));
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Delete activity", description = "Delete a cooldown activity")
  public ResponseEntity<Void> deleteActivity(@PathVariable Long id) {
    cooldownActivityService.deleteActivity(id);
    return ResponseEntity.noContent().build();
  }

  private Long getUserId(UserDetails userDetails) {
    return userRepository.findByUsername(userDetails.getUsername())
        .map(User::getId)
        .orElseThrow(() -> new RuntimeException("User not found"));
  }
}
