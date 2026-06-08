package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

/**
 * Unit tests for {@link ScimService} — pure Mockito, no Spring context.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ScimService Tests")
class ScimServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private OrganizationSettingsRepository settingsRepository;
  @Mock private ScimAuditLogRepository auditLogRepository;
  @Mock private OrganizationSettingsService orgSettingsService;
  @Mock private PasswordEncoder passwordEncoder;

  private ScimService scimService;

  @BeforeEach
  void setUp() {
    scimService = new ScimService(
        userRepository, settingsRepository, auditLogRepository,
        orgSettingsService, passwordEncoder);
    when(passwordEncoder.encode(any())).thenReturn("hashed-password");
    when(auditLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
  }

  // ── Helper to build a test user ──────────────────────────────────────────

  private User buildUser(long id, String username, boolean active) {
    User u = new User();
    u.setId(id);
    u.setUsername(username);
    u.setEmail(username + "@example.com");
    u.setPassword("hashed");
    u.setRole(UserRole.MEMBER);
    u.setIsActive(active);
    u.setProvisionedVia(ProvisionedVia.SCIM);
    return u;
  }

  // ── validateScimToken ────────────────────────────────────────────────────

  @Nested
  @DisplayName("validateScimToken")
  class ValidateScimToken {

    @Test
    @DisplayName("valid token passes without exception")
    void validToken_passes() {
      when(orgSettingsService.verifyScimToken("good-token")).thenReturn(true);
      scimService.validateScimToken("Bearer good-token");
      // no exception
    }

    @Test
    @DisplayName("null header throws 401")
    void nullHeader_throws401() {
      assertThatThrownBy(() -> scimService.validateScimToken(null))
          .isInstanceOf(ResponseStatusException.class)
          .hasMessageContaining("401");
    }

    @Test
    @DisplayName("invalid token throws 401")
    void invalidToken_throws401() {
      when(orgSettingsService.verifyScimToken("bad-token")).thenReturn(false);
      assertThatThrownBy(() -> scimService.validateScimToken("Bearer bad-token"))
          .isInstanceOf(ResponseStatusException.class)
          .hasMessageContaining("401");
    }

    @Test
    @DisplayName("missing Bearer prefix throws 401")
    void missingBearer_throws401() {
      assertThatThrownBy(() -> scimService.validateScimToken("just-a-token"))
          .isInstanceOf(ResponseStatusException.class)
          .hasMessageContaining("401");
    }
  }

  // ── listUsers ────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("listUsers")
  class ListUsers {

    @Test
    @DisplayName("returns paginated results")
    void returnsPaginatedResults() {
      List<User> users = List.of(
          buildUser(1L, "alice", true),
          buildUser(2L, "bob", true),
          buildUser(3L, "carol", true));
      when(userRepository.findByDeletedAtIsNull()).thenReturn(users);

      ScimListResponse result = scimService.listUsers(1, 2);

      assertThat(result.getTotalResults()).isEqualTo(3);
      assertThat(result.getStartIndex()).isEqualTo(1);
      assertThat(result.getItemsPerPage()).isEqualTo(2);
      assertThat(result.getResources()).hasSize(2);
      assertThat(result.getResources().get(0).getUserName()).isEqualTo("alice");
    }

    @Test
    @DisplayName("startIndex beyond total returns empty page")
    void startIndexBeyondTotal_returnsEmpty() {
      when(userRepository.findByDeletedAtIsNull()).thenReturn(List.of(buildUser(1L, "alice", true)));

      ScimListResponse result = scimService.listUsers(10, 10);

      assertThat(result.getTotalResults()).isEqualTo(1);
      assertThat(result.getResources()).isEmpty();
    }

    @Test
    @DisplayName("response contains SCIM list schema")
    void containsScimListSchema() {
      when(userRepository.findByDeletedAtIsNull()).thenReturn(List.of());
      ScimListResponse result = scimService.listUsers(1, 10);
      assertThat(result.getSchemas()).contains(ScimService.SCIM_LIST_SCHEMA);
    }
  }

  // ── createUser ───────────────────────────────────────────────────────────

  @Nested
  @DisplayName("createUser")
  class CreateUser {

    @Test
    @DisplayName("sets provisionedVia=SCIM and logs audit event")
    void setsProvisionedViaAndLogsAudit() {
      when(userRepository.existsByUsername("alice")).thenReturn(false);
      User savedUser = buildUser(42L, "alice", true);
      savedUser.setExternalUserId("ext-001");
      when(userRepository.save(any(User.class))).thenReturn(savedUser);

      ScimUser input = ScimUser.builder()
          .userName("alice")
          .externalId("ext-001")
          .active(true)
          .emails(List.of(ScimUser.ScimEmail.builder().value("alice@example.com").primary(true).build()))
          .build();

      ScimUser result = scimService.createUser(input);

      assertThat(result.getId()).isEqualTo("42");
      assertThat(result.getUserName()).isEqualTo("alice");

      ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
      verify(userRepository).save(userCaptor.capture());
      assertThat(userCaptor.getValue().getProvisionedVia()).isEqualTo(ProvisionedVia.SCIM);

      ArgumentCaptor<ScimAuditLog> auditCaptor = ArgumentCaptor.forClass(ScimAuditLog.class);
      verify(auditLogRepository).save(auditCaptor.capture());
      assertThat(auditCaptor.getValue().getEventType()).isEqualTo("USER_CREATED");
      assertThat(auditCaptor.getValue().getUsername()).isEqualTo("alice");
    }

    @Test
    @DisplayName("duplicate userName throws 409")
    void duplicateUsername_throws409() {
      when(userRepository.existsByUsername("alice")).thenReturn(true);
      ScimUser input = ScimUser.builder().userName("alice").active(true).build();
      assertThatThrownBy(() -> scimService.createUser(input))
          .isInstanceOf(ResponseStatusException.class)
          .hasMessageContaining("409");
    }
  }

  // ── patchUser ────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("patchUser")
  class PatchUser {

    @Test
    @DisplayName("op=replace active=false soft-deletes the user")
    void replaceActiveFalse_softDeletesUser() {
      User user = buildUser(5L, "bob", true);
      when(userRepository.findById(5L)).thenReturn(Optional.of(user));
      when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

      Map<String, Object> patchBody = Map.of(
          "Operations", List.of(Map.of("op", "replace", "value", Map.of("active", false))));

      ScimUser result = scimService.patchUser("5", patchBody);

      assertThat(result.isActive()).isFalse();
      assertThat(user.getDeletedAt()).isNotNull();

      ArgumentCaptor<ScimAuditLog> auditCaptor = ArgumentCaptor.forClass(ScimAuditLog.class);
      verify(auditLogRepository).save(auditCaptor.capture());
      assertThat(auditCaptor.getValue().getEventType()).isEqualTo("USER_DEACTIVATED");
    }

    @Test
    @DisplayName("op=replace active=true reactivates the user")
    void replaceActiveTrue_reactivatesUser() {
      User user = buildUser(5L, "bob", false);
      user.setDeletedAt(LocalDateTime.now().minusDays(1));
      when(userRepository.findById(5L)).thenReturn(Optional.of(user));
      when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

      Map<String, Object> patchBody = Map.of(
          "Operations", List.of(Map.of("op", "replace", "value", Map.of("active", true))));

      ScimUser result = scimService.patchUser("5", patchBody);

      assertThat(result.isActive()).isTrue();
      assertThat(user.getDeletedAt()).isNull();

      ArgumentCaptor<ScimAuditLog> auditCaptor = ArgumentCaptor.forClass(ScimAuditLog.class);
      verify(auditLogRepository).save(auditCaptor.capture());
      assertThat(auditCaptor.getValue().getEventType()).isEqualTo("USER_REACTIVATED");
    }

    @Test
    @DisplayName("unknown user id throws ResourceNotFoundException")
    void unknownId_throwsNotFound() {
      when(userRepository.findById(99L)).thenReturn(Optional.empty());
      Map<String, Object> patchBody = Map.of("Operations", List.of());
      assertThatThrownBy(() -> scimService.patchUser("99", patchBody))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }

  // ── deleteUser ───────────────────────────────────────────────────────────

  @Nested
  @DisplayName("deleteUser")
  class DeleteUser {

    @Test
    @DisplayName("soft-deletes the user and logs audit event")
    void softDeletesAndLogsAudit() {
      User user = buildUser(7L, "carol", true);
      when(userRepository.findById(7L)).thenReturn(Optional.of(user));
      when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

      scimService.deleteUser("7");

      assertThat(user.getIsActive()).isFalse();
      assertThat(user.getDeletedAt()).isNotNull();

      ArgumentCaptor<ScimAuditLog> auditCaptor = ArgumentCaptor.forClass(ScimAuditLog.class);
      verify(auditLogRepository).save(auditCaptor.capture());
      assertThat(auditCaptor.getValue().getEventType()).isEqualTo("USER_DELETED");
    }
  }
}
