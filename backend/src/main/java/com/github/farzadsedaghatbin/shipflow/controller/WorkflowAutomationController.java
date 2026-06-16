package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.workflow.CreateWorkflowAutomationRequest;
import com.github.farzadsedaghatbin.shipflow.dto.workflow.WorkflowAutomationDto;
import com.github.farzadsedaghatbin.shipflow.dto.workflow.WorkflowAutomationExecutionDto;
import com.github.farzadsedaghatbin.shipflow.dto.workflow.WorkflowAutomationTemplateDto;
import com.github.farzadsedaghatbin.shipflow.service.WorkflowAutomationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/automations")
@RequiredArgsConstructor
@Tag(name = "Workflow Automations", description = "Trigger/action automation engine for projects")
public class WorkflowAutomationController {

  private final WorkflowAutomationService automationService;

  // ── Templates ────────────────────────────────────────────────────────────────

  @GetMapping("/templates")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "List all built-in automation templates")
  public ResponseEntity<List<WorkflowAutomationTemplateDto>> getTemplates() {
    return ResponseEntity.ok(automationService.getTemplates());
  }

  @GetMapping("/templates/category/{category}")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "List templates by category")
  public ResponseEntity<List<WorkflowAutomationTemplateDto>> getTemplatesByCategory(
      @PathVariable String category) {
    return ResponseEntity.ok(automationService.getTemplatesByCategory(category));
  }

  // ── Automation Rules ─────────────────────────────────────────────────────────

  @GetMapping("/project/{projectId}")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "List all automation rules for a project")
  public ResponseEntity<List<WorkflowAutomationDto>> getByProject(@PathVariable Long projectId) {
    return ResponseEntity.ok(automationService.getByProject(projectId));
  }

  @GetMapping("/{id}")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Get an automation rule by ID")
  public ResponseEntity<WorkflowAutomationDto> getById(@PathVariable Long id) {
    return ResponseEntity.ok(automationService.getById(id));
  }

  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  @Operation(summary = "Create a new automation rule")
  public ResponseEntity<WorkflowAutomationDto> create(
      @Valid @RequestBody CreateWorkflowAutomationRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(automationService.create(request));
  }

  @PostMapping("/from-template")
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  @Operation(summary = "Create an automation rule from a template")
  public ResponseEntity<WorkflowAutomationDto> createFromTemplate(
      @RequestParam Long templateId,
      @RequestParam Long projectId,
      @RequestParam(required = false) String name) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(automationService.createFromTemplate(templateId, projectId, name));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  @Operation(summary = "Update an automation rule")
  public ResponseEntity<WorkflowAutomationDto> update(
      @PathVariable Long id,
      @Valid @RequestBody CreateWorkflowAutomationRequest request) {
    return ResponseEntity.ok(automationService.update(id, request));
  }

  @PatchMapping("/{id}/toggle")
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  @Operation(summary = "Toggle an automation rule on/off")
  public ResponseEntity<WorkflowAutomationDto> toggle(@PathVariable Long id) {
    return ResponseEntity.ok(automationService.toggleEnabled(id));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  @Operation(summary = "Delete (soft-delete) an automation rule")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    automationService.delete(id);
    return ResponseEntity.noContent().build();
  }

  // ── Execution Logs ───────────────────────────────────────────────────────────

  @GetMapping("/{id}/executions")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Get recent execution log for an automation rule")
  public ResponseEntity<List<WorkflowAutomationExecutionDto>> getExecutionLog(
      @PathVariable Long id,
      @RequestParam(defaultValue = "50") int limit) {
    return ResponseEntity.ok(automationService.getExecutionLog(id, limit));
  }

  @GetMapping("/project/{projectId}/executions")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Get recent automation execution log for an entire project")
  public ResponseEntity<List<WorkflowAutomationExecutionDto>> getProjectExecutionLog(
      @PathVariable Long projectId,
      @RequestParam(defaultValue = "100") int limit) {
    return ResponseEntity.ok(automationService.getProjectExecutionLog(projectId, limit));
  }
}
