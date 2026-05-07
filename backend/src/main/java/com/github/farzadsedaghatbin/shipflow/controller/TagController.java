package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.PitchDTO;
import com.github.farzadsedaghatbin.shipflow.dto.TagDTO;
import com.github.farzadsedaghatbin.shipflow.dto.TagRequest;
import com.github.farzadsedaghatbin.shipflow.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for tag management.
 * Part of v0.5 "Insight, Not Metrics" - enables linking retro items to pitches.
 */
@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
@Tag(name = "Tags", description = "Tag management for cross-entity correlation")
public class TagController {

  private final TagService tagService;

  @GetMapping("/project/{projectId}")
  @PreAuthorize("@permissionService.hasPermission('PROJECT', 'READ')")
  @Operation(summary = "Get all tags for a project")
  public ResponseEntity<List<TagDTO>> getTagsByProject(@PathVariable Long projectId) {
    return ResponseEntity.ok(tagService.getTagsByProject(projectId));
  }

  @GetMapping("/project/{projectId}/search")
  @PreAuthorize("@permissionService.hasPermission('PROJECT', 'READ')")
  @Operation(summary = "Search tags by name")
  public ResponseEntity<List<TagDTO>> searchTags(
      @PathVariable Long projectId,
      @RequestParam String query) {
    return ResponseEntity.ok(tagService.searchTags(projectId, query));
  }

  @PostMapping
  @PreAuthorize("@permissionService.hasPermission('PROJECT', 'UPDATE')")
  @Operation(summary = "Create a new tag")
  public ResponseEntity<TagDTO> createTag(@Valid @RequestBody TagRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(tagService.createTag(request));
  }

  @PutMapping("/{tagId}")
  @PreAuthorize("@permissionService.hasPermission('PROJECT', 'UPDATE')")
  @Operation(summary = "Update a tag")
  public ResponseEntity<TagDTO> updateTag(
      @PathVariable Long tagId,
      @Valid @RequestBody TagRequest request) {
    return ResponseEntity.ok(tagService.updateTag(tagId, request));
  }

  @DeleteMapping("/{tagId}")
  @PreAuthorize("@permissionService.hasPermission('PROJECT', 'UPDATE')")
  @Operation(summary = "Delete a tag")
  public ResponseEntity<Void> deleteTag(@PathVariable Long tagId) {
    tagService.deleteTag(tagId);
    return ResponseEntity.noContent().build();
  }

  // Retro item tag associations
  @PostMapping("/retro-item/{retroItemId}/tag/{tagId}")
  @PreAuthorize("@permissionService.hasPermission('RETROSPECTIVE', 'UPDATE')")
  @Operation(summary = "Add tag to retro item")
  public ResponseEntity<Void> addTagToRetroItem(
      @PathVariable Long retroItemId,
      @PathVariable Long tagId) {
    tagService.addTagToRetroItem(retroItemId, tagId);
    return ResponseEntity.ok().build();
  }

  @DeleteMapping("/retro-item/{retroItemId}/tag/{tagId}")
  @PreAuthorize("@permissionService.hasPermission('RETROSPECTIVE', 'UPDATE')")
  @Operation(summary = "Remove tag from retro item")
  public ResponseEntity<Void> removeTagFromRetroItem(
      @PathVariable Long retroItemId,
      @PathVariable Long tagId) {
    tagService.removeTagFromRetroItem(retroItemId, tagId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/retro-item/{retroItemId}")
  @PreAuthorize("@permissionService.hasPermission('RETROSPECTIVE', 'READ')")
  @Operation(summary = "Get tags for a retro item")
  public ResponseEntity<List<TagDTO>> getTagsForRetroItem(@PathVariable Long retroItemId) {
    return ResponseEntity.ok(tagService.getTagsForRetroItem(retroItemId));
  }

  // Pitch tag associations
  @PostMapping("/pitch/{pitchId}/tag/{tagId}")
  @PreAuthorize("@permissionService.hasPermission('PITCH', 'UPDATE')")
  @Operation(summary = "Add tag to pitch")
  public ResponseEntity<Void> addTagToPitch(
      @PathVariable Long pitchId,
      @PathVariable Long tagId) {
    tagService.addTagToPitch(pitchId, tagId);
    return ResponseEntity.ok().build();
  }

  @DeleteMapping("/pitch/{pitchId}/tag/{tagId}")
  @PreAuthorize("@permissionService.hasPermission('PITCH', 'UPDATE')")
  @Operation(summary = "Remove tag from pitch")
  public ResponseEntity<Void> removeTagFromPitch(
      @PathVariable Long pitchId,
      @PathVariable Long tagId) {
    tagService.removeTagFromPitch(pitchId, tagId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/pitch/{pitchId}")
  @PreAuthorize("@permissionService.hasPermission('PITCH', 'READ')")
  @Operation(summary = "Get tags for a pitch")
  public ResponseEntity<List<TagDTO>> getTagsForPitch(@PathVariable Long pitchId) {
    return ResponseEntity.ok(tagService.getTagsForPitch(pitchId));
  }

  // Related pitches (for linking retro items to future bets)
  @GetMapping("/retro-item/{retroItemId}/related-pitches")
  @PreAuthorize("@permissionService.hasPermission('RETROSPECTIVE', 'READ')")
  @Operation(summary = "Find pitches related to retro item via shared tags")
  public ResponseEntity<List<PitchDTO>> findRelatedPitches(@PathVariable Long retroItemId) {
    var pitches = tagService.findRelatedPitches(retroItemId)
        .stream()
        .map(p -> PitchDTO.builder()
            .id(p.getId())
            .title(p.getTitle())
            .status(p.getStatus())
            .appetiteDays(p.getAppetiteDays())
            .cycleId(p.getCycle().getId())
            .cycleName(p.getCycle().getName())
            .build())
        .collect(Collectors.toList());
    return ResponseEntity.ok(pitches);
  }
}
