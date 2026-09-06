package com.github.farzadsedaghatbin.shipflow.controller.publicapi;

import com.github.farzadsedaghatbin.shipflow.dto.publicapi.PublicEpicDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Epic;
import com.github.farzadsedaghatbin.shipflow.repository.EpicRepository;
import com.github.farzadsedaghatbin.shipflow.security.PublicApiAuthorizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/public/roadmap")
@RequiredArgsConstructor
@Tag(name = "Public API – Roadmap", description = "Public API endpoints for roadmap items (epics)")
public class PublicRoadmapController {

  private final EpicRepository epicRepository;
  private final PublicApiAuthorizationService publicApiAuthorizationService;

  @GetMapping
  @Operation(summary = "List all roadmap items (epics, not deleted)")
  public ResponseEntity<List<PublicEpicDTO>> listEpics(
      @RequestParam(required = false) Long projectId) {
    Long restrictedProjectId = publicApiAuthorizationService.restrictedProjectIdOrNull();
    List<Epic> epics;
    if (restrictedProjectId != null) {
      // A project-restricted key always sees only its own project's epics — any caller-supplied
      // projectId is superseded by the restriction rather than combined with it.
      epics = epicRepository.findByProjectIdNotDeleted(restrictedProjectId);
    } else if (projectId != null) {
      epics = epicRepository.findByProjectIdNotDeleted(projectId);
    } else {
      epics = epicRepository.findAllNotDeleted();
    }
    return ResponseEntity.ok(epics.stream().map(this::toDTO).toList());
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get a roadmap item (epic) by ID")
  public ResponseEntity<PublicEpicDTO> getEpic(@PathVariable Long id) {
    Epic epic = epicRepository.findByIdNotDeleted(id).orElse(null);
    if (epic == null) {
      return ResponseEntity.notFound().build();
    }
    publicApiAuthorizationService.requireProjectAccess(
        publicApiAuthorizationService.currentApiKey(),
        epic.getProject() != null ? epic.getProject().getId() : null);
    return ResponseEntity.ok(toDTO(epic));
  }

  private PublicEpicDTO toDTO(Epic e) {
    return PublicEpicDTO.builder()
        .id(e.getId())
        .name(e.getName())
        .description(e.getDescription())
        .status(e.getStatus() != null ? e.getStatus().name() : null)
        .color(e.getColor())
        .targetStartDate(e.getTargetStartDate())
        .targetEndDate(e.getTargetEndDate())
        .projectId(e.getProject() != null ? e.getProject().getId() : null)
        .initiativeId(e.getInitiative() != null ? e.getInitiative().getId() : null)
        .ownerId(e.getOwner() != null ? e.getOwner().getId() : null)
        .sortOrder(e.getSortOrder())
        .createdAt(e.getCreatedAt())
        .updatedAt(e.getUpdatedAt())
        .build();
  }
}
