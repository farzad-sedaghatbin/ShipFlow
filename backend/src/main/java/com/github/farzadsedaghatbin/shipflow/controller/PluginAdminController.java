package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.plugin.PluginDTO;
import com.github.farzadsedaghatbin.shipflow.dto.plugin.PluginToggleRequest;
import com.github.farzadsedaghatbin.shipflow.plugin.PluginRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/plugins")
@RequiredArgsConstructor
@Tag(name = "Admin – Plugins", description = "List and toggle registered plugin extensions")
public class PluginAdminController {

  private final PluginRegistry pluginRegistry;

  @GetMapping
  @PreAuthorize("hasAuthority('ROLE_ADMIN')")
  @Operation(summary = "List all registered plugins with enabled status")
  public ResponseEntity<List<PluginDTO>> listPlugins() {
    return ResponseEntity.ok(pluginRegistry.getAllPluginDTOs());
  }

  @PatchMapping("/{pluginId}/enabled")
  @PreAuthorize("hasAuthority('ROLE_ADMIN')")
  @Operation(summary = "Enable or disable a plugin at runtime")
  public ResponseEntity<PluginDTO> togglePlugin(
      @PathVariable String pluginId,
      @RequestBody PluginToggleRequest request) {

    String displayName = pluginRegistry.findDisplayName(pluginId);
    if (displayName == null) {
      return ResponseEntity.notFound().build();
    }
    pluginRegistry.setEnabled(pluginId, request.isEnabled());
    // Determine plugin type for the response
    List<PluginDTO> all = pluginRegistry.getAllPluginDTOs();
    PluginDTO updated = all.stream()
        .filter(p -> p.getPluginId().equals(pluginId))
        .findFirst()
        .orElse(null);
    return updated != null ? ResponseEntity.ok(updated) : ResponseEntity.notFound().build();
  }
}
