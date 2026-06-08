package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.sso.CreateIdentityProviderRequest;
import com.github.farzadsedaghatbin.shipflow.dto.sso.IdentityProviderDTO;
import com.github.farzadsedaghatbin.shipflow.dto.sso.SsoInitiateResponse;
import com.github.farzadsedaghatbin.shipflow.service.SsoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for SSO (Single Sign-On) identity provider management and SSO flows.
 *
 * <p>Public (unauthenticated) endpoints:
 * <ul>
 *   <li>{@code GET /api/sso/providers} — list enabled providers for the login page
 *   <li>{@code GET /api/sso/initiate/{idpId}} — initiate OIDC or SAML2 flow
 *   <li>{@code GET /api/sso/callback/oidc} — OIDC authorization-code callback
 *   <li>{@code POST /api/sso/callback/saml} — SAML2 POST-binding callback
 * </ul>
 *
 * <p>Admin-only endpoints ({@code /api/admin/sso/providers}): full CRUD.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "SSO", description = "Single Sign-On identity provider management")
public class IdentityProviderController {

  private final SsoService ssoService;

  @Value("${app.frontend.url:http://localhost:3000}")
  private String frontendUrl;

  // =========================================================================
  // Public endpoints (no authentication required)
  // =========================================================================

  /**
   * Returns enabled identity providers for display on the login page.
   * Certificates and secrets are never included.
   */
  @GetMapping("/api/sso/providers")
  @PreAuthorize("permitAll()")
  @Operation(summary = "List enabled SSO providers (public)")
  public ResponseEntity<List<IdentityProviderDTO>> listPublicProviders() {
    return ResponseEntity.ok(ssoService.listProviders());
  }

  /**
   * Initiates an SSO flow for the given provider.
   * For OIDC: returns the authorization redirect URL + state token.
   * For SAML2: returns the SSO endpoint URL.
   */
  @GetMapping("/api/sso/initiate/{idpId}")
  @PreAuthorize("permitAll()")
  @Operation(summary = "Initiate SSO flow for a given identity provider")
  public ResponseEntity<SsoInitiateResponse> initiateSso(@PathVariable Long idpId) {
    // Attempt OIDC first; fall back to SAML2
    try {
      SsoInitiateResponse response = ssoService.initiateOidc(idpId);
      return ResponseEntity.ok(response);
    } catch (IllegalArgumentException ex) {
      // Not OIDC — try SAML2
      String samlUrl = ssoService.initiateSaml(idpId);
      return ResponseEntity.ok(SsoInitiateResponse.builder()
          .redirectUrl(samlUrl)
          .state("idp:" + idpId)
          .build());
    }
  }

  /**
   * OIDC authorization-code callback.
   * Exchanges the code for tokens, resolves/creates the user, issues a ShipFlow JWT,
   * and redirects the browser to {@code {frontendUrl}/sso-callback?token=...}.
   */
  @GetMapping("/api/sso/callback/oidc")
  @PreAuthorize("permitAll()")
  @Operation(summary = "OIDC authorization-code callback")
  public ResponseEntity<Void> oidcCallback(
      @RequestParam String code,
      @RequestParam String state,
      @RequestParam(required = false, name = "idp") Long idpId) {
    String jwt = ssoService.handleOidcCallback(code, state);
    URI redirect = URI.create(frontendUrl + "/sso-callback?token=" + jwt);
    return ResponseEntity.status(302).location(redirect).build();
  }

  /**
   * SAML2 HTTP-POST binding callback.
   * Decodes the SAMLResponse, extracts NameID, resolves/creates the user,
   * issues a ShipFlow JWT, and redirects to {@code {frontendUrl}/sso-callback?token=...}.
   */
  @PostMapping(value = "/api/sso/callback/saml",
      consumes = {"application/x-www-form-urlencoded", "application/json"})
  @PreAuthorize("permitAll()")
  @Operation(summary = "SAML2 POST-binding callback")
  public ResponseEntity<Void> samlCallback(
      @RequestParam(name = "SAMLResponse", required = false) String samlResponseParam,
      @RequestParam(name = "RelayState", required = false) String relayStateParam,
      @RequestBody(required = false)
          com.github.farzadsedaghatbin.shipflow.dto.sso.SsoCallbackRequest body) {
    // Support both form-encoded and JSON bodies
    String samlResponse = samlResponseParam != null ? samlResponseParam
        : (body != null ? body.getSamlResponse() : null);
    String relayState = relayStateParam != null ? relayStateParam
        : (body != null ? body.getRelayState() : null);

    String jwt = ssoService.handleSamlCallback(samlResponse, relayState);
    URI redirect = URI.create(frontendUrl + "/sso-callback?token=" + jwt);
    return ResponseEntity.status(302).location(redirect).build();
  }

  // =========================================================================
  // Admin endpoints — ROLE_ADMIN required
  // =========================================================================

  @GetMapping("/api/admin/sso/providers")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Admin: list all SSO providers")
  public ResponseEntity<List<IdentityProviderDTO>> adminListProviders() {
    return ResponseEntity.ok(ssoService.listAllProviders());
  }

  @PostMapping("/api/admin/sso/providers")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Admin: create an SSO identity provider")
  public ResponseEntity<IdentityProviderDTO> createProvider(
      @Valid @RequestBody CreateIdentityProviderRequest request) {
    return ResponseEntity.ok(ssoService.createProvider(request));
  }

  @PutMapping("/api/admin/sso/providers/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Admin: update an SSO identity provider")
  public ResponseEntity<IdentityProviderDTO> updateProvider(
      @PathVariable Long id, @Valid @RequestBody CreateIdentityProviderRequest request) {
    return ResponseEntity.ok(ssoService.updateProvider(id, request));
  }

  @DeleteMapping("/api/admin/sso/providers/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Admin: disable (soft-delete) an SSO identity provider")
  public ResponseEntity<Void> deleteProvider(@PathVariable Long id) {
    ssoService.deleteProvider(id);
    return ResponseEntity.noContent().build();
  }
}
