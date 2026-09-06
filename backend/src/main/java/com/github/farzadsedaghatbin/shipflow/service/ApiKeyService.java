package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.publicapi.ApiKeyDTO;
import com.github.farzadsedaghatbin.shipflow.dto.publicapi.CreateApiKeyRequest;
import com.github.farzadsedaghatbin.shipflow.entity.ApiKey;
import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ApiKeyScope;
import com.github.farzadsedaghatbin.shipflow.repository.ApiKeyRepository;
import com.github.farzadsedaghatbin.shipflow.repository.ProjectRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ApiKeyService {

  private final ApiKeyRepository apiKeyRepository;
  private final UserRepository userRepository;
  private final ProjectRepository projectRepository;
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  /**
   * Creates a new API key for the given user.
   *
   * @return a DTO that includes the raw key (shown only once).
   */
  public ApiKeyDTO createApiKey(Long userId, CreateApiKeyRequest request) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

    Project restrictedToProject = null;
    if (request.getRestrictedToProjectId() != null) {
      restrictedToProject = projectRepository.findById(request.getRestrictedToProjectId())
          .orElseThrow(() -> new IllegalArgumentException(
              "Project not found: " + request.getRestrictedToProjectId()));
    }

    String rawKey = generateRawKey();
    String keyHash = sha256(rawKey);
    String keyPrefix = rawKey.substring(0, 8);

    ApiKey apiKey = ApiKey.builder()
        .name(request.getName())
        .keyPrefix(keyPrefix)
        .keyHash(keyHash)
        .user(user)
        .scopes(request.getScopes() != null ? request.getScopes() : java.util.Set.of(ApiKeyScope.READ))
        .expiresAt(request.getExpiresAt())
        .restrictedToProjectId(request.getRestrictedToProjectId())
        .build();

    apiKeyRepository.save(apiKey);
    log.info("Created API key '{}' (prefix={}) for user {}, restrictedToProjectId={}",
        request.getName(), keyPrefix, userId, request.getRestrictedToProjectId());

    // Reuse the just-validated Project instead of a second lookup for the DTO's resolved name.
    ApiKeyDTO dto = toDTO(apiKey, restrictedToProject != null ? restrictedToProject.getName() : null);
    dto.setRawKey(rawKey); // returned only on creation
    return dto;
  }

  /**
   * Validates a raw API key and returns the associated entity if valid.
   */
  @Transactional(readOnly = true)
  public Optional<ApiKey> validateKey(String rawKey) {
    String keyHash = sha256(rawKey);
    return apiKeyRepository.findByKeyHash(keyHash)
        .filter(ApiKey::isUsable);
  }

  /**
   * Records the last-used timestamp for an API key.
   */
  public void recordUsage(ApiKey apiKey) {
    apiKey.setLastUsedAt(LocalDateTime.now());
    apiKeyRepository.save(apiKey);
  }

  /**
   * Revokes an API key.
   */
  public void revokeKey(Long keyId, Long userId) {
    ApiKey key = apiKeyRepository.findById(keyId)
        .orElseThrow(() -> new IllegalArgumentException("API key not found: " + keyId));
    if (!key.getUser().getId().equals(userId)) {
      throw new SecurityException("Cannot revoke another user's API key");
    }
    key.setIsActive(false);
    key.setRevokedAt(LocalDateTime.now());
    apiKeyRepository.save(key);
    log.info("Revoked API key id={} for user {}", keyId, userId);
  }

  /**
   * Lists all API keys for a user (never returns the raw key).
   */
  @Transactional(readOnly = true)
  public List<ApiKeyDTO> listKeys(Long userId) {
    List<ApiKey> keys = apiKeyRepository.findByUserId(userId);
    Map<Long, String> projectNames = resolveProjectNames(keys);
    return keys.stream()
        .map(key -> toDTO(key, projectNames.get(key.getRestrictedToProjectId())))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<ApiKeyDTO> listAllKeys() {
    List<ApiKey> keys = apiKeyRepository.findAllWithUsers();
    Map<Long, String> projectNames = resolveProjectNames(keys);
    return keys.stream()
        .map(key -> toDTO(key, projectNames.get(key.getRestrictedToProjectId())))
        .toList();
  }

  /**
   * Batch-resolves project names for every restricted key in the list, avoiding an N+1 lookup
   * when listing many keys (mirrors the batch-fetch pattern used elsewhere in this codebase, e.g.
   * {@code ProjectRepository.findAllById}).
   */
  private Map<Long, String> resolveProjectNames(List<ApiKey> keys) {
    List<Long> projectIds = keys.stream()
        .map(ApiKey::getRestrictedToProjectId)
        .filter(java.util.Objects::nonNull)
        .distinct()
        .toList();
    if (projectIds.isEmpty()) {
      // Not Map.of(): its immutable-collection .get() eagerly requireNonNull()s the key and
      // throws NPE for a null key, which every unrestricted ApiKey (the common case — most
      // keys have no restrictedToProjectId) looks up via toDTO's projectNames.get(key.get
      // RestrictedToProjectId()). Collections.emptyMap() tolerates a null-key .get() (returns
      // null, same as HashMap), matching the non-empty branch below (Collectors.toMap's HashMap).
      return Collections.emptyMap();
    }
    return projectRepository.findAllById(projectIds).stream()
        .collect(Collectors.toMap(Project::getId, Project::getName));
  }

  public void adminRevokeKey(Long keyId) {
    ApiKey key = apiKeyRepository.findById(keyId)
        .orElseThrow(() -> new IllegalArgumentException("API key not found: " + keyId));
    key.setIsActive(false);
    key.setRevokedAt(LocalDateTime.now());
    apiKeyRepository.save(key);
    log.info("Admin revoked API key id={}", keyId);
  }

  // ──────────────────────────── helpers ────────────────────────────

  private String generateRawKey() {
    byte[] bytes = new byte[32];
    SECURE_RANDOM.nextBytes(bytes);
    return "sf_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  static String sha256(String input) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }

  private ApiKeyDTO toDTO(ApiKey key, String restrictedToProjectName) {
    return ApiKeyDTO.builder()
        .id(key.getId())
        .name(key.getName())
        .keyPrefix(key.getKeyPrefix())
        .scopes(key.getScopes())
        .isActive(key.getIsActive())
        .expiresAt(key.getExpiresAt())
        .lastUsedAt(key.getLastUsedAt())
        .createdAt(key.getCreatedAt())
        .revokedAt(key.getRevokedAt())
        .createdByUsername(key.getUser() != null ? key.getUser().getUsername() : null)
        .restrictedToProjectId(key.getRestrictedToProjectId())
        .restrictedToProjectName(restrictedToProjectName)
        .build();
  }
}
