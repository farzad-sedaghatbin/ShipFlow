package com.github.farzadsedaghatbin.shipflow.controller.publicapi;

import com.github.farzadsedaghatbin.shipflow.dto.publicapi.PublicProjectDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.repository.ProjectRepository;
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

  @GetMapping
  @Operation(summary = "List all active projects")
  public ResponseEntity<List<PublicProjectDTO>> listProjects() {
    List<PublicProjectDTO> projects = projectRepository.findByIsActiveTrue().stream()
        .map(this::toDTO)
        .toList();
    return ResponseEntity.ok(projects);
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get a project by ID")
  public ResponseEntity<PublicProjectDTO> getProject(@PathVariable Long id) {
    return projectRepository.findById(id)
        .map(this::toDTO)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping("/by-key/{projectKey}")
  @Operation(summary = "Get a project by its unique key")
  public ResponseEntity<PublicProjectDTO> getProjectByKey(@PathVariable String projectKey) {
    return projectRepository.findByProjectKey(projectKey)
        .map(this::toDTO)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
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
