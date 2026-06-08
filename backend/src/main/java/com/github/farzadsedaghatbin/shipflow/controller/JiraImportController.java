package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.ImportJobDTO;
import com.github.farzadsedaghatbin.shipflow.dto.JiraConnectionStatusDTO;
import com.github.farzadsedaghatbin.shipflow.dto.JiraImportRequest;
import com.github.farzadsedaghatbin.shipflow.dto.JiraProjectDTO;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import com.github.farzadsedaghatbin.shipflow.service.JiraApiImportService;
import com.github.farzadsedaghatbin.shipflow.service.JiraOAuthService;
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
 * REST controller for Jira API import — OAuth flow and issue import (v1.2.0 S30).
 *
 * <p>Base path: {@code /api/import/jira}
 */
@RestController
@RequestMapping("/api/import/jira")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Jira Import", description = "Jira API OAuth flow and issue import (v1.2.0)")
public class JiraImportController {

  private final JiraOAuthService jiraOAuthService;
  private final JiraApiImportService jiraApiImportService;
  private final UserRepository userRepository;

  // ── Status ────────────────────────────────────────────────────────────────

  @GetMapping("/status")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Get Jira connection status")
  public ResponseEntity<JiraConnectionStatusDTO> getStatus() {
    return ResponseEntity.ok(jiraOAuthService.getConnectionStatus());
  }

  // ── OAuth flow ─────────────────────────────────────────────────────────────

  @PostMapping("/authorize")
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  @Operation(
      summary = "Begin Jira OAuth flow",
      description =
          "Returns an authorization URL that the user must visit to grant ShipFlow read access "
              + "to their Atlassian workspace. The base URL is auto-detected from the request if "
              + "not supplied in the body.")
  public ResponseEntity<Map<String, String>> authorize(
      @RequestBody(required = false) Map<String, String> body, HttpServletRequest httpRequest) {

    if (!jiraOAuthService.isConfigured()) {
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
          .body(
              Map.of(
                  "error",
                  "Jira OAuth is not configured. Set JIRA_OAUTH_CLIENT_ID and "
                      + "JIRA_OAUTH_CLIENT_SECRET environment variables."));
    }

    // Resolve base URL — proxy-aware, same pattern as LinearImportController
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

    String authorizationUrl = jiraOAuthService.generateAuthorizationUrl(baseUrl);
    return ResponseEntity.ok(Map.of("authorizationUrl", authorizationUrl));
  }

  @GetMapping("/callback")
  @Operation(
      summary = "Handle Jira OAuth callback",
      description =
          "Receives the authorization code from Atlassian after user consent, exchanges it for an "
              + "access token, fetches the accessible cloud id, and redirects to the frontend "
              + "import page.")
  public void callback(
      @RequestParam(value = "code", required = false) String code,
      @RequestParam(value = "state", required = false) String state,
      @RequestParam(value = "error", required = false) String error,
      HttpServletResponse response)
      throws IOException {

    if (error != null) {
      log.warn("Jira OAuth callback received error: {}", error);
      response.sendRedirect("/import?tab=jira&jira_error=" + error);
      return;
    }

    if (code == null || state == null) {
      log.warn("Jira OAuth callback: missing code or state");
      response.sendRedirect("/import?tab=jira&jira_error=missing_params");
      return;
    }

    String redirectUrl = jiraOAuthService.processCallback(code, state);
    response.sendRedirect(redirectUrl);
  }

  // ── Projects ───────────────────────────────────────────────────────────────

  @GetMapping("/projects")
  @PreAuthorize("isAuthenticated()")
  @Operation(
      summary = "List available Jira projects",
      description =
          "Queries the Jira API for all projects accessible by the stored access token.")
  public ResponseEntity<List<JiraProjectDTO>> listProjects() {
    String accessToken = jiraOAuthService.getStoredAccessToken();
    String cloudId = jiraOAuthService.getStoredCloudId();
    if (accessToken == null || accessToken.isBlank()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Jira is not connected. Complete OAuth flow first.");
    }
    if (cloudId == null || cloudId.isBlank()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Jira cloud id is missing. Reconnect the Jira integration.");
    }
    List<JiraProjectDTO> projects = jiraApiImportService.fetchProjects(accessToken, cloudId);
    return ResponseEntity.ok(projects);
  }

  // ── Disconnect ─────────────────────────────────────────────────────────────

  @DeleteMapping("/disconnect")
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  @Operation(
      summary = "Disconnect Jira integration",
      description = "Removes the stored Jira access token, refresh token, and cloud details.")
  public ResponseEntity<Void> disconnect() {
    jiraOAuthService.disconnect();
    return ResponseEntity.ok().build();
  }

  // ── Import ─────────────────────────────────────────────────────────────────

  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  @Operation(
      summary = "Import issues from Jira",
      description =
          "Fetches sprints, epics, and issues from the specified Jira project and imports them "
              + "into a new ShipFlow project. Creates an ImportJob record for tracking.")
  public ResponseEntity<ImportJobDTO> importFromJira(
      @RequestBody JiraImportRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {

    if (request.getProjectKey() == null || request.getProjectKey().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "projectKey is required");
    }
    if (request.getProjectName() == null || request.getProjectName().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "projectName is required");
    }

    User currentUser = resolveUser(userDetails);
    log.info(
        "Jira API import started by user={} projectKey={} projectName={}",
        currentUser.getUsername(),
        request.getProjectKey(),
        request.getProjectName());

    ImportJobDTO result = jiraApiImportService.importFromJira(request, currentUser);
    return ResponseEntity.ok(result);
  }

  // ── Internal helpers ───────────────────────────────────────────────────────

  private User resolveUser(UserDetails userDetails) {
    return userRepository
        .findByUsername(userDetails.getUsername())
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "User not found: " + userDetails.getUsername()));
  }
}
