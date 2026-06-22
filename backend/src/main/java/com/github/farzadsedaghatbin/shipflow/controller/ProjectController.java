package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.CreateProjectRequest;
import com.github.farzadsedaghatbin.shipflow.dto.ProjectDTO;
import com.github.farzadsedaghatbin.shipflow.entity.UserProject;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ProjectRole;
import com.github.farzadsedaghatbin.shipflow.service.ProjectPermissionService;
import com.github.farzadsedaghatbin.shipflow.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Tag(name = "Projects", description = "Project management endpoints")
public class ProjectController {

  private final ProjectService projectService;
  private final ProjectPermissionService projectPermissionService;

  /**
   * Get all projects (ADMIN only). For regular users, use /my-projects endpoint.
   */
  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Get all projects (Admin only)", description = "Returns all projects. For scoped access use /my-projects")
  public ResponseEntity<List<ProjectDTO>> getAllProjects() {
    return ResponseEntity.ok(projectService.findAll());
  }

  /**
   * Get projects accessible to the current user. This is the primary endpoint for
   * listing projects.
   */
  @GetMapping("/my-projects")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Get user's accessible projects", description = "Returns projects the current user has access to")
  public ResponseEntity<List<ProjectDTO>> getMyProjects() {
    return ResponseEntity.ok(projectService.findAccessibleProjects());
  }

  /** Get active projects accessible to the current user. */
  @GetMapping("/active")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Get user's active projects", description = "Returns active projects the user has access to")
  public ResponseEntity<List<ProjectDTO>> getActiveProjects() {
    return ResponseEntity.ok(projectService.findAccessibleActiveProjects());
  }

  @GetMapping("/{id}")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Get project by ID", description = "Returns a single project by its ID (if user has access)")
  public ResponseEntity<ProjectDTO> getProjectById(@PathVariable Long id) {
    return ResponseEntity.ok(projectService.findById(id));
  }

  @GetMapping("/key/{key}")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Get project by key", description = "Returns a single project by its unique key (if user has access)")
  public ResponseEntity<ProjectDTO> getProjectByKey(@PathVariable String key) {
    return ResponseEntity.ok(projectService.findByKey(key));
  }

  /** Check if current user has access to a project */
  @GetMapping("/{id}/has-access")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Check project access", description = "Returns true if user has access to the project")
  public ResponseEntity<Boolean> hasProjectAccess(@PathVariable Long id) {
    return ResponseEntity.ok(projectService.hasProjectAccess(id));
  }

  /** Get users who have access to a project */
  @GetMapping("/{id}/members")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Get project members", description = "Returns users with direct access to the project")
  public ResponseEntity<List<ProjectMemberDTO>> getProjectMembers(@PathVariable Long id) {
    List<UserProject> members = projectService.getProjectMembers(id);
    List<ProjectMemberDTO> dtos = members.stream().map(this::toMemberDTO).collect(Collectors.toList());
    return ResponseEntity.ok(dtos);
  }

  /** Grant project access to a user — requires MANAGER role on the project */
  @PostMapping("/{projectId}/members/{userId}")
  @PreAuthorize("@projectPermissionService.hasProjectRole(#projectId, 'MANAGER') or hasRole('ADMIN')")
  @Operation(summary = "Grant project access", description = "Grant access to a user (Project Manager or Admin)")
  public ResponseEntity<Void> grantAccess(@PathVariable Long projectId, @PathVariable Long userId,
      @RequestParam(defaultValue = "VIEWER") ProjectRole role) {
    projectService.grantProjectAccess(projectId, userId, role);
    return ResponseEntity.ok().build();
  }

  /** Revoke project access from a user — requires MANAGER role on the project */
  @DeleteMapping("/{projectId}/members/{userId}")
  @PreAuthorize("@projectPermissionService.hasProjectRole(#projectId, 'MANAGER') or hasRole('ADMIN')")
  @Operation(summary = "Revoke project access", description = "Revoke access from a user (Project Manager or Admin)")
  public ResponseEntity<Void> revokeAccess(@PathVariable Long projectId, @PathVariable Long userId) {
    projectService.revokeProjectAccess(projectId, userId);
    return ResponseEntity.noContent().build();
  }

  /** Returns the current user's project-level role */
  @GetMapping("/{projectId}/my-role")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Get my project role", description = "Returns the calling user's ProjectRole for the given project")
  public ResponseEntity<ProjectRole> getMyRole(@PathVariable Long projectId) {
    return ResponseEntity.ok(projectPermissionService.getMyRole(projectId).orElse(null));
  }

  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  @Operation(summary = "Create a new project", description = "Creates a new project (Admin or Manager only)")
  public ResponseEntity<ProjectDTO> createProject(@Valid @RequestBody CreateProjectRequest request) {
    ProjectDTO created = projectService.create(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  @Operation(summary = "Update a project", description = "Updates an existing project (Admin or Manager only)")
  public ResponseEntity<ProjectDTO> updateProject(@PathVariable Long id,
      @Valid @RequestBody CreateProjectRequest request) {
    return ResponseEntity.ok(projectService.update(id, request));
  }

  @PostMapping("/{id}/deactivate")
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  @Operation(summary = "Deactivate a project", description = "Deactivates a project (Admin or Manager only)")
  public ResponseEntity<ProjectDTO> deactivateProject(@PathVariable Long id) {
    return ResponseEntity.ok(projectService.deactivate(id));
  }

  @PostMapping("/{id}/activate")
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  @Operation(summary = "Activate a project", description = "Reactivates a previously deactivated project (Admin or Manager only)")
  public ResponseEntity<ProjectDTO> activateProject(@PathVariable Long id) {
    return ResponseEntity.ok(projectService.activate(id));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Delete a project", description = "Permanently deletes a project (Admin only). Project must have no cycles.")
  public ResponseEntity<Void> deleteProject(@PathVariable Long id) {
    projectService.delete(id);
    return ResponseEntity.noContent().build();
  }

  private ProjectMemberDTO toMemberDTO(UserProject up) {
    return ProjectMemberDTO.builder().userId(up.getUser().getId()).username(up.getUser().getUsername())
        .email(up.getUser().getEmail()).projectRole(up.getProjectRole()).grantedAt(up.getCreatedAt())
        .personId(up.getUser().getPerson() != null ? up.getUser().getPerson().getId() : null)
        .grantedByUsername(up.getGrantedBy() != null ? up.getGrantedBy().getUsername() : null).build();
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ProjectMemberDTO {
    private Long userId;
    private Long personId;
    private String username;
    private String email;
    private ProjectRole projectRole;
    private LocalDateTime grantedAt;
    private String grantedByUsername;
  }
}
