package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.CreateProjectRequest;
import com.github.farzadsedaghatbin.shipflow.dto.ProjectDTO;
import com.github.farzadsedaghatbin.shipflow.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Tag(name = "Projects", description = "Project management endpoints")
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all projects", description = "Returns all projects (active and inactive)")
    public ResponseEntity<List<ProjectDTO>> getAllProjects() {
        return ResponseEntity.ok(projectService.findAll());
    }

    @GetMapping("/active")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get active projects", description = "Returns only active projects")
    public ResponseEntity<List<ProjectDTO>> getActiveProjects() {
        return ResponseEntity.ok(projectService.findAllActive());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get project by ID", description = "Returns a single project by its ID")
    public ResponseEntity<ProjectDTO> getProjectById(@PathVariable Long id) {
        return ResponseEntity.ok(projectService.findById(id));
    }

    @GetMapping("/key/{key}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get project by key", description = "Returns a single project by its unique key")
    public ResponseEntity<ProjectDTO> getProjectByKey(@PathVariable String key) {
        return ResponseEntity.ok(projectService.findByKey(key));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    @Operation(summary = "Create a new project", description = "Creates a new project (Admin or Project Manager only)")
    public ResponseEntity<ProjectDTO> createProject(@Valid @RequestBody CreateProjectRequest request) {
        ProjectDTO created = projectService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    @Operation(summary = "Update a project", description = "Updates an existing project (Admin or Project Manager only)")
    public ResponseEntity<ProjectDTO> updateProject(
            @PathVariable Long id,
            @Valid @RequestBody CreateProjectRequest request) {
        return ResponseEntity.ok(projectService.update(id, request));
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    @Operation(summary = "Deactivate a project", description = "Deactivates a project (Admin or Project Manager only)")
    public ResponseEntity<ProjectDTO> deactivateProject(@PathVariable Long id) {
        return ResponseEntity.ok(projectService.deactivate(id));
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    @Operation(summary = "Activate a project", description = "Reactivates a previously deactivated project (Admin or Project Manager only)")
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
}
