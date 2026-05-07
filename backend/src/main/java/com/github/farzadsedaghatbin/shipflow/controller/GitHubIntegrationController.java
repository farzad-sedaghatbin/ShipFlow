package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.github.*;
import com.github.farzadsedaghatbin.shipflow.service.MessageService;
import com.github.farzadsedaghatbin.shipflow.service.github.GitHubIntegrationService;
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
@RequestMapping("/api/github")
@RequiredArgsConstructor
@Tag(name = "GitHub Integration", description = "APIs for managing GitHub integration with tasks and pitches")
public class GitHubIntegrationController {

  private final GitHubIntegrationService integrationService;
  private final MessageService messageService;

  @PostMapping("/repositories")
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  @Operation(summary = "Register a GitHub repository", description = "Register a new GitHub repository to enable webhook integration")
  public ResponseEntity<String> registerRepository(@Valid @RequestBody CreateGitHubRepositoryRequest request) {
    integrationService.createOrUpdateRepository(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(messageService.getMessage("github.repository.registered"));
  }

  @GetMapping("/repositories")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Get all registered GitHub repositories")
  public ResponseEntity<List<GitHubRepositoryDTO>> getAllRepositories() {
    return ResponseEntity.ok(integrationService.getAllRepositories());
  }

  @GetMapping("/tasks/{taskId}/links")
  @Operation(summary = "Get all GitHub links for a task", description = "Returns all commits, PRs, and branches linked to the specified task")
  public ResponseEntity<List<GitHubLinkDTO>> getTaskGitHubLinks(@PathVariable Long taskId) {
    return ResponseEntity.ok(integrationService.getTaskGitHubLinks(taskId));
  }

  @GetMapping("/pitches/{pitchId}/links")
  @Operation(summary = "Get all GitHub links for a pitch", description = "Returns all commits, PRs, and branches linked to the specified pitch")
  public ResponseEntity<List<GitHubLinkDTO>> getPitchGitHubLinks(@PathVariable Long pitchId) {
    return ResponseEntity.ok(integrationService.getPitchGitHubLinks(pitchId));
  }
}
