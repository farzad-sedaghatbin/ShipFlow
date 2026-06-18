package com.github.farzadsedaghatbin.shipflow.service;

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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves effective read/write access for a user on a {@link WikiSpace}.
 *
 * <p>Resolution order:
 * <ol>
 *   <li>ADMIN role → unconditional full access (read + write).</li>
 *   <li>Explicit {@link WikiSpacePermission} row for the space:
 *       USER grant checked first, then ROLE grant.</li>
 *   <li>Fall back to the global RBAC table via {@link PermissionService}
 *       ({@code WIKI / READ} and {@code WIKI / UPDATE}).</li>
 *   <li>Otherwise deny.</li>
 * </ol>
 *
 * <p>Note: {@code space.projectId} scopes the space to a project for listing
 * purposes, but per-space access resolution uses the RBAC fallback above.
 * // future: project-membership-based access
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WikiPermissionService {

  private final UserRepository userRepository;
  private final WikiSpacePermissionRepository wikiSpacePermissionRepository;
  private final PermissionService permissionService;

  /**
   * Returns {@code true} if {@code userId} may read {@code space}.
   *
   * @param userId the user to check
   * @param space  the wiki space
   * @return true if read access is granted
   */
  @Transactional(readOnly = true)
  public boolean canRead(Long userId, WikiSpace space) {
    return resolveAccess(userId, space).canRead();
  }

  /**
   * Returns {@code true} if {@code userId} may write to {@code space}.
   *
   * @param userId the user to check
   * @param space  the wiki space
   * @return true if write access is granted
   */
  @Transactional(readOnly = true)
  public boolean canWrite(Long userId, WikiSpace space) {
    return resolveAccess(userId, space).canWrite();
  }

  /**
   * Asserts read access; throws {@link AccessDeniedException} if denied.
   *
   * @param userId the user to check
   * @param space  the wiki space
   * @throws AccessDeniedException if read access is denied
   */
  public void requireRead(Long userId, WikiSpace space) {
    if (!canRead(userId, space)) {
      throw new AccessDeniedException(
          String.format("User %d does not have READ access to wiki space %d", userId, space.getId()));
    }
  }

  /**
   * Asserts write access; throws {@link AccessDeniedException} if denied.
   *
   * @param userId the user to check
   * @param space  the wiki space
   * @throws AccessDeniedException if write access is denied
   */
  public void requireWrite(Long userId, WikiSpace space) {
    if (!canWrite(userId, space)) {
      throw new AccessDeniedException(
          String.format("User %d does not have WRITE access to wiki space %d", userId, space.getId()));
    }
  }

  /**
   * Returns {@code true} if {@code userId} may create a new wiki space.
   *
   * <p>Allowed when the user is ADMIN, or when the user's role has the global
   * {@code WIKI / CREATE} permission via {@link PermissionService}.
   * Fail-closed: returns {@code false} if the user is not found.
   *
   * @param userId the user to check
   * @return true if the user may create a wiki space
   */
  @Transactional(readOnly = true)
  public boolean canCreateSpace(Long userId) {
    Optional<User> userOpt = userRepository.findById(userId);
    if (userOpt.isEmpty()) {
      log.warn("WikiPermissionService: user {} not found — denying createSpace", userId);
      return false;
    }
    User user = userOpt.get();
    if (user.getRole() == UserRole.ADMIN) {
      return true;
    }
    return permissionService.hasPermission(user.getRole(), ResourceType.WIKI, PermissionType.CREATE);
  }

  /**
   * Asserts that {@code userId} may create a new wiki space; throws
   * {@link AccessDeniedException} if denied.
   *
   * @param userId the user to check
   * @throws AccessDeniedException if the user is not permitted to create a wiki space
   */
  public void requireCreateSpace(Long userId) {
    if (!canCreateSpace(userId)) {
      throw new AccessDeniedException(
          String.format("User %d does not have permission to create a wiki space", userId));
    }
  }

  // -----------------------------------------------------------------------
  // Internal resolution logic
  // -----------------------------------------------------------------------

  private record AccessResult(boolean canRead, boolean canWrite) {
    static AccessResult full() {
      return new AccessResult(true, true);
    }

    static AccessResult write() {
      return new AccessResult(true, true); // WRITE implies READ
    }

    static AccessResult readOnly() {
      return new AccessResult(true, false);
    }

    static AccessResult deny() {
      return new AccessResult(false, false);
    }

    static AccessResult fromLevel(WikiPermissionLevel level) {
      return level == WikiPermissionLevel.WRITE ? write() : readOnly();
    }
  }

  private AccessResult resolveAccess(Long userId, WikiSpace space) {
    // Step 1: ADMIN gets unconditional full access
    Optional<User> userOpt = userRepository.findById(userId);
    if (userOpt.isEmpty()) {
      log.warn("WikiPermissionService: user {} not found — denying access to space {}", userId, space.getId());
      return AccessResult.deny();
    }

    User user = userOpt.get();
    if (user.getRole() == UserRole.ADMIN) {
      log.debug("WikiPermissionService: ADMIN user {} — granting full access to space {}", userId, space.getId());
      return AccessResult.full();
    }

    // Step 2: Check explicit WikiSpacePermission rows
    List<WikiSpacePermission> permissions = wikiSpacePermissionRepository.findBySpaceId(space.getId());

    String userIdRef = String.valueOf(userId);
    String roleName = user.getRole().name();

    // Step 2a: USER-level grant takes precedence over ROLE grant
    Optional<WikiSpacePermission> userGrant = permissions.stream()
        .filter(p -> p.getGranteeType() == WikiGranteeType.USER && userIdRef.equals(p.getGranteeRef()))
        .findFirst();

    if (userGrant.isPresent()) {
      log.debug(
          "WikiPermissionService: explicit USER grant ({}) for user {} on space {}",
          userGrant.get().getLevel(), userId, space.getId());
      return AccessResult.fromLevel(userGrant.get().getLevel());
    }

    // Step 2b: ROLE-level grant
    Optional<WikiSpacePermission> roleGrant = permissions.stream()
        .filter(p -> p.getGranteeType() == WikiGranteeType.ROLE && roleName.equals(p.getGranteeRef()))
        .findFirst();

    if (roleGrant.isPresent()) {
      log.debug(
          "WikiPermissionService: explicit ROLE grant ({}) for role {} on space {}",
          roleGrant.get().getLevel(), roleName, space.getId());
      return AccessResult.fromLevel(roleGrant.get().getLevel());
    }

    // Step 3: Fall back to global RBAC via PermissionService(UserRole, ResourceType, PermissionType)
    // future: project-membership-based access
    boolean rbacRead = permissionService.hasPermission(user.getRole(), ResourceType.WIKI, PermissionType.READ);
    boolean rbacWrite = permissionService.hasPermission(user.getRole(), ResourceType.WIKI, PermissionType.UPDATE);

    log.debug(
        "WikiPermissionService: RBAC fallback for user {} (role {}) on space {} — read={}, write={}",
        userId, roleName, space.getId(), rbacRead, rbacWrite);

    return new AccessResult(rbacRead, rbacWrite);
  }
}
