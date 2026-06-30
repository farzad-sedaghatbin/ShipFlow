package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.UserRole;
import com.github.farzadsedaghatbin.shipflow.entity.WikiSpace;
import com.github.farzadsedaghatbin.shipflow.entity.WikiSpacePermission;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PermissionType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ResourceType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.WikiGranteeType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.WikiPermissionLevel;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import com.github.farzadsedaghatbin.shipflow.repository.WikiSpacePermissionRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WikiPermissionServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private WikiSpacePermissionRepository wikiSpacePermissionRepository;

  @Mock
  private PermissionService permissionService;

  @InjectMocks
  private WikiPermissionService wikiPermissionService;

  private WikiSpace space;

  @BeforeEach
  void setUp() {
    space = WikiSpace.builder().id(42L).name("Engineering").spaceKey("ENG").projectId(1L).createdBy(1L).build();
  }

  // -----------------------------------------------------------------------
  // Helpers
  // -----------------------------------------------------------------------

  private User userWithRole(Long id, UserRole role) {
    return User.builder().id(id).username("user" + id).role(role).isActive(true).build();
  }

  private WikiSpacePermission userGrant(Long userId, WikiPermissionLevel level) {
    return WikiSpacePermission.builder()
        .id(100L)
        .spaceId(space.getId())
        .granteeType(WikiGranteeType.USER)
        .granteeRef(String.valueOf(userId))
        .level(level)
        .build();
  }

  private WikiSpacePermission roleGrant(UserRole role, WikiPermissionLevel level) {
    return WikiSpacePermission.builder()
        .id(200L)
        .spaceId(space.getId())
        .granteeType(WikiGranteeType.ROLE)
        .granteeRef(role.name())
        .level(level)
        .build();
  }

  // -----------------------------------------------------------------------
  // 1. ADMIN — unconditional full access regardless of ACL
  // -----------------------------------------------------------------------

  @Nested
  @DisplayName("ADMIN user")
  class AdminUser {

    @Test
    @DisplayName("canRead returns true")
    void adminCanRead() {
      User admin = userWithRole(1L, UserRole.ADMIN);
      when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

      assertThat(wikiPermissionService.canRead(1L, space)).isTrue();
      verifyNoInteractions(wikiSpacePermissionRepository, permissionService);
    }

    @Test
    @DisplayName("canWrite returns true")
    void adminCanWrite() {
      User admin = userWithRole(1L, UserRole.ADMIN);
      when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

      assertThat(wikiPermissionService.canWrite(1L, space)).isTrue();
      verifyNoInteractions(wikiSpacePermissionRepository, permissionService);
    }

    @Test
    @DisplayName("no ACL rows needed — PermissionService never called")
    void adminSkipsAclAndRbac() {
      User admin = userWithRole(1L, UserRole.ADMIN);
      when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

      wikiPermissionService.canRead(1L, space);
      wikiPermissionService.canWrite(1L, space);

      verifyNoInteractions(permissionService);
    }
  }

  // -----------------------------------------------------------------------
  // 2a. Explicit USER WRITE grant
  // -----------------------------------------------------------------------

  @Nested
  @DisplayName("Explicit USER grant")
  class ExplicitUserGrant {

    @Test
    @DisplayName("USER WRITE grant → canWrite true, canRead true (WRITE implies READ)")
    void userWriteGrant() {
      User member = userWithRole(2L, UserRole.MEMBER);
      when(userRepository.findById(2L)).thenReturn(Optional.of(member));
      when(wikiSpacePermissionRepository.findBySpaceId(space.getId()))
          .thenReturn(List.of(userGrant(2L, WikiPermissionLevel.WRITE)));

      assertThat(wikiPermissionService.canRead(2L, space)).isTrue();
      assertThat(wikiPermissionService.canWrite(2L, space)).isTrue();
      verifyNoInteractions(permissionService);
    }

    @Test
    @DisplayName("USER READ grant → canRead true, canWrite false")
    void userReadGrant() {
      User member = userWithRole(2L, UserRole.MEMBER);
      when(userRepository.findById(2L)).thenReturn(Optional.of(member));
      when(wikiSpacePermissionRepository.findBySpaceId(space.getId()))
          .thenReturn(List.of(userGrant(2L, WikiPermissionLevel.READ)));

      assertThat(wikiPermissionService.canRead(2L, space)).isTrue();
      assertThat(wikiPermissionService.canWrite(2L, space)).isFalse();
      verifyNoInteractions(permissionService);
    }

    @Test
    @DisplayName("USER grant takes precedence over a ROLE WRITE grant for same space")
    void userGrantTakesPrecedenceOverRoleGrant() {
      User member = userWithRole(3L, UserRole.MEMBER);
      when(userRepository.findById(3L)).thenReturn(Optional.of(member));
      // USER READ grant overrides ROLE WRITE grant
      when(wikiSpacePermissionRepository.findBySpaceId(space.getId()))
          .thenReturn(List.of(
              userGrant(3L, WikiPermissionLevel.READ),
              roleGrant(UserRole.MEMBER, WikiPermissionLevel.WRITE)));

      assertThat(wikiPermissionService.canRead(3L, space)).isTrue();
      assertThat(wikiPermissionService.canWrite(3L, space)).isFalse(); // USER READ wins over ROLE WRITE
      verifyNoInteractions(permissionService);
    }
  }

  // -----------------------------------------------------------------------
  // 2b. Explicit ROLE grant (no USER grant present)
  // -----------------------------------------------------------------------

  @Nested
  @DisplayName("Explicit ROLE grant")
  class ExplicitRoleGrant {

    @Test
    @DisplayName("ROLE WRITE grant honored when no USER grant")
    void roleWriteGrantHonored() {
      User manager = userWithRole(4L, UserRole.MANAGER);
      when(userRepository.findById(4L)).thenReturn(Optional.of(manager));
      when(wikiSpacePermissionRepository.findBySpaceId(space.getId()))
          .thenReturn(List.of(roleGrant(UserRole.MANAGER, WikiPermissionLevel.WRITE)));

      assertThat(wikiPermissionService.canRead(4L, space)).isTrue();
      assertThat(wikiPermissionService.canWrite(4L, space)).isTrue();
      verifyNoInteractions(permissionService);
    }

    @Test
    @DisplayName("ROLE READ grant honored when no USER grant")
    void roleReadGrantHonored() {
      User manager = userWithRole(4L, UserRole.MANAGER);
      when(userRepository.findById(4L)).thenReturn(Optional.of(manager));
      when(wikiSpacePermissionRepository.findBySpaceId(space.getId()))
          .thenReturn(List.of(roleGrant(UserRole.MANAGER, WikiPermissionLevel.READ)));

      assertThat(wikiPermissionService.canRead(4L, space)).isTrue();
      assertThat(wikiPermissionService.canWrite(4L, space)).isFalse();
      verifyNoInteractions(permissionService);
    }

    @Test
    @DisplayName("ROLE grant for different role does not apply")
    void roleGrantForDifferentRole() {
      User readonly = userWithRole(5L, UserRole.READONLY);
      when(userRepository.findById(5L)).thenReturn(Optional.of(readonly));
      // Only MANAGER gets WRITE — READONLY should fall through to RBAC
      when(wikiSpacePermissionRepository.findBySpaceId(space.getId()))
          .thenReturn(List.of(roleGrant(UserRole.MANAGER, WikiPermissionLevel.WRITE)));
      when(permissionService.hasPermission(UserRole.READONLY, ResourceType.WIKI, PermissionType.READ)).thenReturn(false);
      when(permissionService.hasPermission(UserRole.READONLY, ResourceType.WIKI, PermissionType.UPDATE)).thenReturn(false);

      assertThat(wikiPermissionService.canRead(5L, space)).isFalse();
      assertThat(wikiPermissionService.canWrite(5L, space)).isFalse();
    }
  }

  // -----------------------------------------------------------------------
  // 3. No explicit grant → RBAC fallback via PermissionService
  // -----------------------------------------------------------------------

  @Nested
  @DisplayName("RBAC fallback (no ACL rows)")
  class RbacFallback {

    @BeforeEach
    void noAclRows() {
      when(wikiSpacePermissionRepository.findBySpaceId(space.getId())).thenReturn(List.of());
    }

    @Test
    @DisplayName("RBAC grants read and write → both true")
    void rbacGrantsReadAndWrite() {
      User member = userWithRole(6L, UserRole.MEMBER);
      when(userRepository.findById(6L)).thenReturn(Optional.of(member));
      when(permissionService.hasPermission(UserRole.MEMBER, ResourceType.WIKI, PermissionType.READ)).thenReturn(true);
      when(permissionService.hasPermission(UserRole.MEMBER, ResourceType.WIKI, PermissionType.UPDATE)).thenReturn(true);

      assertThat(wikiPermissionService.canRead(6L, space)).isTrue();
      assertThat(wikiPermissionService.canWrite(6L, space)).isTrue();
    }

    @Test
    @DisplayName("RBAC grants only read → canRead true, canWrite false")
    void rbacGrantsReadOnly() {
      User member = userWithRole(6L, UserRole.MEMBER);
      when(userRepository.findById(6L)).thenReturn(Optional.of(member));
      when(permissionService.hasPermission(UserRole.MEMBER, ResourceType.WIKI, PermissionType.READ)).thenReturn(true);
      when(permissionService.hasPermission(UserRole.MEMBER, ResourceType.WIKI, PermissionType.UPDATE)).thenReturn(false);

      assertThat(wikiPermissionService.canRead(6L, space)).isTrue();
      assertThat(wikiPermissionService.canWrite(6L, space)).isFalse();
    }

    @Test
    @DisplayName("RBAC denies both → canRead false, canWrite false")
    void rbacDeniesAll() {
      User readonly = userWithRole(7L, UserRole.READONLY);
      when(userRepository.findById(7L)).thenReturn(Optional.of(readonly));
      when(permissionService.hasPermission(UserRole.READONLY, ResourceType.WIKI, PermissionType.READ)).thenReturn(false);
      when(permissionService.hasPermission(UserRole.READONLY, ResourceType.WIKI, PermissionType.UPDATE)).thenReturn(false);

      assertThat(wikiPermissionService.canRead(7L, space)).isFalse();
      assertThat(wikiPermissionService.canWrite(7L, space)).isFalse();
    }
  }

  // -----------------------------------------------------------------------
  // 4. User not found
  // -----------------------------------------------------------------------

  @Nested
  @DisplayName("User not found")
  class UserNotFound {

    @Test
    @DisplayName("canRead returns false when user does not exist")
    void canReadFalseWhenUserMissing() {
      when(userRepository.findById(99L)).thenReturn(Optional.empty());

      assertThat(wikiPermissionService.canRead(99L, space)).isFalse();
    }

    @Test
    @DisplayName("canWrite returns false when user does not exist")
    void canWriteFalseWhenUserMissing() {
      when(userRepository.findById(99L)).thenReturn(Optional.empty());

      assertThat(wikiPermissionService.canWrite(99L, space)).isFalse();
    }
  }

  // -----------------------------------------------------------------------
  // 5. canCreateSpace / requireCreateSpace
  // -----------------------------------------------------------------------

  @Nested
  @DisplayName("canCreateSpace / requireCreateSpace")
  class CreateSpacePermission {

    @Test
    @DisplayName("ADMIN is always allowed to create a space")
    void adminCanCreateSpace() {
      User admin = userWithRole(1L, UserRole.ADMIN);
      when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

      assertThat(wikiPermissionService.canCreateSpace(1L)).isTrue();
      verifyNoInteractions(wikiSpacePermissionRepository, permissionService);
    }

    @Test
    @DisplayName("role with WIKI CREATE permission is allowed")
    void roleWithWikiCreateCanCreateSpace() {
      User member = userWithRole(2L, UserRole.MEMBER);
      when(userRepository.findById(2L)).thenReturn(Optional.of(member));
      when(permissionService.hasPermission(UserRole.MEMBER, ResourceType.WIKI, PermissionType.CREATE))
          .thenReturn(true);

      assertThat(wikiPermissionService.canCreateSpace(2L)).isTrue();
    }

    @Test
    @DisplayName("role without WIKI CREATE permission → requireCreateSpace throws AccessDeniedException")
    void roleWithoutWikiCreateThrows() {
      User readonly = userWithRole(3L, UserRole.READONLY);
      when(userRepository.findById(3L)).thenReturn(Optional.of(readonly));
      when(permissionService.hasPermission(UserRole.READONLY, ResourceType.WIKI, PermissionType.CREATE))
          .thenReturn(false);

      assertThat(wikiPermissionService.canCreateSpace(3L)).isFalse();
      assertThatThrownBy(() -> wikiPermissionService.requireCreateSpace(3L))
          .isInstanceOf(AccessDeniedException.class);
    }
  }

  // -----------------------------------------------------------------------
  // 6. require* throws AccessDeniedException on deny
  // -----------------------------------------------------------------------

  @Nested
  @DisplayName("requireRead / requireWrite deny paths")
  class RequireThrows {

    @BeforeEach
    void denySetup() {
      User readonly = userWithRole(8L, UserRole.READONLY);
      when(userRepository.findById(8L)).thenReturn(Optional.of(readonly));
      when(wikiSpacePermissionRepository.findBySpaceId(space.getId())).thenReturn(List.of());
      when(permissionService.hasPermission(UserRole.READONLY, ResourceType.WIKI, PermissionType.READ)).thenReturn(false);
      when(permissionService.hasPermission(UserRole.READONLY, ResourceType.WIKI, PermissionType.UPDATE)).thenReturn(false);
    }

    @Test
    @DisplayName("requireRead throws AccessDeniedException when canRead is false")
    void requireReadThrows() {
      assertThatThrownBy(() -> wikiPermissionService.requireRead(8L, space))
          .isInstanceOf(AccessDeniedException.class)
          .hasMessageContaining("READ");
    }

    @Test
    @DisplayName("requireWrite throws AccessDeniedException when canWrite is false")
    void requireWriteThrows() {
      assertThatThrownBy(() -> wikiPermissionService.requireWrite(8L, space))
          .isInstanceOf(AccessDeniedException.class)
          .hasMessageContaining("WRITE");
    }

    @Test
    @DisplayName("requireRead does NOT throw when canRead is true")
    void requireReadDoesNotThrow() {
      User member = userWithRole(9L, UserRole.MEMBER);
      when(userRepository.findById(9L)).thenReturn(Optional.of(member));
      when(wikiSpacePermissionRepository.findBySpaceId(space.getId()))
          .thenReturn(List.of(userGrant(9L, WikiPermissionLevel.READ)));

      // Should not throw
      wikiPermissionService.requireRead(9L, space);
    }

    @Test
    @DisplayName("requireWrite does NOT throw when canWrite is true")
    void requireWriteDoesNotThrow() {
      User member = userWithRole(9L, UserRole.MEMBER);
      when(userRepository.findById(9L)).thenReturn(Optional.of(member));
      when(wikiSpacePermissionRepository.findBySpaceId(space.getId()))
          .thenReturn(List.of(userGrant(9L, WikiPermissionLevel.WRITE)));

      // Should not throw
      wikiPermissionService.requireWrite(9L, space);
    }
  }
}
