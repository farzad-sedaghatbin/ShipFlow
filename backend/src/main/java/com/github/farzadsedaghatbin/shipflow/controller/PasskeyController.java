package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.AuthResponse;
import com.github.farzadsedaghatbin.shipflow.dto.passkey.PasskeyCredentialDTO;
import com.github.farzadsedaghatbin.shipflow.dto.passkey.PasskeyLoginOptionsRequest;
import com.github.farzadsedaghatbin.shipflow.dto.passkey.PasskeyLoginOptionsResponse;
import com.github.farzadsedaghatbin.shipflow.dto.passkey.PasskeyLoginVerifyRequest;
import com.github.farzadsedaghatbin.shipflow.dto.passkey.PasskeyRegistrationOptionsResponse;
import com.github.farzadsedaghatbin.shipflow.dto.passkey.PasskeyRegistrationVerifyRequest;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.security.JwtTokenProvider;
import com.github.farzadsedaghatbin.shipflow.service.PasskeyService;
import com.github.farzadsedaghatbin.shipflow.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * WebAuthn passkey endpoints, split across two authorization tiers:
 *
 * <ul>
 *   <li>{@code /api/auth/passkeys/**} — login ceremony, reachable while
 *       unauthenticated (falls under the existing {@code /api/auth/**}
 *       {@code permitAll()} rule in {@code SecurityConfig}).
 *   <li>{@code /api/passkeys/**} — self-service management for the current
 *       authenticated user (falls under the generic {@code /api/**}
 *       authenticated rule).
 * </ul>
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Passkeys", description = "WebAuthn passkey registration and login")
@Slf4j
public class PasskeyController {

  private final PasskeyService passkeyService;
  private final UserService userService;
  private final JwtTokenProvider jwtTokenProvider;

  // ----------------------------------------------------------------------
  // Login ceremony — unauthenticated
  // ----------------------------------------------------------------------

  @PostMapping("/api/auth/passkeys/login/options")
  @PreAuthorize("permitAll()")
  @Operation(summary = "Begin a passkey login ceremony for a username")
  public ResponseEntity<PasskeyLoginOptionsResponse> loginOptions(
      @Valid @RequestBody PasskeyLoginOptionsRequest request) {
    return ResponseEntity.ok(passkeyService.beginLogin(request.getUsername()));
  }

  @PostMapping("/api/auth/passkeys/login/options/discoverable")
  @PreAuthorize("permitAll()")
  @Operation(summary = "Begin a usernameless passkey login ceremony",
      description = "For conditional UI/autofill: no username is known yet, so allowCredentials is "
          + "empty and the browser resolves candidates from its own discoverable-credential store.")
  public ResponseEntity<PasskeyLoginOptionsResponse> discoverableLoginOptions() {
    return ResponseEntity.ok(passkeyService.beginDiscoverableLogin());
  }

  @PostMapping("/api/auth/passkeys/login/verify")
  @PreAuthorize("permitAll()")
  @Operation(summary = "Verify a passkey login assertion and issue a JWT")
  public ResponseEntity<AuthResponse> loginVerify(@Valid @RequestBody PasskeyLoginVerifyRequest request) {
    String username = passkeyService.finishLogin(request);
    String token = jwtTokenProvider.generateTokenFromUsername(username);
    User user = userService.findUserByUsername(username);

    AuthResponse response = AuthResponse.builder().token(token).type("Bearer").userId(user.getId())
        .username(user.getUsername()).role(user.getRole())
        .personId(user.getPerson() != null ? user.getPerson().getId() : null)
        .personName(user.getPerson() != null ? user.getPerson().getName() : null).build();

    return ResponseEntity.ok(response);
  }

  // ----------------------------------------------------------------------
  // Self-service management — authenticated
  // ----------------------------------------------------------------------

  @PostMapping("/api/passkeys/register/options")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Begin registering a new passkey for the current user")
  public ResponseEntity<PasskeyRegistrationOptionsResponse> registerOptions() {
    User user = currentUser();
    return ResponseEntity.ok(passkeyService.beginRegistration(user));
  }

  @PostMapping("/api/passkeys/register/verify")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Verify and persist a new passkey for the current user")
  public ResponseEntity<PasskeyCredentialDTO> registerVerify(
      @Valid @RequestBody PasskeyRegistrationVerifyRequest request) {
    User user = currentUser();
    return ResponseEntity.ok(passkeyService.finishRegistration(user, request));
  }

  @GetMapping("/api/passkeys")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "List the current user's registered passkeys")
  public ResponseEntity<List<PasskeyCredentialDTO>> listPasskeys() {
    return ResponseEntity.ok(passkeyService.listCredentials(currentUser().getId()));
  }

  @DeleteMapping("/api/passkeys/{id}")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Delete one of the current user's passkeys")
  public ResponseEntity<Void> deletePasskey(@PathVariable Long id) {
    passkeyService.deleteCredential(currentUser().getId(), id);
    return ResponseEntity.noContent().build();
  }

  private User currentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return userService.findUserByUsername(authentication.getName());
  }
}
