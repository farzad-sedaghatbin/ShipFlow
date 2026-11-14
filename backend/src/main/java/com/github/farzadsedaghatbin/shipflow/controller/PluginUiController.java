package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.plugin.PluginMenuItemDTO;
import com.github.farzadsedaghatbin.shipflow.dto.plugin.PluginPageDTO;
import com.github.farzadsedaghatbin.shipflow.plugin.PluginUiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/plugins/ui")
@RequiredArgsConstructor
@Tag(name = "Plugins – UI", description = "Menu entries and pages contributed by UI plugins")
public class PluginUiController {

  private final PluginUiService pluginUiService;

  @GetMapping("/menu")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Sidebar entries from enabled UI plugins that the caller's role may see")
  public ResponseEntity<List<PluginMenuItemDTO>> menu(Authentication authentication) {
    return ResponseEntity.ok(pluginUiService.menuFor(authentication.getName()));
  }

  @GetMapping("/{pluginId}/pages/{pageKey}")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "A UI plugin page as validated data blocks")
  public ResponseEntity<PluginPageDTO> page(
      Authentication authentication,
      @PathVariable String pluginId,
      @PathVariable String pageKey,
      @RequestParam(required = false) Long projectId) {
    return ResponseEntity.ok(
        pluginUiService.pageFor(authentication.getName(), pluginId, pageKey, projectId));
  }
}
