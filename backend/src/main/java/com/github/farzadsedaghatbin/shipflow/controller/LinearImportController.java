package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.ImportJobDTO;
import com.github.farzadsedaghatbin.shipflow.dto.LinearConnectionStatusDTO;
import com.github.farzadsedaghatbin.shipflow.dto.LinearImportRequest;
import com.github.farzadsedaghatbin.shipflow.dto.LinearTeamDTO;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import com.github.farzadsedaghatbin.shipflow.service.LinearApiImportService;
import com.github.farzadsedaghatbin.shipflow.service.LinearOAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * REST controller for Linear API import — OAuth flow and issue import (v1.2.0 S29).
 *
 * <p>Base path: {@code /api/import/linear}
 */
@RestController
@RequestMapping("/api/import/linear")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Linear Import", description = "Linear API OAuth flow and issue import (v1.2.0)")
public class LinearImportController {

  private final LinearOAuthService linearOAuthService;
  private final LinearApiImportService linearApiImportService;
  private final UserRepository userRepository;

  // ── Status ────────────────────────────────────────────────────────────────

  @GetMapping("/status")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Get Linear connection status")
  public ResponseEntity<LinearConnectionStatusDTO> getStatus() {
    return ResponseEntity.ok(linearOAuthService.getConnectionStatus());
  }

  // ── OAuth flow ─────────────────────────────────────────────────────────────

  @PostMapping("/authorize")
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  @Operation(
      summary = "Begin Linear OAuth flow",
      description =
          "Returns an authorization URL that the user must visit to grant ShipFlow read access "
              + "to their Linear workspace. The base URL is auto-detected from the request if not "
              + "supplied in the body.")
  public ResponseEntity<Map<String, String>> authorize(
      @RequestBody(required = false) Map<String, String> body, HttpServletRequest httpRequest) {

    if (!linearOAuthService.isConfigured()) {
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
          .body(
              Map.of(
                  "error",
                  "Linear OAuth is not configured. Set LINEAR_OAUTH_CLIENT_ID and "
                      + "LINEAR_OAUTH_CLIENT_SECRET environment variables."));
    }

    // Resolve base URL — proxy-aware, same pattern as GitHubAppOAuthController
    String baseUrl = body != null ? body.getOrDefault("baseUrl", "") : "";
    if (baseUrl.isBlank()) {
      String scheme = httpRequest.getScheme();
      String serverName = httpRequest.getServerName();
      int serverPort = httpRequest.getServerPort();

      String forwardedProto = httpRequest.getHeader("X-Forwarded-Proto");
      String forwardedHost = httpRequest.getHeader("X-Forwarded-Host");

      if (forwardedProto != null) scheme = forwardedProto;
      if (forwardedHost != null) serverName = forwardedHost.split(":")[0];

      StringBuilder sb = new StringBuilder(scheme).append("://").append(serverName);
      if (("http".equals(scheme) && serverPort != 80)
          || ("https".equals(scheme) && serverPort != 443)) {
        sb.append(":").append(serverPort);
      }
      baseUrl = sb.toString();
    }

    String authorizationUrl = linearOAuthService.generateAuthorizationUrl(baseUrl);
    return ResponseEntity.ok(Map.of("authorizationUrl", authorizationUrl));
  }

  @GetMapping("/callback")
  @Operation(
      summary = "Handle Linear OAuth callback",
      description =
          "Receives the authorization code from Linear after user consent, exchanges it for an "
              + "access token, and redirects to the frontend import page.")
  public void callback(
      @RequestParam(value = "code", required = false) String code,
      @RequestParam(value = "state", required = false) String state,
      @RequestParam(value = "error", required = false) String error,
      HttpServletResponse response)
      throws IOException {

    if (error != null) {
      log.warn("Linear OAuth callback received error: {}", error);
      response.sendRedirect("/import?tab=linear&linear_error=" + error);
      return;
    }

    if (code == null || state == null) {
      log.warn("Linear OAuth callback: missing code or state");
      response.sendRedirect("/import?tab=linear&linear_error=missing_params");
      return;
    }

    String redirectUrl = linearOAuthService.processCallback(code, state);
    response.sendRedirect(redirectUrl);
  }

  // ── Team management ───────────────────────────────────────────────────────

  @GetMapping("/teams")
  @PreAuthorize("isAuthenticated()")
  @Operation(
      summary = "List available Linear teams",
      description = "Queries the Linear API for all teams accessible by the stored access token.")
  public ResponseEntity<List<LinearTeamDTO>> listTeams() {
    String accessToken = linearOAuthService.getStoredAccessToken();
    if (accessToken == null || accessToken.isBlank()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Linear is not connected. Complete OAuth flow first.");
    }
    List<LinearTeamDTO> teams = linearApiImportService.fetchTeams(accessToken);
    return ResponseEntity.ok(teams);
  }

  @PostMapping("/team")
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  @Operation(summary = "Save selected Linear team", description = "Persists the selected team ID and name for future import runs.")
  public ResponseEntity<Void> saveTeam(@RequestBody Map<String, String> body) {
    String teamId = body.get("teamId");
    String teamName = body.get("teamName");
    if (teamId == null || teamId.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "teamId is required");
    }
    linearOAuthService.saveSelectedTeam(teamId, teamName != null ? teamName : "");
    return ResponseEntity.ok().build();
  }

  // ── Disconnect ─────────────────────────────────────────────────────────────

  @DeleteMapping("/disconnect")
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  @Operation(summary = "Disconnect Linear integration", description = "Removes the stored Linear access token and clears the selected team.")
  public ResponseEntity<Void> disconnect() {
    linearOAuthService.disconnect();
    return ResponseEntity.ok().build();
  }

  // ── Import ─────────────────────────────────────────────────────────────────

  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  @Operation(
      summary = "Import issues from Linear",
      description =
          "Fetches cycles, projects, and issues from the specified Linear team and imports them "
              + "into a new ShipFlow project. Creates an ImportJob record for tracking.")
  public ResponseEntity<ImportJobDTO> importFromLinear(
      @RequestBody LinearImportRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {

    if (request.getTeamId() == null || request.getTeamId().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "teamId is required");
    }
    if (request.getProjectName() == null || request.getProjectName().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "projectName is required");
    }

    User currentUser = resolveUser(userDetails);
    log.info(
        "Linear API import started by user={} teamId={} projectName={}",
        currentUser.getUsername(),
        request.getTeamId(),
        request.getProjectName());

    ImportJobDTO result = linearApiImportService.importFromLinear(request, currentUser);
    return ResponseEntity.ok(result);
  }

  // ── Internal helpers ───────────────────────────────────────────────────────

  private User resolveUser(UserDetails userDetails) {
    return userRepository
        .findByUsername(userDetails.getUsername())
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "User not found: " + userDetails.getUsername()));
  }
}
