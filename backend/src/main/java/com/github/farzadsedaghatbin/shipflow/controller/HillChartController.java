package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.CreateHillChartPointRequest;
import com.github.farzadsedaghatbin.shipflow.dto.HillChartPointDTO;
import com.github.farzadsedaghatbin.shipflow.dto.UpdateHillChartPointRequest;
import com.github.farzadsedaghatbin.shipflow.dto.hillchart.ConfidenceAnalysisDTO;
import com.github.farzadsedaghatbin.shipflow.dto.hillchart.HillChartHistoryDTO;
import com.github.farzadsedaghatbin.shipflow.dto.hillchart.UpdateHillChartPositionRequest;
import com.github.farzadsedaghatbin.shipflow.service.HillChartService;
import com.github.farzadsedaghatbin.shipflow.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hill-chart")
@RequiredArgsConstructor
@Tag(name = "Hill Chart", description = "Hill Chart management APIs with drag-drop support and confidence tracking")
@SecurityRequirement(name = "bearer-jwt")
public class HillChartController {

    private final HillChartService hillChartService;
    private final UserService userService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all hill chart points")
    public ResponseEntity<List<HillChartPointDTO>> getAllHillChartPoints() {
        return ResponseEntity.ok(hillChartService.getAllHillChartPoints());
    }

    @GetMapping("/pitch/{pitchId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get hill chart points by pitch")
    public ResponseEntity<List<HillChartPointDTO>> getHillChartPointsByPitch(@PathVariable Long pitchId) {
        return ResponseEntity.ok(hillChartService.getHillChartPointsByPitch(pitchId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get hill chart point by ID")
    public ResponseEntity<HillChartPointDTO> getHillChartPointById(@PathVariable Long id) {
        return ResponseEntity.ok(hillChartService.getHillChartPointById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER', 'PRODUCT')")
    @Operation(summary = "Create a new hill chart point")
    public ResponseEntity<HillChartPointDTO> createHillChartPoint(@Valid @RequestBody CreateHillChartPointRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(hillChartService.createHillChartPoint(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER', 'PRODUCT')")
    @Operation(summary = "Update a hill chart point")
    public ResponseEntity<HillChartPointDTO> updateHillChartPoint(
            @PathVariable Long id,
            @Valid @RequestBody UpdateHillChartPointRequest request) {
        return ResponseEntity.ok(hillChartService.updateHillChartPoint(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    @Operation(summary = "Delete a hill chart point")
    public ResponseEntity<Void> deleteHillChartPoint(@PathVariable Long id) {
        hillChartService.deleteHillChartPoint(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/position")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update hill chart point position with drag-drop", 
               description = "Update position with confidence tracking for analyzing team accuracy")
    public ResponseEntity<HillChartPointDTO> updatePosition(
            @PathVariable Long id,
            @Valid @RequestBody UpdateHillChartPositionRequest request) {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(hillChartService.updatePositionWithHistory(id, request, userId));
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get position history for a point", 
               description = "Get all position changes for a hill chart point")
    public ResponseEntity<List<HillChartHistoryDTO>> getPointHistory(@PathVariable Long id) {
        return ResponseEntity.ok(hillChartService.getPointHistory(id));
    }

    @GetMapping("/pitch/{pitchId}/history")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get position history for a pitch", 
               description = "Get all position changes for all points in a pitch")
    public ResponseEntity<List<HillChartHistoryDTO>> getPitchHistory(@PathVariable Long pitchId) {
        return ResponseEntity.ok(hillChartService.getPitchHistory(pitchId));
    }

    @GetMapping("/pitch/{pitchId}/confidence")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get confidence analysis for a pitch", 
               description = "Compare team confidence vs actual progress based on hill chart moves")
    public ResponseEntity<ConfidenceAnalysisDTO> getConfidenceAnalysis(@PathVariable Long pitchId) {
        return ResponseEntity.ok(hillChartService.getConfidenceAnalysis(pitchId));
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            try {
                return userService.findByUsername(username).getId();
            } catch (Exception e) {
                return null; // Allow anonymous for simplicity
            }
        }
        return null;
    }
}
