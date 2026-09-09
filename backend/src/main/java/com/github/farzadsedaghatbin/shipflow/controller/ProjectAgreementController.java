package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.agreement.ProjectAgreementDTO;
import com.github.farzadsedaghatbin.shipflow.dto.agreement.ProjectAgreementRequest;
import com.github.farzadsedaghatbin.shipflow.service.ProjectAgreementService;
import com.github.farzadsedaghatbin.shipflow.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for per-project agreement/contract-term entries — a place for "we agreed X"
 * items that aren't Tasks, optionally linked to the Meeting they originated from.
 */
@RestController
@RequestMapping("/api/projects/{projectId}/agreements")
@RequiredArgsConstructor
@Tag(name = "Project Agreements", description = "Discrete, dated agreement entries logged against a project")
public class ProjectAgreementController {

  private final ProjectAgreementService projectAgreementService;
  private final ProjectService projectService;

  @GetMapping
  @PreAuthorize("@permissionService.hasPermission('PROJECT', 'READ')")
  @Operation(summary = "List agreements for a project")
  public ResponseEntity<List<ProjectAgreementDTO>> listAgreements(@PathVariable Long projectId) {
    projectService.requireProjectAccess(projectId);
    return ResponseEntity.ok(projectAgreementService.listByProject(projectId));
  }

  @PostMapping
  @PreAuthorize("@permissionService.hasPermission('PROJECT', 'UPDATE')")
  @Operation(summary = "Create a new agreement")
  public ResponseEntity<ProjectAgreementDTO> createAgreement(
      @PathVariable Long projectId,
      @Valid @RequestBody ProjectAgreementRequest request) {
    projectService.requireProjectAccess(projectId);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(projectAgreementService.create(projectId, request));
  }

  @PutMapping("/{agreementId}")
  @PreAuthorize("@permissionService.hasPermission('PROJECT', 'UPDATE')")
  @Operation(summary = "Update an agreement")
  public ResponseEntity<ProjectAgreementDTO> updateAgreement(
      @PathVariable Long projectId,
      @PathVariable Long agreementId,
      @Valid @RequestBody ProjectAgreementRequest request) {
    projectService.requireProjectAccess(projectId);
    return ResponseEntity.ok(projectAgreementService.update(projectId, agreementId, request));
  }

  @DeleteMapping("/{agreementId}")
  @PreAuthorize("@permissionService.hasPermission('PROJECT', 'UPDATE')")
  @Operation(summary = "Soft-delete an agreement")
  public ResponseEntity<Void> deleteAgreement(
      @PathVariable Long projectId,
      @PathVariable Long agreementId) {
    projectService.requireProjectAccess(projectId);
    projectAgreementService.delete(projectId, agreementId);
    return ResponseEntity.noContent().build();
  }
}
