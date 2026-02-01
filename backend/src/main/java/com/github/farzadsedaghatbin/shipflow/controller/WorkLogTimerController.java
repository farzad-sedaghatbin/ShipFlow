package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.StartTimerRequest;
import com.github.farzadsedaghatbin.shipflow.dto.StopTimerResponse;
import com.github.farzadsedaghatbin.shipflow.dto.WorkLogTimerDTO;
import com.github.farzadsedaghatbin.shipflow.service.WorkLogTimerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/timers")
@RequiredArgsConstructor
@Tag(name = "Work Log Timers", description = "Timer-based time tracking for pitches and tasks")
public class WorkLogTimerController {

  private final WorkLogTimerService timerService;

  @PostMapping("/start")
  @Operation(
      summary = "Start a new timer",
      description =
          "Starts tracking time for a pitch or task. Only one active timer per user allowed.")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Timer started successfully"),
    @ApiResponse(
        responseCode = "400",
        description = "Invalid request or user already has active timer"),
    @ApiResponse(responseCode = "401", description = "User not authenticated")
  })
  public ResponseEntity<WorkLogTimerDTO> startTimer(@Valid @RequestBody StartTimerRequest request) {
    WorkLogTimerDTO timer = timerService.startTimer(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(timer);
  }

  @PostMapping("/stop")
  @Operation(
      summary = "Stop the active timer",
      description = "Stops the current timer and automatically creates a work log entry")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Timer stopped and work log created"),
    @ApiResponse(responseCode = "400", description = "No active timer found"),
    @ApiResponse(responseCode = "401", description = "User not authenticated")
  })
  public ResponseEntity<StopTimerResponse> stopTimer() {
    StopTimerResponse response = timerService.stopTimer();
    return ResponseEntity.ok(response);
  }

  @GetMapping("/active")
  @Operation(
      summary = "Get active timer",
      description = "Returns the currently running timer for the authenticated user, if any")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Active timer retrieved (or null if no timer)"),
    @ApiResponse(responseCode = "401", description = "User not authenticated")
  })
  public ResponseEntity<WorkLogTimerDTO> getActiveTimer() {
    Optional<WorkLogTimerDTO> timer = timerService.getActiveTimer();
    return ResponseEntity.ok(timer.orElse(null));
  }

  @DeleteMapping("/cancel")
  @Operation(
      summary = "Cancel the active timer",
      description = "Cancels the current timer without creating a work log entry")
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Timer cancelled successfully"),
    @ApiResponse(responseCode = "400", description = "No active timer found"),
    @ApiResponse(responseCode = "401", description = "User not authenticated")
  })
  public ResponseEntity<Void> cancelTimer() {
    timerService.cancelTimer();
    return ResponseEntity.noContent().build();
  }
}
