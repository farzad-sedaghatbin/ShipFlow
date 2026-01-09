package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.CreateMeetingRequest;
import com.github.farzadsedaghatbin.shipflow.dto.MeetingDTO;
import com.github.farzadsedaghatbin.shipflow.entity.enums.MeetingType;
import com.github.farzadsedaghatbin.shipflow.service.MeetingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/meetings")
@RequiredArgsConstructor
@Tag(name = "Meetings", description = "Meeting management")
public class MeetingController {

    private final MeetingService meetingService;

    @GetMapping
    @Operation(summary = "Get all meetings")
    public ResponseEntity<List<MeetingDTO>> getAllMeetings() {
        return ResponseEntity.ok(meetingService.getAllMeetings());
    }

    @GetMapping("/pitch/{pitchId}")
    @Operation(summary = "Get meetings by pitch ID")
    public ResponseEntity<List<MeetingDTO>> getMeetingsByPitchId(@PathVariable Long pitchId) {
        return ResponseEntity.ok(meetingService.getMeetingsByPitchId(pitchId));
    }

    @GetMapping("/type/{type}")
    @Operation(summary = "Get meetings by type")
    public ResponseEntity<List<MeetingDTO>> getMeetingsByType(@PathVariable MeetingType type) {
        return ResponseEntity.ok(meetingService.getMeetingsByType(type));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get meeting by ID")
    public ResponseEntity<MeetingDTO> getMeetingById(@PathVariable Long id) {
        return ResponseEntity.ok(meetingService.getMeetingById(id));
    }

    @PostMapping
    @Operation(summary = "Create a new meeting")
    public ResponseEntity<MeetingDTO> createMeeting(@Valid @RequestBody CreateMeetingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(meetingService.createMeeting(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a meeting")
    public ResponseEntity<MeetingDTO> updateMeeting(@PathVariable Long id, @Valid @RequestBody CreateMeetingRequest request) {
        return ResponseEntity.ok(meetingService.updateMeeting(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a meeting")
    public ResponseEntity<Void> deleteMeeting(@PathVariable Long id) {
        meetingService.deleteMeeting(id);
        return ResponseEntity.noContent().build();
    }
}
