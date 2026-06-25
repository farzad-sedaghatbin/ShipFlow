package com.github.farzadsedaghatbin.shipflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.farzadsedaghatbin.shipflow.dto.knowledge.ChunkPreview;
import com.github.farzadsedaghatbin.shipflow.dto.knowledge.CreateKnowledgeSourceRequest;
import com.github.farzadsedaghatbin.shipflow.dto.knowledge.KnowledgeSourceResponse;
import com.github.farzadsedaghatbin.shipflow.service.UserService;
import com.github.farzadsedaghatbin.shipflow.service.knowledge.source.KnowledgeSourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST endpoints for managing user-curated knowledge sources.
 *
 * <p>Sources feed structured chunks into the AI retrieval layer (Wise Architecture, RAG Q&amp;A,
 * etc.) under three access scopes — organization, team, or project.
 *
 * <p>The JSON variant of {@code POST /api/knowledge/sources} returns {@code 202 Accepted} because
 * ingestion is dispatched asynchronously via {@link
 * com.github.farzadsedaghatbin.shipflow.service.knowledge.source.event.KnowledgeSourceCreatedEvent}
 * to the {@link com.github.farzadsedaghatbin.shipflow.service.knowledge.source.IngestionOrchestrator}.
 * The multipart variant runs ingestion synchronously (so the upload stream is consumed before the
 * request completes) but the response status remains {@code 202} for API consistency.
 */
@Slf4j
@RestController
@RequestMapping("/api/knowledge/sources")
@RequiredArgsConstructor
@Tag(
    name = "Knowledge Center",
    description = "User-curated knowledge sources fed into the AI features")
public class KnowledgeSourceController {

  private final KnowledgeSourceService svc;
  private final UserService userService;
  private final ObjectMapper json;

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Create a knowledge source from a JSON-described provider config")
  public ResponseEntity<KnowledgeSourceResponse> create(
      @Valid @RequestBody CreateKnowledgeSourceRequest req) {
    KnowledgeSourceResponse resp = svc.create(req, currentUserId());
    return ResponseEntity.status(HttpStatus.ACCEPTED).body(resp);
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Create a knowledge source by uploading a file (PDF, Markdown, text…)")
  public ResponseEntity<KnowledgeSourceResponse> createFromFile(
      @RequestPart("file") MultipartFile file,
      @RequestPart("metadata") @Valid CreateKnowledgeSourceRequest meta)
      throws IOException {

    // Stitch the uploaded file's metadata into the provider config so the FileUploadProvider can
    // record filename/contentType on the resulting source entity.
    ObjectNode cfg =
        (meta.getConfig() instanceof ObjectNode on) ? on : json.createObjectNode();
    cfg.put("originalFilename", file.getOriginalFilename());
    cfg.put("contentType", file.getContentType());
    meta.setConfig(cfg);

    KnowledgeSourceResponse resp =
        svc.createWithUpload(
            meta,
            currentUserId(),
            file.getInputStream(),
            file.getOriginalFilename(),
            file.getContentType());
    return ResponseEntity.status(HttpStatus.ACCEPTED).body(resp);
  }

  @GetMapping(params = "scope=org")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "List organization-scoped knowledge sources visible to the caller")
  public List<KnowledgeSourceResponse> listOrg() {
    return svc.listOrg(currentUserId());
  }

  @GetMapping(params = {"scope=team", "teamId"})
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "List team-scoped knowledge sources for a given team")
  public List<KnowledgeSourceResponse> listTeam(@RequestParam Long teamId) {
    return svc.listTeam(teamId, currentUserId());
  }

  @GetMapping(params = {"scope=project", "projectId"})
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "List project-scoped knowledge sources for a given project")
  public List<KnowledgeSourceResponse> listProject(@RequestParam Long projectId) {
    return svc.listProject(projectId, currentUserId());
  }

  @GetMapping("/{id}/chunks")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Preview the first ~10 chunks ingested from a knowledge source")
  public List<ChunkPreview> chunks(@PathVariable Long id) {
    return svc.previewChunks(id, currentUserId());
  }

  @PostMapping("/{id}/refresh")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Re-run ingestion for a knowledge source")
  public ResponseEntity<Void> refresh(@PathVariable Long id) {
    svc.requestRefresh(id, currentUserId());
    return ResponseEntity.accepted().build();
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Soft-delete a knowledge source and its chunks")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    svc.delete(id, currentUserId());
    return ResponseEntity.noContent().build();
  }

  private Long currentUserId() {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    return userService.findByUsername(username).getId();
  }
}
