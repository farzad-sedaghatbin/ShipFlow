package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.BurndownPointDTO;
import com.github.farzadsedaghatbin.shipflow.service.BurndownService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cycles/{cycleId}/burndown")
@RequiredArgsConstructor
@Tag(name = "Burndown", description = "Scrum sprint burndown chart data")
public class BurndownController {

  private final BurndownService burndownService;

  @GetMapping
  @PreAuthorize(
      "hasAnyRole('ADMIN','PROJECT_MANAGER','DEVELOPER','QA','PRODUCT','VIEWER')")
  @Operation(
      summary = "Get burndown chart data for a sprint",
      description =
          "Returns daily remaining story points and ideal burndown line from sprint start"
              + " to today (capped at sprint end date). Only tasks with story points are"
              + " included.")
  public ResponseEntity<List<BurndownPointDTO>> getBurndown(@PathVariable Long cycleId) {
    return ResponseEntity.ok(burndownService.computeBurndown(cycleId));
  }
}
