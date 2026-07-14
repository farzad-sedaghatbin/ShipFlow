package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.wikilink.LinkWikiPageRequest;
import com.github.farzadsedaghatbin.shipflow.dto.wikilink.LinkedWikiPageDTO;
import com.github.farzadsedaghatbin.shipflow.service.EntityWikiLinkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Links Pitches and Tasks to WikiPages for reference (research/docs), separate from the file
 * attachment system. Bug reports are explicitly out of scope.
 *
 * <p>Follows the {@code GitHubIntegrationController} nested-resource style ({@code
 * /api/github/{tasks|pitches}/{id}/links}) rather than bolting the endpoints directly onto {@code
 * PitchController}/{@code TaskController}. {@code entityType} is a path variable so the correct
 * RBAC resource (PITCH vs BACKLOG) is enforced per-call via {@code @PreAuthorize}, mirroring how
 * task attachments require BACKLOG READ/UPDATE and matching the role set in {@code
 * PERMISSION_MATRIX.md}.
 */
@RestController
@RequestMapping("/api/wiki-links")
@RequiredArgsConstructor
@Tag(name = "Wiki Links", description = "Link Pitches/Tasks to WikiPages for reference")
public class EntityWikiLinkController {

  private final EntityWikiLinkService entityWikiLinkService;

  @GetMapping("/{entityType}/{entityId}")
  @PreAuthorize("@permissionService.hasPermission(#entityType.toUpperCase() == 'PITCH' ? "
      + "T(com.github.farzadsedaghatbin.shipflow.entity.enums.ResourceType).PITCH : "
      + "T(com.github.farzadsedaghatbin.shipflow.entity.enums.ResourceType).BACKLOG, "
      + "T(com.github.farzadsedaghatbin.shipflow.entity.enums.PermissionType).READ)")
  @Operation(summary = "List linked wiki pages", description = "Returns the wiki pages linked to the given Pitch or Task")
  public ResponseEntity<List<LinkedWikiPageDTO>> listLinkedWikiPages(
      @PathVariable String entityType, @PathVariable Long entityId) {
    return ResponseEntity.ok(entityWikiLinkService.getLinkedWikiPages(entityType, entityId));
  }

  @PostMapping("/{entityType}/{entityId}")
  @PreAuthorize("@permissionService.hasPermission(#entityType.toUpperCase() == 'PITCH' ? "
      + "T(com.github.farzadsedaghatbin.shipflow.entity.enums.ResourceType).PITCH : "
      + "T(com.github.farzadsedaghatbin.shipflow.entity.enums.ResourceType).BACKLOG, "
      + "T(com.github.farzadsedaghatbin.shipflow.entity.enums.PermissionType).UPDATE)")
  @Operation(summary = "Link a wiki page", description = "Links an existing wiki page to the given Pitch or Task")
  public ResponseEntity<LinkedWikiPageDTO> linkWikiPage(
      @PathVariable String entityType,
      @PathVariable Long entityId,
      @Valid @RequestBody LinkWikiPageRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(entityWikiLinkService.linkWikiPage(entityType, entityId, request.wikiPageId()));
  }

  @DeleteMapping("/{entityType}/{entityId}/{wikiPageId}")
  @PreAuthorize("@permissionService.hasPermission(#entityType.toUpperCase() == 'PITCH' ? "
      + "T(com.github.farzadsedaghatbin.shipflow.entity.enums.ResourceType).PITCH : "
      + "T(com.github.farzadsedaghatbin.shipflow.entity.enums.ResourceType).BACKLOG, "
      + "T(com.github.farzadsedaghatbin.shipflow.entity.enums.PermissionType).UPDATE)")
  @Operation(summary = "Unlink a wiki page", description = "Removes the link between a wiki page and the given Pitch or Task")
  public ResponseEntity<Void> unlinkWikiPage(
      @PathVariable String entityType, @PathVariable Long entityId, @PathVariable Long wikiPageId) {
    entityWikiLinkService.unlinkWikiPage(entityType, entityId, wikiPageId);
    return ResponseEntity.noContent().build();
  }
}
