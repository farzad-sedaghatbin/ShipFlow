package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.customfield.*;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CustomFieldEntityType;
import com.github.farzadsedaghatbin.shipflow.service.CustomFieldService;
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
@RequestMapping("/api/custom-fields")
@RequiredArgsConstructor
@Tag(name = "Custom Fields", description = "Manage custom field definitions and values on Tasks, Pitches, and Bug Reports")
public class CustomFieldController {

  private final CustomFieldService customFieldService;

  // ── Definitions ─────────────────────────────────────────────────────────────

  @GetMapping("/definitions")
  @PreAuthorize("@permissionService.hasPermission('CUSTOM_FIELD', 'READ')")
  @Operation(summary = "List custom field definitions for an entity type")
  public ResponseEntity<List<CustomFieldDefinitionDTO>> getDefinitions(
      @RequestParam CustomFieldEntityType entityType,
      @RequestParam(required = false) Long projectId) {
    return ResponseEntity.ok(customFieldService.getDefinitionsForEntity(entityType, projectId));
  }

  @PostMapping("/definitions")
  @PreAuthorize("@permissionService.hasPermission('CUSTOM_FIELD', 'CREATE')")
  @Operation(summary = "Create a custom field definition")
  public ResponseEntity<CustomFieldDefinitionDTO> createDefinition(
      @Valid @RequestBody CreateCustomFieldDefinitionRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(customFieldService.createDefinition(request));
  }

  @PutMapping("/definitions/{id}")
  @PreAuthorize("@permissionService.hasPermission('CUSTOM_FIELD', 'UPDATE')")
  @Operation(summary = "Update a custom field definition (fieldType and entityType are immutable)")
  public ResponseEntity<CustomFieldDefinitionDTO> updateDefinition(
      @PathVariable Long id,
      @Valid @RequestBody UpdateCustomFieldDefinitionRequest request) {
    return ResponseEntity.ok(customFieldService.updateDefinition(id, request));
  }

  @DeleteMapping("/definitions/{id}")
  @PreAuthorize("@permissionService.hasPermission('CUSTOM_FIELD', 'DELETE')")
  @Operation(summary = "Soft-delete a custom field definition (cascades hard-delete of its values)")
  public ResponseEntity<Void> deleteDefinition(@PathVariable Long id) {
    customFieldService.deleteDefinition(id);
    return ResponseEntity.noContent().build();
  }

  // ── Values ───────────────────────────────────────────────────────────────────

  @GetMapping("/values")
  @PreAuthorize("@permissionService.hasPermission('CUSTOM_FIELD', 'READ')")
  @Operation(summary = "Get all custom field values for an entity instance")
  public ResponseEntity<List<CustomFieldValueDTO>> getValues(
      @RequestParam CustomFieldEntityType entityType,
      @RequestParam Long entityId) {
    return ResponseEntity.ok(customFieldService.getValuesForEntity(entityType, entityId));
  }

  @PutMapping("/values")
  @PreAuthorize("@permissionService.hasPermission('CUSTOM_FIELD', 'UPDATE')")
  @Operation(summary = "Create or update a single custom field value")
  public ResponseEntity<CustomFieldValueDTO> upsertValue(
      @Valid @RequestBody UpsertCustomFieldValueRequest request) {
    return ResponseEntity.ok(customFieldService.upsertValue(request));
  }

  @PutMapping("/values/bulk")
  @PreAuthorize("@permissionService.hasPermission('CUSTOM_FIELD', 'UPDATE')")
  @Operation(summary = "Bulk create or update custom field values for an entity")
  public ResponseEntity<List<CustomFieldValueDTO>> bulkUpsertValues(
      @Valid @RequestBody BulkUpsertCustomFieldValuesRequest request) {
    return ResponseEntity.ok(customFieldService.bulkUpsertValues(request));
  }
}
