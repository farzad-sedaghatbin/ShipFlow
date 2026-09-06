package com.github.farzadsedaghatbin.shipflow.controller.publicapi;

import com.github.farzadsedaghatbin.shipflow.dto.publicapi.PublicProjectDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.repository.ProjectRepository;
import com.github.farzadsedaghatbin.shipflow.security.PublicApiAuthorizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/public/projects")
@RequiredArgsConstructor
@Tag(name = "Public API – Projects", description = "Public API endpoints for project resources")
public class PublicProjectController {

  private final ProjectRepository projectRepository;
  private final PublicApiAuthorizationService publicApiAuthorizationService;

  @GetMapping
  @Operation(summary = "List all active projects")
  public ResponseEntity<List<PublicProjectDTO>> listProjects() {
    Long restrictedProjectId = publicApiAuthorizationService.restrictedProjectIdOrNull();
    List<Project> projects;
    if (restrictedProjectId != null) {
      // A project-restricted key sees only its one allowed project, not the whole org's list.
      projects = projectRepository.findById(restrictedProjectId)
          .filter(Project::getIsActive)
          .map(List::of)
          .orElse(List.of());
    } else {
      projects = projectRepository.findByIsActiveTrue();
    }
    return ResponseEntity.ok(projects.stream().map(this::toDTO).toList());
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get a project by ID")
  public ResponseEntity<PublicProjectDTO> getProject(@PathVariable Long id) {
    // Match listProjects()'s isActive filter — an archived project is excluded from the list
    // but was previously still individually fetchable by ID.
    Project project = projectRepository.findById(id).filter(Project::getIsActive).orElse(null);
    if (project == null) {
      return ResponseEntity.notFound().build();
    }
    publicApiAuthorizationService.requireProjectAccess(
        publicApiAuthorizationService.currentApiKey(), project.getId());
    return ResponseEntity.ok(toDTO(project));
  }

  @GetMapping("/by-key/{projectKey}")
  @Operation(summary = "Get a project by its unique key")
  public ResponseEntity<PublicProjectDTO> getProjectByKey(@PathVariable String projectKey) {
    // Same isActive consistency fix as getProject(id) above — same underlying gap.
    Project project = projectRepository.findByProjectKey(projectKey)
        .filter(Project::getIsActive)
        .orElse(null);
    if (project == null) {
      return ResponseEntity.notFound().build();
    }
    publicApiAuthorizationService.requireProjectAccess(
        publicApiAuthorizationService.currentApiKey(), project.getId());
    return ResponseEntity.ok(toDTO(project));
  }

  private PublicProjectDTO toDTO(Project p) {
    return PublicProjectDTO.builder()
        .id(p.getId())
        .name(p.getName())
        .projectKey(p.getProjectKey())
        .description(p.getDescription())
        .color(p.getColor())
        .projectType(p.getProjectType() != null ? p.getProjectType().name() : null)
        .isActive(p.getIsActive())
        .createdAt(p.getCreatedAt())
        .updatedAt(p.getUpdatedAt())
        .build();
  }
}
