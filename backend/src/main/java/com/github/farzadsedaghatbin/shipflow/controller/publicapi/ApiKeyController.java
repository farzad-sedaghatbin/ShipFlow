package com.github.farzadsedaghatbin.shipflow.controller.publicapi;

import com.github.farzadsedaghatbin.shipflow.dto.publicapi.ApiKeyDTO;
import com.github.farzadsedaghatbin.shipflow.dto.publicapi.CreateApiKeyRequest;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import com.github.farzadsedaghatbin.shipflow.service.ApiKeyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Manages API keys. These endpoints require JWT authentication (not API key auth),
 * since users manage their own keys from the ShipFlow UI.
 */
@RestController
@RequestMapping("/api/api-keys")
@RequiredArgsConstructor
@Tag(name = "API Key Management", description = "Create, list, and revoke API keys for the public API")
public class ApiKeyController {

  private final ApiKeyService apiKeyService;
  private final UserRepository userRepository;

  @PostMapping
  @Operation(summary = "Create a new API key – the raw key is returned only once")
  public ResponseEntity<ApiKeyDTO> createKey(
      @AuthenticationPrincipal UserDetails principal,
      @Valid @RequestBody CreateApiKeyRequest request) {
    User user = resolveUser(principal);
    ApiKeyDTO dto = apiKeyService.createApiKey(user.getId(), request);
    return ResponseEntity.status(HttpStatus.CREATED).body(dto);
  }

  @GetMapping
  @Operation(summary = "List all API keys for the current user")
  public ResponseEntity<List<ApiKeyDTO>> listKeys(@AuthenticationPrincipal UserDetails principal) {
    User user = resolveUser(principal);
    return ResponseEntity.ok(apiKeyService.listKeys(user.getId()));
  }

  @DeleteMapping("/{keyId}")
  @Operation(summary = "Revoke an API key")
  public ResponseEntity<Void> revokeKey(
      @AuthenticationPrincipal UserDetails principal,
      @PathVariable Long keyId) {
    User user = resolveUser(principal);
    apiKeyService.revokeKey(keyId, user.getId());
    return ResponseEntity.noContent().build();
  }

  private User resolveUser(UserDetails principal) {
    return userRepository.findByUsername(principal.getUsername())
        .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
  }
}
