package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.UserRole;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ProjectRole;
import com.github.farzadsedaghatbin.shipflow.repository.ProjectRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserProjectRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("projectPermissionService")
@RequiredArgsConstructor
@Slf4j
public class ProjectPermissionService {

  private final UserRepository userRepository;
  private final UserProjectRepository userProjectRepository;
  private final ProjectRepository projectRepository;

  /**
   * Returns true when the calling user has at least one of the supplied project-level roles. Global
   * ADMIN always passes. The project owner is treated as a MANAGER.
   */
  @Transactional(readOnly = true)
  public boolean hasProjectRole(Long projectId, String... roles) {
    User user = getCurrentUser();
    if (user == null) return false;
    if (user.getRole() == UserRole.ADMIN) return true;

    Project project = projectRepository.findById(projectId).orElse(null);
    if (project != null
        && project.getOwner() != null
        && project.getOwner().getId().equals(user.getId())) {
      return Arrays.asList(roles).contains(ProjectRole.MANAGER.name());
    }

    return userProjectRepository
        .findProjectRoleByUserIdAndProjectId(user.getId(), projectId)
        .map(pr -> Arrays.asList(roles).contains(pr.name()))
        .orElse(false);
  }

  /** Returns the calling user's ProjectRole for a given project, or empty if no membership. */
  @Transactional(readOnly = true)
  public Optional<ProjectRole> getMyRole(Long projectId) {
    User user = getCurrentUser();
    if (user == null) return Optional.empty();
    if (user.getRole() == UserRole.ADMIN) return Optional.of(ProjectRole.MANAGER);

    Project project = projectRepository.findById(projectId).orElse(null);
    if (project != null
        && project.getOwner() != null
        && project.getOwner().getId().equals(user.getId())) {
      return Optional.of(ProjectRole.MANAGER);
    }

    return userProjectRepository.findProjectRoleByUserIdAndProjectId(user.getId(), projectId);
  }

  /**
   * Throws AccessDeniedException unless the caller is ADMIN, project owner, or has any project
   * membership.
   */
  public void requireProjectAccess(Long projectId) {
    if (!hasProjectRole(projectId, ProjectRole.VIEWER.name(), ProjectRole.CONTRIBUTOR.name(), ProjectRole.MANAGER.name())) {
      throw new AccessDeniedException(
          "You do not have access to project " + projectId);
    }
  }

  /**
   * Throws AccessDeniedException unless the caller is ADMIN, project owner, or a CONTRIBUTOR /
   * MANAGER on the project.
   */
  public void requireContributor(Long projectId) {
    if (!hasProjectRole(projectId, ProjectRole.CONTRIBUTOR.name(), ProjectRole.MANAGER.name())) {
      throw new AccessDeniedException(
          "You must be a CONTRIBUTOR or MANAGER on project " + projectId);
    }
  }

  /**
   * Throws AccessDeniedException unless the caller is ADMIN, project owner, or a MANAGER on the
   * project.
   */
  public void requireManager(Long projectId) {
    if (!hasProjectRole(projectId, ProjectRole.MANAGER.name())) {
      throw new AccessDeniedException(
          "You must be a MANAGER on project " + projectId);
    }
  }

  private User getCurrentUser() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated()) return null;
    String username = auth.getName();
    return userRepository.findByUsername(username).orElse(null);
  }

  /** Returns all project IDs the calling user can access (for filtering list endpoints). */
  @Transactional(readOnly = true)
  public List<Long> accessibleProjectIds() {
    User user = getCurrentUser();
    if (user == null) return List.of();
    if (user.getRole() == UserRole.ADMIN) {
      return projectRepository.findAll().stream()
          .map(Project::getId)
          .toList();
    }
    return userProjectRepository.findByUserId(user.getId()).stream()
        .map(up -> up.getProject().getId())
        .toList();
  }
}
