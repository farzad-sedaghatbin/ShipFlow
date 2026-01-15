package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.*;
import com.github.farzadsedaghatbin.shipflow.dto.betting.*;
import com.github.farzadsedaghatbin.shipflow.service.BettingTableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/betting")
@RequiredArgsConstructor
@Tag(name = "Betting Table", description = "Shape Up Betting Table management for assigning shaped pitches to cycle slots")
public class BettingTableController {

    private final BettingTableService bettingTableService;

    @GetMapping("/cycle/{cycleId}")
    @Operation(summary = "Get betting table for a cycle", 
               description = "Returns the full betting table view including shaped pitches, team tracks, and assigned slots")
    public ResponseEntity<BettingTableDTO> getBettingTable(@PathVariable Long cycleId) {
        return ResponseEntity.ok(bettingTableService.getBettingTable(cycleId));
    }

    @GetMapping("/shaped-pitches")
    @Operation(summary = "Get shaped pitches available for betting",
               description = "Returns pitches with SHAPED status that haven't been assigned to any slot yet")
    public ResponseEntity<List<PitchDTO>> getShapedPitches(
            @RequestParam(required = false) Long projectId) {
        return ResponseEntity.ok(bettingTableService.getShapedPitchesForBetting(projectId));
    }

    @PostMapping("/slots")
    @Operation(summary = "Create a new betting slot",
               description = "Creates a new slot for a team in a cycle. Can optionally assign a pitch immediately.")
    public ResponseEntity<BettingSlotDTO> createSlot(@Valid @RequestBody CreateBettingSlotRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bettingTableService.createSlot(request));
    }

    @PostMapping("/cycle/{cycleId}/generate-slots")
    @Operation(summary = "Auto-generate slots for all teams in a cycle",
               description = "Creates default slots spanning the entire cycle for each team that doesn't have slots yet")
    public ResponseEntity<List<BettingSlotDTO>> generateSlots(@PathVariable Long cycleId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bettingTableService.generateSlotsForCycle(cycleId));
    }

    @PatchMapping("/slots/{slotId}/assign/{pitchId}")
    @Operation(summary = "Assign a pitch to a slot",
               description = "The main betting action - assigns a shaped pitch to a team's slot. " +
                           "Validates that the pitch fits within the slot's duration.")
    public ResponseEntity<BettingSlotDTO> assignPitchToSlot(
            @PathVariable Long slotId, 
            @PathVariable Long pitchId) {
        return ResponseEntity.ok(bettingTableService.assignPitchToSlot(slotId, pitchId));
    }

    @PatchMapping("/slots/{slotId}/unassign")
    @Operation(summary = "Remove pitch from a slot",
               description = "Unassigns the pitch from the slot, returning it to the shaped pitches pool")
    public ResponseEntity<BettingSlotDTO> removePitchFromSlot(@PathVariable Long slotId) {
        return ResponseEntity.ok(bettingTableService.removePitchFromSlot(slotId));
    }

    @PatchMapping("/slots/{slotId}/dates")
    @Operation(summary = "Update slot dates",
               description = "Resize a slot by changing its start and end dates. " +
                           "Validates that any assigned pitch still fits.")
    public ResponseEntity<BettingSlotDTO> updateSlotDates(
            @PathVariable Long slotId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(bettingTableService.updateSlotDates(slotId, startDate, endDate));
    }

    @DeleteMapping("/slots/{slotId}")
    @Operation(summary = "Delete a betting slot",
               description = "Removes the slot. If a pitch is assigned, it will be returned to SHAPED status.")
    public ResponseEntity<Void> deleteSlot(@PathVariable Long slotId) {
        bettingTableService.deleteSlot(slotId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/slots/{slotId}/can-fit/{pitchId}")
    @Operation(summary = "Check if pitch fits in slot",
               description = "Validates if a pitch's appetite fits within the slot's duration. " +
                           "Useful for drag-and-drop preview validation.")
    public ResponseEntity<Boolean> canPitchFitInSlot(
            @PathVariable Long slotId, 
            @PathVariable Long pitchId) {
        return ResponseEntity.ok(bettingTableService.canPitchFitInSlot(pitchId, slotId));
    }

    // === NEW: Pitch Comparison and Analytics Endpoints ===

    @GetMapping("/team/{teamId}/performance-history")
    @Operation(summary = "Get team performance history",
               description = "Returns historical betting success rates and performance metrics for a team")
    public ResponseEntity<TeamPerformanceHistoryDTO> getTeamPerformanceHistory(@PathVariable Long teamId) {
        return ResponseEntity.ok(bettingTableService.getTeamPerformanceHistory(teamId));
    }

    @GetMapping("/cycle/{cycleId}/pitch-comparisons")
    @Operation(summary = "Get pitch comparison analysis",
               description = "Returns shaped pitches with team fit analysis and capacity warnings for betting meetings")
    public ResponseEntity<List<PitchComparisonDTO>> getPitchComparisons(@PathVariable Long cycleId) {
        return ResponseEntity.ok(bettingTableService.getPitchComparisons(cycleId));
    }

    @GetMapping("/cycle/{cycleId}/capacity-analysis")
    @Operation(summary = "Get capacity analysis",
               description = "Returns overall capacity utilization and warnings for over/under-allocation")
    public ResponseEntity<CapacityAnalysisDTO> getCapacityAnalysis(@PathVariable Long cycleId) {
        return ResponseEntity.ok(bettingTableService.getCapacityAnalysis(cycleId));
    }
}
