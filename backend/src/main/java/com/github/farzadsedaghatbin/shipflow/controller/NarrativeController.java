package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.narrative.*;
import com.github.farzadsedaghatbin.shipflow.entity.enums.NarrativeType;
import com.github.farzadsedaghatbin.shipflow.service.CycleNarrativeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for cycle narrative summaries.
 * Part of v0.5 "Insight, Not Metrics" feature set.
 */
@RestController
@RequestMapping("/api/narratives")
@RequiredArgsConstructor
@Tag(name = "Narratives", description = "Cycle narrative summaries and exports")
public class NarrativeController {

  private final CycleNarrativeService narrativeService;

  @GetMapping("/cycle/{cycleId}/summary")
  @PreAuthorize("@permissionService.hasPermission('REPORT', 'READ')")
  @Operation(summary = "Get complete cycle summary",
      description = "Returns all narrative sections: what we bet, shipped, cut, and surprises")
  public ResponseEntity<CycleSummaryDTO> getCycleSummary(@PathVariable Long cycleId) {
    return ResponseEntity.ok(narrativeService.getCycleSummary(cycleId));
  }

  @GetMapping("/cycle/{cycleId}/{type}")
  @PreAuthorize("@permissionService.hasPermission('REPORT', 'READ')")
  @Operation(summary = "Get specific narrative section",
      description = "Returns a single narrative section (WHAT_WE_BET, WHAT_SHIPPED, WHAT_WE_CUT, SURPRISES, FULL_SUMMARY)")
  public ResponseEntity<CycleNarrativeDTO> getNarrative(
      @PathVariable Long cycleId,
      @PathVariable NarrativeType type) {
    return ResponseEntity.ok(narrativeService.getOrGenerateNarrative(cycleId, type, false));
  }

  @PostMapping("/cycle/{cycleId}/regenerate")
  @PreAuthorize("@permissionService.hasPermission('REPORT', 'CREATE')")
  @Operation(summary = "Regenerate all narratives for a cycle",
      description = "Forces regeneration of all narrative sections using AI if available")
  public ResponseEntity<CycleSummaryDTO> regenerateNarratives(@PathVariable Long cycleId) {
    return ResponseEntity.ok(narrativeService.regenerateAllNarratives(cycleId));
  }

  @PostMapping("/cycle/{cycleId}/{type}/regenerate")
  @PreAuthorize("@permissionService.hasPermission('REPORT', 'CREATE')")
  @Operation(summary = "Regenerate specific narrative section",
      description = "Forces regeneration of a single narrative section")
  public ResponseEntity<CycleNarrativeDTO> regenerateNarrative(
      @PathVariable Long cycleId,
      @PathVariable NarrativeType type) {
    return ResponseEntity.ok(narrativeService.generateNarrative(cycleId, type));
  }

  @GetMapping("/cycle/{cycleId}/export/markdown")
  @PreAuthorize("@permissionService.hasPermission('REPORT', 'READ')")
  @Operation(summary = "Export cycle summary as Markdown",
      description = "Exports the complete cycle summary in Markdown format")
  public ResponseEntity<String> exportAsMarkdown(@PathVariable Long cycleId) {
    String markdown = narrativeService.exportAsMarkdown(cycleId);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=cycle_summary_" + cycleId + ".md")
        .contentType(MediaType.parseMediaType("text/markdown"))
        .body(markdown);
  }

  @PostMapping("/cycle/{cycleId}/retrospective/summarize")
  @PreAuthorize("@permissionService.hasPermission('REPORT', 'CREATE')")
  @Operation(summary = "Generate AI retrospective summary",
      description = "Generates (or regenerates) an AI summary of the retro board entries for a cycle")
  public ResponseEntity<CycleNarrativeDTO> generateRetroSummary(@PathVariable Long cycleId) {
    return ResponseEntity.ok(narrativeService.generateRetroSummary(cycleId));
  }

  @GetMapping("/cycle/{cycleId}/retrospective/summary")
  @PreAuthorize("@permissionService.hasPermission('REPORT', 'READ')")
  @Operation(summary = "Get existing retrospective summary",
      description = "Returns the stored retro summary if one has been generated, or 204 No Content if none")
  public ResponseEntity<CycleNarrativeDTO> getRetroSummary(@PathVariable Long cycleId) {
    Optional<CycleNarrativeDTO> summary = narrativeService.getRetroSummary(cycleId);
    return summary
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.noContent().build());
  }
}
