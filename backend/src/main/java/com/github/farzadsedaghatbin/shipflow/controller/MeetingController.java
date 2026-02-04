package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.CreateMeetingRequest;
import com.github.farzadsedaghatbin.shipflow.dto.MeetingDTO;
import com.github.farzadsedaghatbin.shipflow.entity.enums.MeetingType;
import com.github.farzadsedaghatbin.shipflow.service.MeetingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
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
@RequestMapping("/api/meetings")
@RequiredArgsConstructor
@Tag(name = "Meetings", description = "Meeting management")
public class MeetingController {

  private final MeetingService meetingService;

  @GetMapping
  @PreAuthorize("@permissionService.hasPermission('PITCH', 'READ')")
  @Operation(summary = "Get all meetings (deprecated - use /paginated)")
  public ResponseEntity<List<MeetingDTO>> getAllMeetings() {
    return ResponseEntity.ok(meetingService.getAllMeetings());
  }

  @GetMapping("/paginated")
  @PreAuthorize("@permissionService.hasPermission('PITCH', 'READ')")
  @Operation(summary = "Get all meetings with pagination and sorting")
  public ResponseEntity<Page<MeetingDTO>> getAllMeetingsPaginated(@RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size, @RequestParam(defaultValue = "dateHeld") String sortBy,
      @RequestParam(defaultValue = "desc") String sortOrder) {
    Sort.Direction direction = sortOrder.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
    Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
    return ResponseEntity.ok(meetingService.getAllMeetingsPaginated(pageable));
  }

  @GetMapping("/filter")
  @PreAuthorize("@permissionService.hasPermission('PITCH', 'READ')")
  @Operation(summary = "Get meetings with filters, pagination and sorting", description = "Filter meetings by cycle, project, pitch, types, date range, DOR/DOD status")
  public ResponseEntity<Page<MeetingDTO>> getMeetingsWithFilters(@RequestParam(required = false) Long cycleId,
      @RequestParam(required = false) Long projectId, @RequestParam(required = false) Long pitchId,
      @RequestParam(required = false) List<MeetingType> types,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
      @RequestParam(required = false) Boolean dorReady, @RequestParam(required = false) Boolean dodReady,
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "dateHeld") String sortBy,
      @RequestParam(defaultValue = "desc") String sortOrder) {
    Sort.Direction direction = sortOrder.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
    Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
    return ResponseEntity.ok(meetingService.getMeetingsWithFilters(cycleId, projectId, pitchId, types, startDate,
        endDate, dorReady, dodReady, pageable));
  }

  @GetMapping("/pitch/{pitchId}")
  @PreAuthorize("@permissionService.hasPermission('PITCH', 'READ')")
  @Operation(summary = "Get meetings by pitch ID")
  public ResponseEntity<List<MeetingDTO>> getMeetingsByPitchId(@PathVariable Long pitchId) {
    return ResponseEntity.ok(meetingService.getMeetingsByPitchId(pitchId));
  }

  @GetMapping("/type/{type}")
  @PreAuthorize("@permissionService.hasPermission('PITCH', 'READ')")
  @Operation(summary = "Get meetings by type")
  public ResponseEntity<List<MeetingDTO>> getMeetingsByType(@PathVariable MeetingType type) {
    return ResponseEntity.ok(meetingService.getMeetingsByType(type));
  }

  @GetMapping("/{id}")
  @PreAuthorize("@permissionService.hasPermission('PITCH', 'READ')")
  @Operation(summary = "Get meeting by ID")
  public ResponseEntity<MeetingDTO> getMeetingById(@PathVariable Long id) {
    return ResponseEntity.ok(meetingService.getMeetingById(id));
  }

  @GetMapping("/{id}/view")
  @PreAuthorize("@permissionService.hasPermission('PITCH', 'READ')")
  @Operation(summary = "Get meeting by ID for viewing (shows only completed checklist items)")
  public ResponseEntity<MeetingDTO> getMeetingForView(@PathVariable Long id) {
    return ResponseEntity.ok(meetingService.getMeetingForView(id));
  }

  @PostMapping
  @PreAuthorize("@permissionService.hasPermission('PITCH', 'UPDATE')")
  @Operation(summary = "Create a new meeting")
  public ResponseEntity<MeetingDTO> createMeeting(@Valid @RequestBody CreateMeetingRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(meetingService.createMeeting(request));
  }

  @PutMapping("/{id}")
  @PreAuthorize("@permissionService.hasPermission('PITCH', 'UPDATE')")
  @Operation(summary = "Update a meeting")
  public ResponseEntity<MeetingDTO> updateMeeting(@PathVariable Long id,
      @Valid @RequestBody CreateMeetingRequest request) {
    return ResponseEntity.ok(meetingService.updateMeeting(id, request));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("@permissionService.hasPermission('PITCH', 'DELETE')")
  @Operation(summary = "Delete a meeting")
  public ResponseEntity<Void> deleteMeeting(@PathVariable Long id) {
    meetingService.deleteMeeting(id);
    return ResponseEntity.noContent().build();
  }
}
