package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.wiki.CreateWikiPageRequest;
import com.github.farzadsedaghatbin.shipflow.dto.wiki.CreateWikiSpaceRequest;
import com.github.farzadsedaghatbin.shipflow.dto.wiki.GrantPermissionRequest;
import com.github.farzadsedaghatbin.shipflow.dto.wiki.MovePageRequest;
import com.github.farzadsedaghatbin.shipflow.dto.wiki.UpdateWikiPageRequest;
import com.github.farzadsedaghatbin.shipflow.dto.wiki.UpdateWikiSpaceRequest;
import com.github.farzadsedaghatbin.shipflow.dto.wiki.WikiAttachmentDTO;
import com.github.farzadsedaghatbin.shipflow.dto.wiki.WikiPageDTO;
import com.github.farzadsedaghatbin.shipflow.dto.wiki.WikiPageSearchDTO;
import com.github.farzadsedaghatbin.shipflow.dto.wiki.WikiRevisionDTO;
import com.github.farzadsedaghatbin.shipflow.dto.wiki.WikiSpaceDTO;
import com.github.farzadsedaghatbin.shipflow.dto.wiki.WikiSpacePermissionDTO;
import com.github.farzadsedaghatbin.shipflow.dto.wiki.WikiTreeNodeDTO;
import com.github.farzadsedaghatbin.shipflow.service.UserService;
import com.github.farzadsedaghatbin.shipflow.service.WikiAttachmentService;
import com.github.farzadsedaghatbin.shipflow.service.WikiAttachmentService.DownloadResult;
import com.github.farzadsedaghatbin.shipflow.service.WikiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/wiki")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
@Tag(name = "Wiki", description = "Wiki spaces, pages, attachments, and permissions")
public class WikiController {

  private final WikiService wikiService;
  private final WikiAttachmentService attachmentService;
  private final UserService userService;

  // ── Spaces ────────────────────────────────────────────────────────────────

  @PostMapping("/spaces")
  @Operation(summary = "Create a wiki space")
  public ResponseEntity<WikiSpaceDTO> createSpace(@RequestBody CreateWikiSpaceRequest req) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(wikiService.createSpace(req, currentUserId()));
  }

  @GetMapping("/spaces")
  @Operation(summary = "List wiki spaces visible to the current user")
  public ResponseEntity<List<WikiSpaceDTO>> listSpaces() {
    return ResponseEntity.ok(wikiService.listSpaces(currentUserId()));
  }

  @GetMapping("/spaces/{id}")
  @Operation(summary = "Get a wiki space by ID")
  public ResponseEntity<WikiSpaceDTO> getSpace(@PathVariable Long id) {
    return ResponseEntity.ok(wikiService.getSpace(id, currentUserId()));
  }

  @PutMapping("/spaces/{id}")
  @Operation(summary = "Update a wiki space")
  public ResponseEntity<WikiSpaceDTO> updateSpace(
      @PathVariable Long id, @RequestBody UpdateWikiSpaceRequest req) {
    return ResponseEntity.ok(wikiService.updateSpace(id, req, currentUserId()));
  }

  @DeleteMapping("/spaces/{id}")
  @Operation(summary = "Delete (soft-delete) a wiki space")
  public ResponseEntity<Void> deleteSpace(@PathVariable Long id) {
    wikiService.deleteSpace(id, currentUserId());
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/spaces/{id}/tree")
  @Operation(summary = "Get the page tree for a space")
  public ResponseEntity<List<WikiTreeNodeDTO>> getTree(@PathVariable Long id) {
    return ResponseEntity.ok(wikiService.getTree(id, currentUserId()));
  }

  // ── Space permissions ─────────────────────────────────────────────────────

  @GetMapping("/spaces/{id}/permissions")
  @Operation(summary = "List permissions for a space")
  public ResponseEntity<List<WikiSpacePermissionDTO>> listPermissions(@PathVariable Long id) {
    return ResponseEntity.ok(wikiService.listPermissions(id, currentUserId()));
  }

  @PostMapping("/spaces/{id}/permissions")
  @Operation(summary = "Grant a permission on a space")
  public ResponseEntity<WikiSpacePermissionDTO> grantPermission(
      @PathVariable Long id, @RequestBody GrantPermissionRequest req) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(wikiService.grantPermission(id, req, currentUserId()));
  }

  @DeleteMapping("/permissions/{permId}")
  @Operation(summary = "Revoke a space permission")
  public ResponseEntity<Void> revokePermission(@PathVariable Long permId) {
    wikiService.revokePermission(permId, currentUserId());
    return ResponseEntity.noContent().build();
  }

  // ── Pages ─────────────────────────────────────────────────────────────────

  @PostMapping("/pages")
  @Operation(summary = "Create a wiki page")
  public ResponseEntity<WikiPageDTO> createPage(@RequestBody CreateWikiPageRequest req) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(wikiService.createPage(req, currentUserId()));
  }

  @GetMapping("/pages/{id}")
  @Operation(summary = "Get a wiki page by ID")
  public ResponseEntity<WikiPageDTO> getPage(@PathVariable Long id) {
    return ResponseEntity.ok(wikiService.getPage(id, currentUserId()));
  }

  @PutMapping("/pages/{id}")
  @Operation(summary = "Update a wiki page")
  public ResponseEntity<WikiPageDTO> updatePage(
      @PathVariable Long id, @RequestBody UpdateWikiPageRequest req) {
    return ResponseEntity.ok(wikiService.updatePage(id, req, currentUserId()));
  }

  @DeleteMapping("/pages/{id}")
  @Operation(summary = "Delete (soft-delete) a wiki page")
  public ResponseEntity<Void> deletePage(@PathVariable Long id) {
    wikiService.deletePage(id, currentUserId());
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/pages/{id}/move")
  @Operation(summary = "Move a wiki page to a new parent/position")
  public ResponseEntity<Void> movePage(
      @PathVariable Long id, @RequestBody MovePageRequest req) {
    wikiService.movePage(id, req, currentUserId());
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/pages/{id}/history")
  @Operation(summary = "Get revision history of a page")
  public ResponseEntity<List<WikiRevisionDTO>> getHistory(@PathVariable Long id) {
    return ResponseEntity.ok(wikiService.getHistory(id, currentUserId()));
  }

  @PostMapping("/pages/{id}/restore/{revision}")
  @Operation(summary = "Restore a page to a specific revision")
  public ResponseEntity<WikiPageDTO> restoreRevision(
      @PathVariable Long id, @PathVariable int revision) {
    return ResponseEntity.ok(wikiService.restoreRevision(id, revision, currentUserId()));
  }

  // ── Attachments ───────────────────────────────────────────────────────────

  @PostMapping(value = "/pages/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(summary = "Upload an attachment to a wiki page")
  public ResponseEntity<WikiAttachmentDTO> uploadAttachment(
      @PathVariable Long id, @RequestParam("file") MultipartFile file) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(attachmentService.upload(id, file, currentUserId()));
  }

  @GetMapping("/pages/{id}/attachments")
  @Operation(summary = "List attachments for a wiki page")
  public ResponseEntity<List<WikiAttachmentDTO>> listAttachments(@PathVariable Long id) {
    return ResponseEntity.ok(attachmentService.list(id, currentUserId()));
  }

  @GetMapping("/attachments/{attId}/download")
  @Operation(summary = "Download a wiki attachment")
  public ResponseEntity<Resource> downloadAttachment(@PathVariable Long attId) {
    DownloadResult result = attachmentService.download(attId, currentUserId());
    ContentDisposition disposition =
        ContentDisposition.attachment().filename(result.originalFileName()).build();
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
        .contentType(MediaType.parseMediaType(result.contentType()))
        .body(result.resource());
  }

  @DeleteMapping("/attachments/{attId}")
  @Operation(summary = "Delete a wiki attachment")
  public ResponseEntity<Void> deleteAttachment(@PathVariable Long attId) {
    attachmentService.delete(attId, currentUserId());
    return ResponseEntity.noContent().build();
  }

  // ── Search ────────────────────────────────────────────────────────────────

  @GetMapping("/pages/search")
  @Operation(summary = "Search wiki pages by title (permission-filtered)")
  public ResponseEntity<List<WikiPageSearchDTO>> searchPages(
      @RequestParam String q, @RequestParam(required = false) Long spaceId) {
    return ResponseEntity.ok(wikiService.searchPages(q, spaceId, currentUserId()));
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  private Long currentUserId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    return userService.findByUsername(auth.getName()).getId();
  }
}
