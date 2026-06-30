package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.CreateProjectRequest;
import com.github.farzadsedaghatbin.shipflow.dto.ProjectDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.UserProject;
import com.github.farzadsedaghatbin.shipflow.entity.UserRole;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CyclePhase;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ProjectRole;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ProjectType;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.repository.ProjectRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserProjectRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectService {

  private final ProjectRepository projectRepository;
  private final UserRepository userRepository;
  private final UserProjectRepository userProjectRepository;
  private final CycleRepository cycleRepository;

  /**
   * Self-reference injected lazily so that calls to cached internal methods
   * go through the Spring AOP proxy (required for @Cacheable to work when
   * called from within the same bean).
   */
  @Lazy
  @Autowired
  private ProjectService self;
  private final LocalizationService localizationService;

  /** Find all projects - ADMIN only, returns all projects */
  @Cacheable(value = "projects", key = "'all'")
  @Transactional(readOnly = true)
  public List<ProjectDTO> findAll() {
    return projectRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
  }

  /** Find all active projects - ADMIN only, returns all active projects */
  @Cacheable(value = "projects", key = "'allActive'")
  @Transactional(readOnly = true)
  public List<ProjectDTO> findAllActive() {
    return projectRepository.findAllActiveOrderByName().stream().map(this::toDTO).collect(Collectors.toList());
  }

  /**
   * Find projects accessible to the current user. ADMINs see all projects, others
   * see only projects they have access to.
   */
  @Transactional(readOnly = true)
  public List<ProjectDTO> findAccessibleProjects() {
    User currentUser = getCurrentUser();

    // ADMINs see all projects
    if (currentUser.getRole() == UserRole.ADMIN) {
      log.debug("ADMIN user {} - returning all projects", currentUser.getUsername());
      return findAll();
    }

    // Others see only accessible projects
    return userProjectRepository.findAccessibleProjectsByUserId(currentUser.getId()).stream().map(this::toDTO)
        .collect(Collectors.toList());
  }

  /**
   * Find active projects accessible to the current user. ADMINs see all active
   * projects, others see only active projects they have access to.
   */
  @Transactional(readOnly = true)
  public List<ProjectDTO> findAccessibleActiveProjects() {
    User currentUser = getCurrentUser();

    // ADMINs see all active projects
    if (currentUser.getRole() == UserRole.ADMIN) {
      log.debug("ADMIN user {} - returning all active projects", currentUser.getUsername());
      return findAllActive();
    }

    // Others see only accessible active projects
    return userProjectRepository.findActiveAccessibleProjectsByUserId(currentUser.getId()).stream().map(this::toDTO)
        .collect(Collectors.toList());
  }

  /** Check if current user has access to a specific project */
  @Transactional(readOnly = true)
  public boolean hasProjectAccess(Long projectId) {
    User currentUser = getCurrentUser();

    // ADMINs have access to all projects
    if (currentUser.getRole() == UserRole.ADMIN) {
      return true;
    }

    return userProjectRepository.hasProjectAccess(currentUser.getId(), projectId);
  }

  /** Require project access or throw AccessDeniedException */
  @Transactional(readOnly = true)
  public void requireProjectAccess(Long projectId) {
    if (!hasProjectAccess(projectId)) {
      throw new AccessDeniedException(localizationService.getMessage("project.access.denied", projectId));
    }
  }

  /** Get the user's effective role in a project */
  @Transactional(readOnly = true)
  public Optional<ProjectRole> getUserProjectRole(Long projectId) {
    User currentUser = getCurrentUser();

    // Check if user is project owner
    Project project = projectRepository.findById(projectId).orElse(null);
    if (project != null && project.getOwner() != null && project.getOwner().getId().equals(currentUser.getId())) {
      return Optional.of(ProjectRole.MANAGER);
    }

    // Check direct assignment
    return userProjectRepository.findProjectRoleByUserIdAndProjectId(currentUser.getId(), projectId);
  }

  /**
   * Public entry point: enforces authorization on every call (cache hits included),
   * then delegates to {@link #findByIdCached(Long)} which is wrapped by @Cacheable
   * via the Spring AOP proxy (through {@code self}).
   */
  @Transactional(readOnly = true)
  public ProjectDTO findById(Long id) {
    requireProjectAccess(id);   // always runs — never bypassed by the cache
    return self.findByIdCached(id);
  }

  /**
   * Internal cacheable method. Must only be invoked AFTER access has been verified
   * by {@link #findById(Long)}. Package-private to discourage direct calls.
   */
  @Cacheable(value = "projects", key = "'detail:' + #id")
  @Transactional(readOnly = true)
  public ProjectDTO findByIdCached(Long id) {
    Project project = projectRepository.findByIdWithOwner(id)
        .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));
    return toDTO(project);
  }

  @Transactional(readOnly = true)
  public ProjectDTO findByKey(String projectKey) {
    Project project = projectRepository.findByProjectKey(projectKey.toUpperCase())
        .orElseThrow(() -> new ResourceNotFoundException("Project not found with key: " + projectKey));

    // Check access
    requireProjectAccess(project.getId());

    return toDTO(project);
  }

  @CacheEvict(value = "projects", allEntries = true)
  @Transactional
  public ProjectDTO create(CreateProjectRequest request) {
    if (projectRepository.existsByProjectKey(request.getProjectKey().toUpperCase())) {
      throw new IllegalArgumentException(
          localizationService.getMessage("project.key.exists", request.getProjectKey()));
    }

    Project project = Project.builder().name(request.getName()).projectKey(request.getProjectKey().toUpperCase())
        .description(request.getDescription()).color(request.getColor()).logoUrl(request.getLogoUrl())
        .projectType(request.getProjectType() != null ? request.getProjectType() : ProjectType.SHAPE_UP)
        .isActive(true).build();

    if (request.getOwnerId() != null) {
      User owner = userRepository.findById(request.getOwnerId()).orElseThrow(
          () -> new ResourceNotFoundException("User not found with id: " + request.getOwnerId()));
      project.setOwner(owner);
    }

    project = projectRepository.save(project);

    // For Kanban projects, create a default "Continuous Flow" cycle
    if (project.getProjectType() == ProjectType.KANBAN) {
      createDefaultKanbanCycle(project);
    }

    return toDTO(project);
  }

  /**
   * Creates a default continuous cycle for Kanban projects. This cycle has no end
   * date concept (set to far future) and is always active.
   */
  private void createDefaultKanbanCycle(Project project) {
    Cycle defaultCycle = Cycle.builder().name("Continuous Flow").project(project).startDate(LocalDate.now())
        .endDate(LocalDate.of(2099, 12, 31)) // Far future end date
        .phase(CyclePhase.SHAPING_BUILDING).isActive(true).build();
    cycleRepository.save(defaultCycle);
  }

  @CacheEvict(value = "projects", allEntries = true)
  @Transactional
  public ProjectDTO update(Long id, CreateProjectRequest request) {
    Project project = projectRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));

    // Check if key is being changed and if new key exists
    if (!project.getProjectKey().equals(request.getProjectKey().toUpperCase())) {
      if (projectRepository.existsByProjectKey(request.getProjectKey().toUpperCase())) {
        throw new IllegalArgumentException(
            localizationService.getMessage("project.key.exists", request.getProjectKey()));
      }
      project.setProjectKey(request.getProjectKey().toUpperCase());
    }

    project.setName(request.getName());
    project.setDescription(request.getDescription());
    project.setColor(request.getColor());
    project.setLogoUrl(request.getLogoUrl());

    // Check if project type is being changed to Kanban
    ProjectType oldProjectType = project.getProjectType();
    boolean isConvertingToKanban = false;

    // Update project type if provided
    if (request.getProjectType() != null) {
      isConvertingToKanban = (oldProjectType != ProjectType.KANBAN
          && request.getProjectType() == ProjectType.KANBAN);
      project.setProjectType(request.getProjectType());
    }

    if (request.getOwnerId() != null) {
      User owner = userRepository.findById(request.getOwnerId()).orElseThrow(
          () -> new ResourceNotFoundException("User not found with id: " + request.getOwnerId()));
      project.setOwner(owner);
    } else {
      project.setOwner(null);
    }

    project = projectRepository.save(project);

    // If project was converted to Kanban and doesn't have any active cycles, create
    // a default one
    if (isConvertingToKanban) {
      long activeCycleCount = cycleRepository.countActiveByProjectId(project.getId());
      if (activeCycleCount == 0) {
        createDefaultKanbanCycle(project);
      }
    }

    return toDTO(project);
  }

  @CacheEvict(value = "projects", allEntries = true)
  @Transactional
  public ProjectDTO deactivate(Long id) {
    Project project = projectRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));
    project.setIsActive(false);
    project = projectRepository.save(project);
    return toDTO(project);
  }

  @CacheEvict(value = "projects", allEntries = true)
  @Transactional
  public ProjectDTO activate(Long id) {
    Project project = projectRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));
    project.setIsActive(true);
    project = projectRepository.save(project);
    return toDTO(project);
  }

  @CacheEvict(value = "projects", allEntries = true)
  @Transactional
  public void delete(Long id) {
    Project project = projectRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));

    // Check if project has cycles
    long cycleCount = cycleRepository.countByProjectId(id);
    if (cycleCount > 0) {
      throw new IllegalArgumentException(localizationService.getMessage("project.cannot.delete.with.cycles"));
    }

    projectRepository.delete(project);
  }

  private ProjectDTO toDTO(Project project) {
    long cycleCount = cycleRepository.countByProjectId(project.getId());
    long activeCycleCount = cycleRepository.countActiveByProjectId(project.getId());

    // Get current user's role in this project if available
    ProjectRole userProjectRole = null;
    try {
      userProjectRole = getUserProjectRole(project.getId()).orElse(null);
    } catch (Exception e) {
      // Ignore - may not have auth context
    }

    return ProjectDTO.builder().id(project.getId()).name(project.getName()).projectKey(project.getProjectKey())
        .description(project.getDescription()).color(project.getColor()).logoUrl(project.getLogoUrl())
        .ownerId(project.getOwner() != null ? project.getOwner().getId() : null)
        .ownerName(project.getOwner() != null ? project.getOwner().getUsername() : null)
        .isActive(project.getIsActive()).projectType(project.getProjectType()).createdAt(project.getCreatedAt())
        .updatedAt(project.getUpdatedAt()).cycleCount((int) cycleCount).activeCycleCount((int) activeCycleCount)
        .userProjectRole(userProjectRole != null ? userProjectRole.name() : null).build();
  }

  /** Get the currently authenticated user */
  private User getCurrentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new AccessDeniedException("User not authenticated");
    }

    String username = authentication.getName();
    return userRepository.findByUsername(username)
        .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
  }

  /**
   * Grant project access to a user. Only project MANAGERs, owners, or ADMINs can
   * grant access.
   */
  @Transactional
  public void grantProjectAccess(Long projectId, Long userId, ProjectRole role) {
    User currentUser = getCurrentUser();

    // Check if current user can grant access
    if (currentUser.getRole() != UserRole.ADMIN) {
      requireProjectAccess(projectId);
      ProjectRole currentRole = getUserProjectRole(projectId).orElse(null);
      if (currentRole != ProjectRole.MANAGER) {
        throw new AccessDeniedException("Only project managers can grant access");
      }
    }

    Project project = projectRepository.findById(projectId)
        .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));
    User targetUser = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

    // Check if assignment already exists
    Optional<UserProject> existing = userProjectRepository.findByUserIdAndProjectId(userId, projectId);
    if (existing.isPresent()) {
      // Update existing role
      UserProject up = existing.get();
      up.setProjectRole(role);
      userProjectRepository.save(up);
      log.info("Updated project access: user={}, project={}, role={}", userId, projectId, role);
    } else {
      // Create new assignment
      UserProject userProject = UserProject.builder().user(targetUser).project(project).projectRole(role)
          .grantedBy(currentUser).build();
      userProjectRepository.save(userProject);
      log.info("Granted project access: user={}, project={}, role={}", userId, projectId, role);
    }
  }

  /**
   * Revoke project access from a user. Only project MANAGERs, owners, or ADMINs
   * can revoke access.
   */
  @Transactional
  public void revokeProjectAccess(Long projectId, Long userId) {
    User currentUser = getCurrentUser();

    // Check if current user can revoke access
    if (currentUser.getRole() != UserRole.ADMIN) {
      requireProjectAccess(projectId);
      ProjectRole currentRole = getUserProjectRole(projectId).orElse(null);
      if (currentRole != ProjectRole.MANAGER) {
        throw new AccessDeniedException("Only project managers can revoke access");
      }
    }

    // Cannot revoke own access
    if (currentUser.getId().equals(userId)) {
      throw new IllegalArgumentException("Cannot revoke your own project access");
    }

    userProjectRepository.deleteByUserIdAndProjectId(userId, projectId);
    log.info("Revoked project access: user={}, project={}", userId, projectId);
  }

  /** Get all users with access to a project */
  @Transactional(readOnly = true)
  public List<UserProject> getProjectMembers(Long projectId) {
    requireProjectAccess(projectId);
    return userProjectRepository.findByProjectIdEager(projectId);
  }
}
