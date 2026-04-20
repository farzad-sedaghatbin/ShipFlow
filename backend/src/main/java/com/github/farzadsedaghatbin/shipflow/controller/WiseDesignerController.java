package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.wisedesigner.*;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import com.github.farzadsedaghatbin.shipflow.service.wisedesigner.WiseDesignerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for the Wise Designer feature.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>{@code POST /api/wise-designer/generate} — generate a UI prototype from a pitch</li>
 *   <li>{@code POST /api/wise-designer/iterate} — refine an existing prototype</li>
 *   <li>{@code GET  /api/wise-designer/pitch/{pitchId}} — list all artifacts for a pitch</li>
 *   <li>{@code GET  /api/wise-designer/artifacts/{id}} — fetch a specific artifact (with HTML)</li>
 *   <li>{@code GET  /api/wise-designer/status} — check if the feature is enabled</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/wise-designer")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Wise Designer", description = "AI-powered UI prototype generator for pitches (Experimental)")
public class WiseDesignerController {

  private final WiseDesignerService wiseDesignerService;
  private final UserRepository userRepository;

  // ──────────────────────────────────────────────────────────────────────────

  @PostMapping("/generate")
  @Operation(
      summary = "Generate UI prototype",
      description = "Generates a self-contained HTML UI prototype from the pitch context. "
          + "Optionally pass a wiseArchSessionId to align the design with the planned architecture.")
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DEVELOPER', 'PRODUCT')")
  public ResponseEntity<WiseDesignerResponseDTO> generate(
      @AuthenticationPrincipal UserDetails userDetails,
      @Valid @RequestBody WiseDesignerRequestDTO request) {

    log.info("Generating UI prototype for pitch {}", request.getPitchId());
    Long userId = getUserId(userDetails);
    return ResponseEntity.ok(wiseDesignerService.generate(request, userId));
  }

  @PostMapping("/iterate")
  @Operation(
      summary = "Iterate on an existing prototype",
      description = "Applies a natural-language instruction to an existing artifact and saves "
          + "the result as a new artifact (history preserved).")
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DEVELOPER', 'PRODUCT')")
  public ResponseEntity<WiseDesignerResponseDTO> iterate(
      @AuthenticationPrincipal UserDetails userDetails,
      @Valid @RequestBody WiseDesignerIterateRequestDTO request) {

    log.info("Iterating Wise Designer artifact {}: {}", request.getArtifactId(), request.getInstruction());
    Long userId = getUserId(userDetails);
    return ResponseEntity.ok(wiseDesignerService.iterate(request, userId));
  }

  @GetMapping("/pitch/{pitchId}")
  @Operation(
      summary = "List artifacts for a pitch",
      description = "Returns all design artifacts for the pitch, newest first. Does not include htmlContent.")
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DEVELOPER', 'PRODUCT', 'VIEWER')")
  public ResponseEntity<List<WiseDesignerArtifactSummaryDTO>> getArtifactsForPitch(
      @PathVariable Long pitchId) {
    return ResponseEntity.ok(wiseDesignerService.getArtifactsForPitch(pitchId));
  }

  @GetMapping("/artifacts/{artifactId}")
  @Operation(
      summary = "Get a specific artifact",
      description = "Returns the full artifact including htmlContent.")
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DEVELOPER', 'PRODUCT', 'VIEWER')")
  public ResponseEntity<WiseDesignerResponseDTO> getArtifact(@PathVariable Long artifactId) {
    return ResponseEntity.ok(wiseDesignerService.getArtifact(artifactId));
  }

  @GetMapping("/status")
  @Operation(summary = "Check if Wise Designer is enabled")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<Map<String, Object>> getStatus() {
    try {
      wiseDesignerService.checkFeatureEnabled();
      return ResponseEntity.ok(Map.of("enabled", true, "message", "Wise Designer is enabled"));
    } catch (com.github.farzadsedaghatbin.shipflow.exception.FeatureDisabledException e) {
      return ResponseEntity.ok(Map.of("enabled", false, "message", e.getMessage()));
    }
  }

  // ──────────────────────────────────────────────────────────────────────────

  private Long getUserId(UserDetails userDetails) {
    return userRepository.findByUsername(userDetails.getUsername())
        .orElseThrow(() -> new RuntimeException("User not found: " + userDetails.getUsername()))
        .getId();
  }
}
