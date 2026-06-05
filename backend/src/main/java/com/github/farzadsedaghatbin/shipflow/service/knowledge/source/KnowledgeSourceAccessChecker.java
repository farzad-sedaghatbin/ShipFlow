package com.github.farzadsedaghatbin.shipflow.service.knowledge.source;

import com.github.farzadsedaghatbin.shipflow.entity.KnowledgeSource;
import com.github.farzadsedaghatbin.shipflow.entity.Person;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.UserRole;
import com.github.farzadsedaghatbin.shipflow.entity.enums.KnowledgeSourceScope;
import com.github.farzadsedaghatbin.shipflow.repository.TeamAssignmentRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserProjectRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Permission gate for {@link KnowledgeSource} operations.
 *
 * <p>Maps the Knowledge Center scope/role contract onto ShipFlow's single-org RBAC. Single-org
 * deployment: no organization FK or orgId parameter anywhere.
 *
 * <p>Role mapping (PERMISSION_MATRIX.md → {@link UserRole}):
 *
 * <ul>
 *   <li>{@code ADMIN} → {@link UserRole#ADMIN}
 *   <li>{@code PROJECT_MANAGER} → {@link UserRole#MANAGER}
 *   <li>{@code DEVELOPER}, {@code PRODUCT} → {@link UserRole#MEMBER}
 *   <li>{@code VIEWER} → {@link UserRole#READONLY}
 * </ul>
 *
 * <p>Membership lookups go through the existing repositories. Team membership is keyed by
 * {@code person_id} (not user_id), so we resolve the user's linked {@link Person} first.
 */
@Component
@RequiredArgsConstructor
public class KnowledgeSourceAccessChecker {

  private final UserRepository users;
  private final TeamAssignmentRepository teamAssignments;
  private final UserProjectRepository userProjects;

  public void assertCanCreate(
      KnowledgeSourceScope scope, Long teamId, Long projectId, Long userId) {
    User u = loadUser(userId);
    UserRole role = u.getRole();
    switch (scope) {
      case ORG ->
          require(role == UserRole.ADMIN, "Only ADMIN can create org-wide sources");
      case TEAM -> {
        require(teamId != null, "teamId required for TEAM scope");
        require(isUserInTeam(u, teamId), "User is not in team");
        require(
            role == UserRole.ADMIN || role == UserRole.MANAGER,
            "MANAGER or ADMIN required for team-scoped sources");
      }
      case PROJECT -> {
        require(projectId != null, "projectId required for PROJECT scope");
        require(isUserInProject(userId, projectId), "User is not on this project");
        require(
            role == UserRole.ADMIN || role == UserRole.MANAGER || role == UserRole.MEMBER,
            "ADMIN, MANAGER, or MEMBER required for project-scoped sources");
      }
    }
  }

  public void assertCanListOrg(Long userId) {
    require(users.existsById(userId), "Unknown user");
  }

  public void assertCanListTeam(Long teamId, Long userId) {
    User u = loadUser(userId);
    require(isUserInTeam(u, teamId), "Not in team");
  }

  public void assertCanListProject(Long projectId, Long userId) {
    require(isUserInProject(userId, projectId), "Not in project");
  }

  public void assertCanModify(KnowledgeSource src, Long userId) {
    User u = loadUser(userId);
    if (u.getRole() == UserRole.ADMIN || userId.equals(src.getCreatedBy())) {
      return;
    }
    assertCanCreate(src.getScope(), src.getTeamId(), src.getProjectId(), userId);
  }

  private User loadUser(Long userId) {
    return users
        .findById(userId)
        .orElseThrow(() -> new AccessDeniedException("Unknown user"));
  }

  private boolean isUserInTeam(User u, Long teamId) {
    Person person = u.getPerson();
    if (person == null || person.getId() == null) {
      return false;
    }
    return teamAssignments.existsActiveAssignment(person.getId(), teamId);
  }

  private boolean isUserInProject(Long userId, Long projectId) {
    return userProjects.existsByUserIdAndProjectId(userId, projectId);
  }

  private static void require(boolean ok, String msg) {
    if (!ok) {
      throw new AccessDeniedException(msg);
    }
  }
}
