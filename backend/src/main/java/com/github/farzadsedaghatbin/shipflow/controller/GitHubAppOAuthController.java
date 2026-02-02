package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.github.*;
import com.github.farzadsedaghatbin.shipflow.service.MessageService;
import com.github.farzadsedaghatbin.shipflow.service.github.GitHubAppOAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.*;

/**
 * Controller for GitHub App OAuth flow and installation management.
 *
 * <p>This controller provides endpoints for: - Initiating GitHub App authorization
 * (organization-wide consent) - Handling OAuth callbacks from GitHub - Managing GitHub App
 * installations - Syncing repositories from all installations
 *
 * <p>Benefits over manual repository registration: - Single authorization grants access to all
 * organization repositories - Automatic webhook configuration for all repositories - New
 * repositories automatically accessible (if "all" is selected) - No need to add repositories one by
 * one
 */
@RestController
@RequestMapping("/api/github/app")
@RequiredArgsConstructor
@Slf4j
@Tag(
    name = "GitHub App",
    description = "APIs for GitHub App OAuth flow and bulk repository management")
public class GitHubAppOAuthController {

  private final GitHubAppOAuthService oauthService;
  private final MessageService messageService;

  @GetMapping("/status")
  @PreAuthorize("isAuthenticated()")
  @Operation(
      summary = "Get GitHub App configuration status",
      description = "Check if GitHub App is properly configured and return connection statistics")
  public ResponseEntity<Map<String, Object>> getConfigurationStatus() {
    return ResponseEntity.ok(oauthService.getConfigurationStatus());
  }

  @PostMapping("/authorize")
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  @Operation(
      summary = "Initiate GitHub App authorization",
      description =
          "Generate authorization URL for installing the GitHub App on an organization. "
              + "This is a one-time setup that grants access to all organization repositories.")
  public ResponseEntity<GitHubOAuthUrlResponse> initiateAuthorization(
      @RequestBody(required = false) GitHubOAuthInitRequest request,
      HttpServletRequest httpRequest) {

    if (!oauthService.isAppConfigured()) {
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
          .body(
              GitHubOAuthUrlResponse.builder()
                  .instructions("GitHub App is not configured. Please contact your administrator.")
                  .build());
    }

    if (request == null) {
      request = new GitHubOAuthInitRequest();
    }

    // Auto-detect base URL from request if not provided
    if (request.getBaseUrl() == null || request.getBaseUrl().isEmpty()) {
      String scheme = httpRequest.getScheme();
      String serverName = httpRequest.getServerName();
      int serverPort = httpRequest.getServerPort();

      // Check for forwarded headers (behind proxy/load balancer)
      String forwardedProto = httpRequest.getHeader("X-Forwarded-Proto");
      String forwardedHost = httpRequest.getHeader("X-Forwarded-Host");

      if (forwardedProto != null) scheme = forwardedProto;
      if (forwardedHost != null) serverName = forwardedHost.split(":")[0];

      StringBuilder baseUrl = new StringBuilder(scheme).append("://").append(serverName);
      if (("http".equals(scheme) && serverPort != 80)
          || ("https".equals(scheme) && serverPort != 443)) {
        baseUrl.append(":").append(serverPort);
      }
      request.setBaseUrl(baseUrl.toString());
    }

    GitHubOAuthUrlResponse response = oauthService.generateAuthorizationUrl(request);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/callback")
  @Operation(
      summary = "Handle GitHub App OAuth callback",
      description =
          "This endpoint receives the callback from GitHub after app installation. "
              + "It syncs all accessible repositories automatically.")
  public void handleCallback(
      @Parameter(description = "GitHub App installation ID") @RequestParam("installation_id")
          Long installationId,
      @Parameter(description = "State parameter for CSRF protection")
          @RequestParam(value = "state", required = false)
          String state,
      @Parameter(description = "Setup action taken by user")
          @RequestParam(value = "setup_action", required = false)
          String setupAction,
      HttpServletResponse response)
      throws IOException {

    log.info(
        "Received GitHub App callback: installationId={}, setupAction={}",
        installationId,
        setupAction);

    GitHubOAuthCallbackResult result =
        oauthService.processCallback(installationId, state, setupAction);

    // Redirect to frontend with result
    String redirectUrl =
        result.getRedirectUrl() != null ? result.getRedirectUrl() : "/settings/integrations";

    if (result.isSuccess()) {
      redirectUrl += "?github_connected=true&repos=" + result.getRepositoriesSynced();
    } else {
      redirectUrl += "?github_error=" + result.getError();
    }

    response.sendRedirect(redirectUrl);
  }

  @GetMapping("/installations")
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  @Operation(
      summary = "Get all GitHub App installations",
      description = "List all active GitHub App installations with their repository counts")
  public ResponseEntity<List<GitHubAppInstallationDTO>> getAllInstallations() {
    return ResponseEntity.ok(oauthService.getAllInstallations());
  }

  @GetMapping("/installations/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  @Operation(
      summary = "Get a specific GitHub App installation",
      description = "Get details of a specific installation including repositories")
  public ResponseEntity<GitHubAppInstallationDTO> getInstallation(@PathVariable Long id) {
    return oauthService
        .getInstallation(id)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @DeleteMapping("/installations/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
      summary = "Remove a GitHub App installation",
      description =
          "Deactivate a GitHub App installation (repositories will remain but won't receive updates)")
  public ResponseEntity<String> removeInstallation(@PathVariable Long id) {
    oauthService.removeInstallation(id);
    return ResponseEntity.ok(messageService.getMessage("github.installation.removed"));
  }

  @PostMapping("/installations/{id}/sync")
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  @Operation(
      summary = "Sync repositories for an installation",
      description = "Fetch and sync all repositories accessible through a specific installation")
  public ResponseEntity<GitHubBulkSyncResultDTO> syncInstallationRepositories(
      @PathVariable Long id) {
    return oauthService
        .getInstallation(id)
        .map(
            installation -> {
              // Perform sync for this specific installation
              return oauthService.syncAllRepositories();
            })
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @PostMapping("/sync-all")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
      summary = "Sync all repositories from all installations",
      description =
          "Perform a full sync of all repositories from all active GitHub App installations. "
              + "This discovers new repositories and updates existing ones.")
  public ResponseEntity<GitHubBulkSyncResultDTO> syncAllRepositories() {
    GitHubBulkSyncResultDTO result = oauthService.syncAllRepositories();
    return ResponseEntity.ok(result);
  }

  @PostMapping("/discover")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
      summary = "Discover existing GitHub App installations",
      description =
          "Fetch all existing GitHub App installations from GitHub API. "
              + "Use this when the app was installed directly on GitHub (not through ShipFlow OAuth flow). "
              + "This will find all organizations/users where the app is installed and sync their repositories.")
  public ResponseEntity<GitHubBulkSyncResultDTO> discoverInstallations() {
    if (!oauthService.isAppConfigured()) {
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
          .body(
              GitHubBulkSyncResultDTO.builder()
                  .success(false)
                  .message("GitHub App is not configured")
                  .build());
    }

    GitHubBulkSyncResultDTO result = oauthService.discoverInstallations();
    return ResponseEntity.ok(result);
  }
}
