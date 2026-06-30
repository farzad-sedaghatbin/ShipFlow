package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.GlobalSearchResultDTO;
import com.github.farzadsedaghatbin.shipflow.service.GlobalSearchService;
import com.github.farzadsedaghatbin.shipflow.service.ProjectService;
import com.github.farzadsedaghatbin.shipflow.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for global search across tasks, subtasks, bug reports, pitches, and epics.
 * Results are scoped to a specific project and ranked by relevance.
 */
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Search", description = "Global search across all entity types")
public class GlobalSearchController {

  private final GlobalSearchService globalSearchService;
  private final ProjectService projectService;
  private final UserService userService;

  @GetMapping("/global")
  @Operation(
      summary = "Global search",
      description = "Search across tasks, subtasks, bug reports, pitches, and epics within a project, "
          + "plus org-global wiki spaces and pages. When projectId is omitted, only the org-global "
          + "wiki is searched. Supports fuzzy matching via trigram similarity and exact key matching "
          + "(e.g. BUG-123).")
  public ResponseEntity<List<GlobalSearchResultDTO>> globalSearch(
      @Parameter(description = "Search query (minimum 2 characters)")
      @RequestParam(required = true) String q,
      @Parameter(description = "Project ID to scope project entities; omit for an org-global wiki search")
      @RequestParam(required = false) Long projectId,
      @Parameter(description = "Maximum number of results to return")
      @RequestParam(defaultValue = "10") int limit) {

    if (q == null || q.trim().length() < 2) {
      return ResponseEntity.badRequest().build();
    }

    if (limit < 1 || limit > 50) {
      limit = 10;
    }

    // Project entities require access to that project; the org-global wiki (no projectId) does not.
    if (projectId != null) {
      projectService.requireProjectAccess(projectId);
    }

    log.debug("Global search: q='{}', projectId={}, limit={}", q, projectId, limit);

    List<GlobalSearchResultDTO> results =
        globalSearchService.search(q, projectId, limit, currentUserId());
    return ResponseEntity.ok(results);
  }

  private Long currentUserId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || auth.getName() == null) {
      return null;
    }
    return userService.findByUsername(auth.getName()).getId();
  }
}
