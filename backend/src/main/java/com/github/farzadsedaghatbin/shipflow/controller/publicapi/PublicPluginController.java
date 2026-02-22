package com.github.farzadsedaghatbin.shipflow.controller.publicapi;

import com.github.farzadsedaghatbin.shipflow.plugin.PluginRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/public/plugins")
@RequiredArgsConstructor
@Tag(name = "Public API – Plugins", description = "Query registered plugins")
public class PublicPluginController {

  private final PluginRegistry pluginRegistry;

  @GetMapping
  @Operation(summary = "List all registered plugins")
  public ResponseEntity<Map<String, Object>> listPlugins() {
    return ResponseEntity.ok(pluginRegistry.getSummary());
  }
}
