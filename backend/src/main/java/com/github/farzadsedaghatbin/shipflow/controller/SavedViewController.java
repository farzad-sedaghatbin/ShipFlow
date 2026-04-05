package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.savedview.CreateSavedViewRequest;
import com.github.farzadsedaghatbin.shipflow.dto.savedview.SavedViewDTO;
import com.github.farzadsedaghatbin.shipflow.dto.savedview.UpdateSavedViewRequest;
import com.github.farzadsedaghatbin.shipflow.service.SavedViewService;
import com.github.farzadsedaghatbin.shipflow.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing saved filter views (named backlog filter presets).
 * Views are private to the authenticated user and scoped to a specific project.
 */
@RestController
@RequestMapping("/api/projects/{projectId}/saved-views")
@RequiredArgsConstructor
@Tag(name = "Saved Views", description = "Save and load named filter presets for the task backlog")
public class SavedViewController {

  private final SavedViewService savedViewService;
  private final UserService userService;

  @GetMapping
  @PreAuthorize("isAuthenticated()")
  @Operation(
      summary = "List saved views",
      description = "Returns all saved filter views for the current user within the given project")
  public ResponseEntity<List<SavedViewDTO>> getSavedViews(@PathVariable Long projectId) {
    Long userId = getCurrentUserId();
    return ResponseEntity.ok(savedViewService.getSavedViews(userId, projectId));
  }

  @PostMapping
  @PreAuthorize("isAuthenticated()")
  @Operation(
      summary = "Create a saved view",
      description = "Saves a named filter preset for the current user within the given project")
  public ResponseEntity<SavedViewDTO> createSavedView(
      @PathVariable Long projectId, @Valid @RequestBody CreateSavedViewRequest request) {
    Long userId = getCurrentUserId();
    SavedViewDTO created = savedViewService.createSavedView(userId, projectId, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  @PutMapping("/{id}")
  @PreAuthorize("isAuthenticated()")
  @Operation(
      summary = "Update a saved view",
      description = "Updates the name and/or filters of an existing saved view owned by the current user")
  public ResponseEntity<SavedViewDTO> updateSavedView(
      @PathVariable Long projectId,
      @PathVariable Long id,
      @Valid @RequestBody UpdateSavedViewRequest request) {
    Long userId = getCurrentUserId();
    return ResponseEntity.ok(savedViewService.updateSavedView(id, userId, request));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("isAuthenticated()")
  @Operation(
      summary = "Delete a saved view",
      description = "Permanently deletes a saved view owned by the current user")
  public ResponseEntity<Void> deleteSavedView(
      @PathVariable Long projectId, @PathVariable Long id) {
    Long userId = getCurrentUserId();
    savedViewService.deleteSavedView(id, userId);
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/{id}/default")
  @PreAuthorize("isAuthenticated()")
  @Operation(
      summary = "Set default view",
      description =
          "Marks the given saved view as the default for this user+project combination, "
              + "unsetting any previous default")
  public ResponseEntity<SavedViewDTO> setDefault(
      @PathVariable Long projectId, @PathVariable Long id) {
    Long userId = getCurrentUserId();
    return ResponseEntity.ok(savedViewService.setDefault(id, userId, projectId));
  }

  // ── Helpers ─────────────────────────────────────────────────────────────────

  private Long getCurrentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String username = authentication.getName();
    return userService.findByUsername(username).getId();
  }
}
