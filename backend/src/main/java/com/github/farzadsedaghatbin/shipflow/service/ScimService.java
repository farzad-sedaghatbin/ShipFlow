package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.scim.ScimError;
import com.github.farzadsedaghatbin.shipflow.dto.scim.ScimListResponse;
import com.github.farzadsedaghatbin.shipflow.dto.scim.ScimUser;
import com.github.farzadsedaghatbin.shipflow.entity.ProvisionedVia;
import com.github.farzadsedaghatbin.shipflow.entity.ScimAuditLog;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.UserRole;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import com.github.farzadsedaghatbin.shipflow.repository.OrganizationSettingsRepository;
import com.github.farzadsedaghatbin.shipflow.repository.ScimAuditLogRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * SCIM 2.0 provisioning service.
 *
 * <p>Implements SCIM 2.0 (RFC 7643/7644) user lifecycle operations. Authentication is handled by
 * validating the Bearer token on every request via {@link
 * OrganizationSettingsService#verifyScimToken(String)}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScimService {

  static final String SCIM_USER_SCHEMA = "urn:ietf:params:scim:schemas:core:2.0:User";
  static final String SCIM_LIST_SCHEMA = "urn:ietf:params:scim:schemas:core:2.0:ListResponse";
  static final String SCIM_ERROR_SCHEMA = "urn:ietf:params:scim:api:messages:2.0:Error";

  private final UserRepository userRepository;
  private final OrganizationSettingsRepository settingsRepository;
  private final ScimAuditLogRepository auditLogRepository;
  private final OrganizationSettingsService orgSettingsService;
  private final PasswordEncoder passwordEncoder;

  // ── Token validation ───────────────────────────────────────────────────────

  /**
   * Validate the {@code Authorization: Bearer <token>} header against the stored hash.
   *
   * @param authHeader full value of the Authorization header (may be null)
   * @throws ResponseStatusException HTTP 401 if SCIM is disabled or token is invalid
   */
  public void validateScimToken(String authHeader) {
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      throw unauthorised("Missing or malformed Authorization header");
    }
    String rawToken = authHeader.substring(7).strip();
    if (!orgSettingsService.verifyScimToken(rawToken)) {
      throw unauthorised("Invalid SCIM bearer token or SCIM is disabled");
    }
  }

  // ── List ───────────────────────────────────────────────────────────────────

  /**
   * Return a paginated list of active (non-deleted) users.
   *
   * @param startIndex 1-based start index (SCIM spec §3.4.2)
   * @param count max items to return
   */
  @Transactional(readOnly = true)
  public ScimListResponse listUsers(int startIndex, int count) {
    List<User> allUsers = userRepository.findByDeletedAtIsNull();
    int total = allUsers.size();
    int fromIndex = Math.max(0, startIndex - 1);
    int toIndex = Math.min(fromIndex + Math.max(1, count), total);
    List<User> page = fromIndex >= total ? List.of() : allUsers.subList(fromIndex, toIndex);

    List<ScimUser> resources = page.stream().map(this::toScimUser).toList();
    return ScimListResponse.builder()
        .schemas(List.of(SCIM_LIST_SCHEMA))
        .totalResults(total)
        .startIndex(startIndex)
        .itemsPerPage(resources.size())
        .resources(resources)
        .build();
  }

  // ── Get ────────────────────────────────────────────────────────────────────

  /**
   * Find a single user by ShipFlow internal id.
   *
   * @param id string representation of the internal user id
   */
  @Transactional(readOnly = true)
  public ScimUser getUser(String id) {
    User user = findActiveUserById(id);
    return toScimUser(user);
  }

  // ── Create ─────────────────────────────────────────────────────────────────

  /**
   * Provision a new user from a SCIM User resource.
   *
   * <p>The new account is provisioned with role {@code DEVELOPER} and {@code provisionedVia =
   * SCIM}.
   */
  @Transactional
  public ScimUser createUser(ScimUser scimUser) {
    String username = resolveUsername(scimUser);
    if (userRepository.existsByUsername(username)) {
      throw conflict("A user with userName '" + username + "' already exists");
    }

    User user = User.builder()
        .username(username)
        .email(resolveEmail(scimUser))
        .password(passwordEncoder.encode(generateRandomPassword()))
        .role(UserRole.MEMBER)
        .isActive(scimUser.isActive())
        .provisionedVia(ProvisionedVia.SCIM)
        .externalUserId(scimUser.getExternalId())
        .build();

    user = userRepository.save(user);
    log.info("SCIM: created user '{}'", username);
    appendAudit("USER_CREATED", scimUser.getExternalId(), username,
        "Provisioned via SCIM 2.0");

    return toScimUser(user);
  }

  // ── Replace (PUT) ──────────────────────────────────────────────────────────

  /** Full-replace a user (all mutable fields overwritten). */
  @Transactional
  public ScimUser replaceUser(String id, ScimUser scimUser) {
    User user = findActiveUserById(id);
    String newUsername = resolveUsername(scimUser);
    String previousUsername = user.getUsername();

    user.setUsername(newUsername);
    user.setEmail(resolveEmail(scimUser));
    user.setIsActive(scimUser.isActive());
    if (scimUser.getExternalId() != null) {
      user.setExternalUserId(scimUser.getExternalId());
    }

    user = userRepository.save(user);
    log.info("SCIM: replaced user '{}' (was '{}')", newUsername, previousUsername);
    appendAudit("USER_UPDATED", scimUser.getExternalId(), newUsername,
        "Full replace via SCIM 2.0");

    return toScimUser(user);
  }

  // ── Patch ──────────────────────────────────────────────────────────────────

  /**
   * Apply a SCIM PATCH request. Only {@code op=replace} on the {@code active} attribute is
   * implemented (covers deactivation / reactivation).
   *
   * @param id user id
   * @param patchBody the raw JSON-decoded patch body
   */
  @Transactional
  public ScimUser patchUser(String id, Map<String, Object> patchBody) {
    // Use findUserById (not findActiveUserById) so that reactivation of a soft-deleted
    // user via op=replace active=true is possible.
    User user = findUserById(id);

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> operations =
        (List<Map<String, Object>>) patchBody.getOrDefault("Operations", List.of());

    for (Map<String, Object> op : operations) {
      String opType = String.valueOf(op.getOrDefault("op", "")).toLowerCase();
      if (!"replace".equals(opType)) {
        continue; // unsupported op — skip (SCIM spec allows ignoring unknown ops)
      }
      Object valueObj = op.get("value");
      if (valueObj instanceof Map<?, ?> valueMap) {
        Object activeVal = valueMap.get("active");
        if (activeVal != null) {
          boolean newActive = parseBoolean(activeVal);
          user.setIsActive(newActive);
          if (!newActive) {
            user.setDeletedAt(LocalDateTime.now());
          } else {
            user.setDeletedAt(null);
          }
          userRepository.save(user);
          String eventType = newActive ? "USER_REACTIVATED" : "USER_DEACTIVATED";
          log.info("SCIM: {} user '{}'", eventType.toLowerCase(), user.getUsername());
          appendAudit(eventType, user.getExternalUserId(), user.getUsername(),
              "Patch op=replace active=" + newActive + " via SCIM 2.0");
        }
      }
    }
    return toScimUser(user);
  }

  // ── Delete ─────────────────────────────────────────────────────────────────

  /** Soft-delete a user (sets deletedAt and isActive=false). */
  @Transactional
  public void deleteUser(String id) {
    User user = findActiveUserById(id);
    user.setIsActive(false);
    user.setDeletedAt(LocalDateTime.now());
    userRepository.save(user);
    log.info("SCIM: soft-deleted user '{}'", user.getUsername());
    appendAudit("USER_DELETED", user.getExternalUserId(), user.getUsername(),
        "Soft-deleted via SCIM 2.0");
  }

  // ── Helpers ────────────────────────────────────────────────────────────────

  private User findActiveUserById(String id) {
    long longId;
    try {
      longId = Long.parseLong(id);
    } catch (NumberFormatException e) {
      throw new ResourceNotFoundException("User not found: " + id);
    }
    return userRepository.findById(longId)
        .filter(u -> u.getDeletedAt() == null)
        .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
  }

  /** Find a user by id regardless of soft-delete status. Used for PATCH to allow reactivation. */
  private User findUserById(String id) {
    long longId;
    try {
      longId = Long.parseLong(id);
    } catch (NumberFormatException e) {
      throw new ResourceNotFoundException("User not found: " + id);
    }
    return userRepository.findById(longId)
        .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
  }

  private ScimUser toScimUser(User user) {
    String primaryEmail = user.getEmail() != null ? user.getEmail() : user.getUsername();
    ScimUser.ScimName name = ScimUser.ScimName.builder()
        .formatted(user.getUsername())
        .givenName(extractGivenName(user))
        .familyName(extractFamilyName(user))
        .build();

    return ScimUser.builder()
        .schemas(List.of(SCIM_USER_SCHEMA))
        .id(String.valueOf(user.getId()))
        .externalId(user.getExternalUserId())
        .userName(user.getUsername())
        .name(name)
        .emails(List.of(ScimUser.ScimEmail.builder().value(primaryEmail).type("work").primary(true).build()))
        .active(Boolean.TRUE.equals(user.getIsActive()))
        .meta(ScimUser.ScimMeta.builder()
            .resourceType("User")
            .location("/scim/v2/Users/" + user.getId())
            .build())
        .build();
  }

  private static String resolveUsername(ScimUser scimUser) {
    String u = scimUser.getUserName();
    if (u == null || u.isBlank()) {
      throw new IllegalArgumentException("userName is required");
    }
    return u.trim();
  }

  private static String resolveEmail(ScimUser scimUser) {
    if (scimUser.getEmails() != null && !scimUser.getEmails().isEmpty()) {
      return scimUser.getEmails().stream()
          .filter(ScimUser.ScimEmail::isPrimary)
          .findFirst()
          .map(ScimUser.ScimEmail::getValue)
          .orElse(scimUser.getEmails().get(0).getValue());
    }
    return scimUser.getUserName();
  }

  private static String extractGivenName(User user) {
    if (user.getPerson() != null && user.getPerson().getName() != null) {
      String[] parts = user.getPerson().getName().split(" ", 2);
      return parts[0];
    }
    return user.getUsername();
  }

  private static String extractFamilyName(User user) {
    if (user.getPerson() != null && user.getPerson().getName() != null) {
      String[] parts = user.getPerson().getName().split(" ", 2);
      return parts.length > 1 ? parts[1] : "";
    }
    return "";
  }

  /** Generate a random 16-character password (never exposed to user — SCIM accounts use SSO). */
  private static String generateRandomPassword() {
    return java.util.UUID.randomUUID().toString();
  }

  private void appendAudit(String eventType, String externalId, String username, String details) {
    auditLogRepository.save(ScimAuditLog.builder()
        .eventType(eventType)
        .externalId(externalId)
        .username(username)
        .details(details)
        .occurredAt(OffsetDateTime.now())
        .build());
  }

  private static boolean parseBoolean(Object value) {
    if (value instanceof Boolean b) {
      return b;
    }
    return Boolean.parseBoolean(String.valueOf(value));
  }

  /** Build a SCIM-friendly 401 response. */
  static ScimError unauthorisedError(String detail) {
    return ScimError.builder()
        .schemas(List.of(SCIM_ERROR_SCHEMA))
        .status(HttpStatus.UNAUTHORIZED.value())
        .detail(detail)
        .build();
  }

  /** Build a SCIM-friendly 404 response. */
  static ScimError notFoundError(String detail) {
    return ScimError.builder()
        .schemas(List.of(SCIM_ERROR_SCHEMA))
        .status(HttpStatus.NOT_FOUND.value())
        .detail(detail)
        .build();
  }

  /** Build a SCIM-friendly 409 conflict response. */
  static ScimError conflictError(String detail) {
    return ScimError.builder()
        .schemas(List.of(SCIM_ERROR_SCHEMA))
        .status(HttpStatus.CONFLICT.value())
        .detail(detail)
        .build();
  }

  private static ResponseStatusException unauthorised(String message) {
    return new ResponseStatusException(HttpStatus.UNAUTHORIZED, message);
  }

  private static ResponseStatusException conflict(String message) {
    return new ResponseStatusException(HttpStatus.CONFLICT, message);
  }
}
