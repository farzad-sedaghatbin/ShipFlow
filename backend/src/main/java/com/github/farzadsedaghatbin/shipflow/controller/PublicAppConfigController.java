package com.github.farzadsedaghatbin.shipflow.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tiny anonymous config surface for the pre-login page — flags the frontend needs before a user
 * has authenticated. Mounted under {@code /api/public/**}, already {@code permitAll} in {@link
 * com.github.farzadsedaghatbin.shipflow.security.SecurityConfig}. Deliberately separate from
 * {@code /api/v1/public/**} (the external integration API, API-key-authenticated and
 * project-scoped) — this is a different concern with a different, much smaller trust boundary:
 * a couple of non-sensitive booleans, not organizational data.
 */
@RestController
@RequestMapping("/api/public")
@Tag(name = "Public App Config", description = "Anonymous config flags for the pre-login page")
public class PublicAppConfigController {

  @Value("${app.demo-mode.enabled:false}")
  private boolean demoModeEnabled;

  @GetMapping("/config")
  @Operation(summary = "Get anonymous, pre-login app config flags")
  public ResponseEntity<Map<String, Object>> getPublicConfig() {
    return ResponseEntity.ok(Map.of("demoModeEnabled", demoModeEnabled));
  }
}
