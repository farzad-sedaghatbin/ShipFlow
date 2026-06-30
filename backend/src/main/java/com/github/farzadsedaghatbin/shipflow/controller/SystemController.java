package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.system.AirGappedStatusDTO;
import com.github.farzadsedaghatbin.shipflow.service.SystemStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only system-status endpoints. Authenticated users may read these so the
 * UI can surface deployment state (e.g. the air-gapped indicator).
 */
@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
@Tag(name = "System", description = "Read-only system status (v1.9.0)")
public class SystemController {

  private final SystemStatusService systemStatusService;

  @GetMapping("/air-gapped")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Air-gapped mode status",
      description = "Returns whether air-gapped AI mode is enabled, the active local provider, "
          + "Ollama reachability, and any active external MCP clients.")
  public ResponseEntity<AirGappedStatusDTO> getAirGappedStatus() {
    return ResponseEntity.ok(systemStatusService.getAirGappedStatus());
  }
}
