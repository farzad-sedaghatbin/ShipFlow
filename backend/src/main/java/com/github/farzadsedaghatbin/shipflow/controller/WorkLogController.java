package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.CreateWorkLogForSelfRequest;
import com.github.farzadsedaghatbin.shipflow.dto.CreateWorkLogRequest;
import com.github.farzadsedaghatbin.shipflow.dto.WorkLogDTO;
import com.github.farzadsedaghatbin.shipflow.service.WorkLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/worklogs")
@RequiredArgsConstructor
@Tag(name = "Work Logs", description = "Work log management APIs for tracking time spent on pitches and tasks")
public class WorkLogController {

  private final WorkLogService workLogService;

  private static final Sort DATE_DESC = Sort.by(Sort.Direction.DESC, "date")
      .and(Sort.by(Sort.Direction.DESC, "id"));

  private Pageable pageOf(int page, int size) {
    return PageRequest.of(page, size, DATE_DESC);
  }

  // ========== Current User's Own Work Logs ==========

  @GetMapping("/my")
  @Operation(summary = "Get current user's work logs", description = "Returns all work logs created by the currently authenticated user. User must be linked to a Person profile.")
  @ApiResponses({@ApiResponse(responseCode = "200", description = "Work logs retrieved successfully"),
      @ApiResponse(responseCode = "401", description = "User not authenticated"),
      @ApiResponse(responseCode = "400", description = "User not linked to a person profile")})
  public ResponseEntity<Page<WorkLogDTO>> getMyWorkLogs(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) Long projectId) {
    if (projectId != null) {
      return ResponseEntity.ok(workLogService.getMyWorkLogsByProjectId(projectId, pageOf(page, size)));
    }
    return ResponseEntity.ok(workLogService.getMyWorkLogs(pageOf(page, size)));
  }

  @GetMapping("/my/cycle/{cycleId}")
  @Operation(summary = "Get current user's work logs by cycle", description = "Returns work logs for the current user filtered by cycle ID")
  public ResponseEntity<Page<WorkLogDTO>> getMyWorkLogsByCycle(@PathVariable Long cycleId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return ResponseEntity.ok(workLogService.getMyWorkLogsByCycle(cycleId, pageOf(page, size)));
  }

  @GetMapping("/my/date/{date}")
  @Operation(summary = "Get current user's work logs by date", description = "Returns work logs for the current user on a specific date")
  public ResponseEntity<Page<WorkLogDTO>> getMyWorkLogsByDate(
      @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return ResponseEntity.ok(workLogService.getMyWorkLogsByDate(date, pageOf(page, size)));
  }

  @PostMapping("/my")
  @Operation(summary = "Create a work log for yourself", description = "Creates a new work log for the currently authenticated user. The personId is automatically set to the user's linked Person profile.")
  @ApiResponses({@ApiResponse(responseCode = "201", description = "Work log created successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid data or user not linked to a person profile"),
      @ApiResponse(responseCode = "401", description = "User not authenticated")})
  public ResponseEntity<WorkLogDTO> createMyWorkLog(@Valid @RequestBody CreateWorkLogForSelfRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(workLogService.createMyWorkLog(request));
  }

  @PutMapping("/my/{id}")
  @Operation(summary = "Update your own work log", description = "Updates an existing work log. Only the owner can update their own work logs.")
  @ApiResponses({@ApiResponse(responseCode = "200", description = "Work log updated successfully"),
      @ApiResponse(responseCode = "400", description = "Work log not found or not owned by current user"),
      @ApiResponse(responseCode = "401", description = "User not authenticated")})
  public ResponseEntity<WorkLogDTO> updateMyWorkLog(@PathVariable Long id,
      @Valid @RequestBody CreateWorkLogForSelfRequest request) {
    return ResponseEntity.ok(workLogService.updateMyWorkLog(id, request));
  }

  @DeleteMapping("/my/{id}")
  @Operation(summary = "Delete your own work log", description = "Deletes an existing work log. Only the owner can delete their own work logs.")
  @ApiResponses({@ApiResponse(responseCode = "204", description = "Work log deleted successfully"),
      @ApiResponse(responseCode = "400", description = "Work log not found or not owned by current user"),
      @ApiResponse(responseCode = "401", description = "User not authenticated")})
  public ResponseEntity<Void> deleteMyWorkLog(@PathVariable Long id) {
    workLogService.deleteMyWorkLog(id);
    return ResponseEntity.noContent().build();
  }

  // ========== Admin/Manager Work Log Management ==========

  @GetMapping
  @PreAuthorize("@permissionService.hasPermission('PITCH', 'READ')")
  @Operation(summary = "Get all work logs", description = "Returns all work logs in the system (admin/manager use)")
  public ResponseEntity<Page<WorkLogDTO>> getAllWorkLogs(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) Long projectId) {
    if (projectId != null) {
      return ResponseEntity.ok(workLogService.getAllWorkLogsByProjectId(projectId, pageOf(page, size)));
    }
    return ResponseEntity.ok(workLogService.getAllWorkLogs(pageOf(page, size)));
  }

  @GetMapping("/pitch/{pitchId}")
  @PreAuthorize("@permissionService.hasPermission('PITCH', 'READ')")
  @Operation(summary = "Get work logs by pitch ID")
  public ResponseEntity<Page<WorkLogDTO>> getWorkLogsByPitchId(@PathVariable Long pitchId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return ResponseEntity.ok(workLogService.getWorkLogsByPitchId(pitchId, pageOf(page, size)));
  }

  @GetMapping("/task/{taskId}")
  @PreAuthorize("@permissionService.hasPermission('PITCH', 'READ')")
  @Operation(summary = "Get work logs by task ID")
  public ResponseEntity<Page<WorkLogDTO>> getWorkLogsByTaskId(@PathVariable Long taskId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return ResponseEntity.ok(workLogService.getWorkLogsByTaskId(taskId, pageOf(page, size)));
  }

  @GetMapping("/person/{personId}")
  @PreAuthorize("@permissionService.hasPermission('PITCH', 'READ')")
  @Operation(summary = "Get work logs by person ID")
  public ResponseEntity<Page<WorkLogDTO>> getWorkLogsByPersonId(@PathVariable Long personId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return ResponseEntity.ok(workLogService.getWorkLogsByPersonId(personId, pageOf(page, size)));
  }

  @GetMapping("/person/{personId}/date/{date}")
  @PreAuthorize("@permissionService.hasPermission('PITCH', 'READ')")
  @Operation(summary = "Get work logs by person and date")
  public ResponseEntity<Page<WorkLogDTO>> getWorkLogsByPersonAndDate(@PathVariable Long personId,
      @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return ResponseEntity.ok(workLogService.getWorkLogsByPersonAndDate(personId, date, pageOf(page, size)));
  }

  @GetMapping("/cycle/{cycleId}")
  @PreAuthorize("@permissionService.hasPermission('PITCH', 'READ')")
  @Operation(summary = "Get work logs by cycle ID")
  public ResponseEntity<Page<WorkLogDTO>> getWorkLogsByCycleId(@PathVariable Long cycleId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return ResponseEntity.ok(workLogService.getWorkLogsByCycleId(cycleId, pageOf(page, size)));
  }

  @GetMapping("/{id}")
  @PreAuthorize("@permissionService.hasPermission('PITCH', 'READ')")
  @Operation(summary = "Get work log by ID")
  public ResponseEntity<WorkLogDTO> getWorkLogById(@PathVariable Long id) {
    return ResponseEntity.ok(workLogService.getWorkLogById(id));
  }

  @PostMapping
  @PreAuthorize("@permissionService.hasPermission('PITCH', 'UPDATE')")
  @Operation(summary = "Create a new work log")
  public ResponseEntity<WorkLogDTO> createWorkLog(@Valid @RequestBody CreateWorkLogRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(workLogService.createWorkLog(request));
  }

  @PutMapping("/{id}")
  @PreAuthorize("@permissionService.hasPermission('PITCH', 'UPDATE')")
  @Operation(summary = "Update a work log")
  public ResponseEntity<WorkLogDTO> updateWorkLog(@PathVariable Long id,
      @Valid @RequestBody CreateWorkLogRequest request) {
    return ResponseEntity.ok(workLogService.updateWorkLog(id, request));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("@permissionService.hasPermission('PITCH', 'DELETE')")
  @Operation(summary = "Delete a work log")
  public ResponseEntity<Void> deleteWorkLog(@PathVariable Long id) {
    workLogService.deleteWorkLog(id);
    return ResponseEntity.noContent().build();
  }
}
